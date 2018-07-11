package org.scribble.ext.ocaml.codegen;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.ast.DataTypeDecl;
import org.scribble.ast.Module;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.main.Job;
import org.scribble.main.ScribbleException;
import org.scribble.model.MState;
import org.scribble.model.endpoint.EState;
import org.scribble.model.global.SGraph;
import org.scribble.model.global.actions.SAction;
import org.scribble.type.name.GProtocolName;
import org.scribble.type.name.PayloadElemType;
import org.scribble.type.name.Role;

public class OCamlAPIBuilder {	
	public final Job job;
	public final GProtocolName fullname;
	public final GProtocolDecl protocol;
	public final HashMap<Role, EState> inits;
	public final boolean hasConnect;
	
	private final String protname;
	
	private static final String EXT_SOURCE_FUNCTOR = "_functor";
	
	public static final Set<String> builtinTypes = new HashSet<>(Arrays.asList("int", "string", "float", "bool"));
	
	public static final String preamble = 
		  "(* Generated from scribble-ocaml https://github.com/keigoi/scribble-ocaml\n"
		+ " * This code should be compiled with scribble-ocaml-runtime\n"
		+ " * https://github.com/keigoi/scribble-ocaml-runtime *)";
	
	public static final String externalTypesFormat =
			  "module type TYPES = sig\n"
			+ "%EXTERNAL_TYPE_CONTENT"
			+ "end";
	
	public static final String wrappingFunctorFormat =
			  "module Make (Session:Scribble.S.SESSION) %TYPEARG = struct\n"
			+ "%PAYLOAD_TYPE_CONTENT\n"
			+ "open Session\n\n"
			+ "%LOCAL_TYPES\n\n"
			+ "module Shmem = Scribble.Shmem.Make(Session.LinIO.IO)(Session.Mutex)(Session.Condition)(Session.Endpoint)\n\n"
			+ "%SHMEM_GLOBAL_CHANNEL\n\n"
			+ "%SERIALIZERS\n"
			+ "end";
		
	public static final String selfRoleFormat =
			  "module %ROLE = struct\n"
			+ "%INITIATOR\n"
		    + "%MODULE_CONTENT\n"
            + "end";
	
	public static final String shmemGlobalChannelFormat =
			"type %PROTOCOL = ShmemMPSTChanenl__ of Shmem.MPSTChannel.t\n" + 
			"let create_shmem_channel : unit -> %PROTOCOL = fun () ->\n" + 
			"  ShmemMPSTChanenl__(Shmem.MPSTChannel.create ~acceptor_role:\"%ACCEPTOR_ROLE\" ~connector_roles:[%CONNECTOR_ROLES])\n"; 
	
	public static final String explicitConnectionInitiatorFormat =
			  "  let initiate : unit -> 'c. ('c, 'c, %CONN_PARAMS %PROTOCOL_%ROLE sess) monad =\n"
			+ "    fun () -> Internal.__initiate ~myname:\"role_%ROLE\"\n\n";
	
	public static final String implicitConnectionInitiatorFormat =
			"  let initiate_shmem : 'c. %PROTOCOL -> ('c, 'c, %CONN_PARAMS %PROTOCOL_%ROLE sess) monad = fun (ShmemMPSTChanenl__(c)) ->\n" + 
			"    Internal.__start (Shmem.MPSTChannel.%CONNECT_OR_ACCEPT c ~role:\"role_%ROLE\")\n"; 
	
	public static final String writerImplFormat = 
			  "      let write_%FUNNAME (conn:X.conn) : [>`%LABEL of %PAYLOAD * 'p sess] -> unit io = function\n"
			+ "        | `%LABEL(Data payload, _) -> failwith \"not implemented\" (* CHANGE HERE *)\n"
			+ "        | _ -> failwith \"impossible: write_%FUNNAME\"";
	
	public static final String readerImplFormat = 
			  "      let read_%FUNNAME (conn:X.conn) : type %TYVARS. %LABELS io =\n"
			+ "        failwith \"not implemented\" (* CHANGE HERE *)";
	
	public static final String typParamFormat = "  type %OCAMLTYP";
	public static final String typDefFormat   = "  type %OCAMLTYP = %REALTYP";
	
	public OCamlAPIBuilder(Job job, GProtocolName fullname) throws ScribbleException {
		this.job = job;
		this.fullname = fullname;
		this.protocol = (GProtocolDecl)this.job.getContext().getMainModule().getProtocolDecl(fullname.getSimpleName());
		this.inits = new HashMap<>();
		
		this.protname = Util.uncapitalise(fullname.getSimpleName().toString());
		this.hasConnect = this.hasConnect();

		
		for (Role role : this.protocol.header.roledecls.getRoles()) {
			inits.put(role, job.getContext().getEGraph(this.fullname, role).init);
		}
	} 		
	
	/**
	 * @return Generated OCaml program
	 */
	public String generateAPI() throws ScribbleException {
		
		List<DataTypeDecl> decls = dataTypeDecls();
		boolean hasFunctorParam = 
				decls.stream()
				.filter(d -> d.extSource.equals(EXT_SOURCE_FUNCTOR))
				.findAny().isPresent();

		String typeparam = 
				hasFunctorParam 
				? externalTypesFormat.replace("%EXTERNAL_TYPE_CONTENT", generateSigBody()) + "\n" 
				: "";
				
		String body = 
				wrappingFunctorFormat
				.replace("%TYPEARG", 
						hasFunctorParam ? "(Types:TYPES)" : "")
				.replace("%PAYLOAD_TYPE_CONTENT", 
						generateImplDataTypeDefs())
				.replace("%LOCAL_TYPES", generateImplLocalTypes())
				.replace("%SHMEM_GLOBAL_CHANNEL", generateImplShmemGlobalChannels())
				.replace("%SERIALIZERS", generateImplSerializers());
		
		return preamble + "\n\n" + typeparam + "\n" + body;
	}
	
	public boolean hasConnect() throws ScribbleException {
		
		SGraph global = job.getContext().getSGraph(this.fullname);
		Set<SAction> actions = MState.getReachableActions(global.init);
		
		for(SAction action : actions) {
			if(action.isConnect()) return true;
		}
		
		return false;
	}

	protected String generateImplLocalTypes() throws ScribbleException {
		
		StringBuffer buf = new StringBuffer();
		Module module = this.job.getContext().getMainModule();
		List<Role> roles = module.getProtocolDecl(this.fullname.getSimpleName()).getHeader().roledecls.getRoles();
		
		for(Role role : roles) {
			OCamlTypeBuilder apigen = new OCamlTypeBuilder(this.job, module, this.fullname, role);
			buf.append(apigen.build());
		}
		return buf.toString();
	}

	protected String generateImplShmemGlobalChannels() {
		List<Role> roles = this.protocol.header.roledecls.getRoles();
		String acceptorRoleStr = "role_" + roles.get(0);
		
		StringBuilder b = new StringBuilder();
		b.append("\"role_" + roles.get(1) + "\"");
		for(int i = 2; i < roles.size(); i++) {
			b.append("; \"role_" + roles.get(i) + "\"");
		}
		b.append("");
		
		
		return
		 shmemGlobalChannelFormat
		 .replace("%PROTOCOL", this.protname)
		 .replace("%ACCEPTOR_ROLE", acceptorRoleStr)
		 .replace("%CONNECTOR_ROLES", b.toString());
	}
	
	protected String generateImplSerializers() throws ScribbleException {
		List<Role> roles = this.protocol.header.roledecls.getRoles();
		return roles.stream().map(role -> generateImplSerializers(role, roles)).collect(Collectors.joining("\n"));
	}
	
	protected String generateImplSerializers(Role me, List<Role> roles) {
		StringBuffer buf = new StringBuffer();
		
		StringBuffer contentBuf = new StringBuffer();
		for(Role peer : roles) {
			if (peer.equals(me)) continue;
			contentBuf.append(Serializers.generateSerializers(this.inits, me, peer));
			contentBuf.append('\n');
		}
		String code = 
				selfRoleFormat
				.replace("%ROLE", me.toString())
				.replace("%INITIATOR", getInitiator(me, roles))
				.replace("%PROTOCOL", this.protname)
				.replace("%MODULE_CONTENT", contentBuf.toString());
		buf.append(code);
		buf.append('\n');
		
		return buf.toString();
	}
	
	protected String getInitiator(Role me, List<Role> roles) {
		String template = hasConnect ? explicitConnectionInitiatorFormat : implicitConnectionInitiatorFormat;
		String connectOrAccept = me.equals(roles.get(0)) ? "accept" : "connect";
		String initiatorConnectionTypeParams = Util.getRoleConnTypeParams(roles, me, hasConnect ? Optional.empty() : Optional.of("Shmem.Raw.t"));
		return template
				.replace("%CONN_PARAMS", initiatorConnectionTypeParams)
				.replace("%PROTOCOL", this.protname)
				.replace("%ROLE", me.toString())
				.replace("%CONNECT_OR_ACCEPT", connectOrAccept);
	}
	
	protected String generateSigBody() {
		List<DataTypeDecl> decls = dataTypeDecls();
		
		return decls.stream()
				.filter(d -> d.extSource.equals(EXT_SOURCE_FUNCTOR))
				.map(typ -> typParamFormat
						.replace("%OCAMLTYP", OCamlTypeBuilder.payloadTypeToString(typ))
						)
				.collect(Collectors.joining("\n"))+"\n";
	}
	
	protected String generateImplDataTypeDefs() {
		List<DataTypeDecl> decls = 
				dataTypeDecls().stream()
				.filter(typ -> !builtinTypes.contains(OCamlTypeBuilder.payloadTypeToString(typ)))
				.collect(Collectors.toList());
		
		return decls.stream()
				.map(typ -> typDefFormat
						.replace("%OCAMLTYP", OCamlTypeBuilder.payloadTypeToString(typ))
						.replace("%REALTYP",  
								typ.extSource.equals(EXT_SOURCE_FUNCTOR) 
								? "Types." + OCamlTypeBuilder.payloadTypeToString(typ) 
								: typ.extName)
						)
				.collect(Collectors.joining("\n"))+"\n";
	}
	
	protected List<DataTypeDecl> dataTypeDecls() {
		Set<PayloadElemType<?>> payloads = this.protocol.header.roledecls.getRoles().stream()
				.flatMap(r -> DataTypes.payloads(this.inits.get(r)).stream())
				.distinct()
				.collect(Collectors.toSet());
		return DataTypes.dataTypeDecls(this.job, payloads);
	}
}

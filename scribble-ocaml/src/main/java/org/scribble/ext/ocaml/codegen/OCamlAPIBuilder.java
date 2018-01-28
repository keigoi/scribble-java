package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.scribble.ast.DataTypeDecl;
import org.scribble.ast.Module;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.main.Job;
import org.scribble.main.ScribbleException;
import org.scribble.model.endpoint.EState;
import org.scribble.model.global.SState;
import org.scribble.model.global.actions.SAction;
import org.scribble.type.name.GProtocolName;
import org.scribble.type.name.PayloadElemType;
import org.scribble.type.name.Role;

public class OCamlAPIBuilder {	
	public final Job job;
	public final GProtocolName fullname;
	public final GProtocolDecl protocol;
	public final List<Role> roles;
	public final HashMap<Role, EState> inits;

	public OCamlAPIBuilder(Job job, GProtocolName fullname) throws ScribbleException {
		this.job = job;
		this.fullname = fullname;
		this.protocol = (GProtocolDecl)this.job.getContext().getMainModule().getProtocolDecl(fullname.getSimpleName());
		this.inits = new HashMap<>();
		this.roles = this.protocol.header.roledecls.getRoles();
		
		for (Role role : roles) {
			inits.put(role, job.getContext().getEGraph(this.fullname, role).init);
		}
	} 
	
	public static final String preamble = 
		  "(* Generated from scribble-ocaml https://github.com/keigoi/scribble-ocaml\n"
		+ " * This code should be compiled with scribble-ocaml-runtime\n"
		+ " * https://github.com/keigoi/scribble-ocaml-runtime *)";
		
	
	/**
	 * @return Generated OCaml program
	 */
	public String generateAPI() throws ScribbleException {
		Module main = this.job.getContext().getMainModule();
		
		boolean hasFunctorParam = !Util.functorParamDataTypeDecls(main).isEmpty();

		String typeparam = 
				hasFunctorParam 
				? externalTypesFormat.replace("%EXTERNAL_TYPE_CONTENT", generateFunctorSigBody(main)) + "\n" 
				: "";
				
		String body = 
				wrappingFunctorFormat
				.replace("%TYPEARG", 
						hasFunctorParam ? "(Types:TYPES)" : "")
				.replace("%PAYLOAD_TYPE_CONTENT", 
						generateExternalTypeDefs(main))
				.replace("%MODULE_CONTENT", 
						generateTypes() + "\n" + generateRoles());
		
		return preamble + "\n\n" + typeparam + "\n" + body;
	}

	public String generateTypes() throws ScribbleException {
		
		StringBuffer buf = new StringBuffer();
		Module module = this.job.getContext().getMainModule();
		
		for(Role role : this.roles) {
			OCamlTypeBuilder apigen = new OCamlTypeBuilder(this.job, module, this.fullname, role);
			buf.append(apigen.build());
		}
		return buf.toString();
	}
	
	public static final String externalTypesFormat =
			  "module type TYPES = sig\n"
			+ "%EXTERNAL_TYPE_CONTENT"
			+ "end";
	
	public static final String wrappingFunctorFormat =
			  "module Make (Session:Scribble.Base.SESSION) %TYPEARG = struct\n"
			+ "%PAYLOAD_TYPE_CONTENT\n"
			+ "open Session\n\n"
			+ "%MODULE_CONTENT\n"
			+ "end";
	
	public static final String selfRoleFormat =
			  "module %ROLE = struct\n"
			+ "  let initiate_%ROLE : unit -> ('c, 'c, %CONN_PARAMS %PROTOCOL_%ROLE sess) monad =\n"
		    + "    fun () -> Internal.__initiate ~myname:\"role_%ROLE\"\n\n"
		    + "%MODULE_CONTENT\n"
            + "end";
	
	public static final String peerRoleFormat = 
			  "  module %ROLE = struct\n"
			+ "    module Make(X:sig\n"
			+ "        type conn\n"
			+ "        val conn : conn Endpoint.conn_kind\n"
			+ "%LABEL_MODULE_ARGS"
			+ "      end) = struct\n"
			+ "      let role : ([>`%ROLE of X.conn * 'lab], X.conn, 'lab) role =\n"
			+ "        {_pack_role=(fun labels -> `%ROLE(labels)) ; _repr=\"role_%ROLE\"; _kind=X.conn}\n\n"
			+ "%LABEL_MODULE_CONTENT"
			+ "    end\n\n"
		    + "    module Shmem = struct\n"
			+ "      include Make(struct\n"
			+ "          type conn = Raw.t\n"
			+ "          let conn = Shmem\n"
			+ "%SHMEM_LABEL_MODULE_CONTENT"
			+ "        end)\n"
			+ "        let connector, acceptor = shmem ()\n"
			+ "    end\n"
			+ "  end";
	
	public static final String writerSigFormat = 
			"        val write_%FUNNAME : conn -> [>`%LABEL of %PAYLOAD * 'p sess] -> unit io";

	public static final String readerSigFormat = 
			  "        val read_%FUNNAME : conn -> %LABELS io";

	public static final String sendLabelFormat =
			    "      let %LABEL : 'p. ([>`%LABEL of %PAYLOAD * 'p sess], X.conn, %PAYLOAD * 'p sess) label =\n"
			  + "        {_pack_label=(fun payload -> `%LABEL(payload)); _send=X.write_%FUNNAME}";
	
	public static final String recvLabelFormat =
			    "      let receive_%FUNNAME  : type %TYVARS. (%LABELS, X.conn) labels =\n"
			  + "        {_receive=X.read_%FUNNAME}";

	public static final String shmemWriterImplFormat = 
			  "          let write_%FUNNAME = Raw.send";
	
	public static final String shmemReaderImplFormat = 
			  "          let read_%FUNNAME = Raw.receive";
	
	public static final String typParamFormat = "  type %OCAMLTYP";
	public static final String typDefFormat   = "  type %OCAMLTYP = %REALTYP";
	
	protected String generateRoles() throws ScribbleException {
		List<Role> roles = this.protocol.header.roledecls.getRoles();
		return roles.stream().map(role -> generateRoles(role, roles)).collect(Collectors.joining("\n"));
	}
	
	protected String generateRoles(Role me, List<Role> roles) {
		StringBuffer buf = new StringBuffer();
		
		StringBuffer contentBuf = new StringBuffer();
		for(Role peer : roles) {
			if (peer.equals(me)) continue;
			contentBuf.append(generatePeerRole(me, peer));
			contentBuf.append('\n');
		}
		String protname = Util.uncapitalise(fullname.getSimpleName().toString());
		String code = 
				selfRoleFormat
				.replace("%ROLE", me.toString())
				.replace("%PROTOCOL", protname)
				.replace("%CONN_PARAMS", Util.getRoleConnTypeParams(roles, me))
				.replace("%MODULE_CONTENT", contentBuf.toString());
		buf.append(code);
		buf.append('\n');
		
		return buf.toString();
	}
	
	protected String generatePeerRole(Role me, Role peer) {
		List<LabelAndPayload> sendLabels = new ArrayList<>(); 
		List<List<LabelAndPayload>> recvLabels = new ArrayList<>(); 
		EState state = inits.get(me);
		sendLabels.addAll(sendLabels(peer, state));
		recvLabels.addAll(recvLabels(peer, state));
		
		StringBuffer moduleArgBuffer = new StringBuffer(), 
				moduleContentBuffer = new StringBuffer(),
				shmemModuleContentBuffer = new StringBuffer();
		
		HashMap<String, Integer> labelDupCheck = new HashMap<>();	
		for(LabelAndPayload label: sendLabels) {
			int count = labelDupCheck.getOrDefault(label.label, 0);
			labelDupCheck.put(label.label, count+1);
			String funname = label.label + (count == 0 ? "" : "_" + count); 
			moduleArgBuffer.append(writerSigFormat.replace("%FUNNAME", funname).replace("%LABEL", Util.label(label.label)).replace("%PAYLOAD", label.getPayloadTypeRepr()));
			moduleArgBuffer.append('\n');
			moduleContentBuffer.append(sendLabelFormat.replace("%FUNNAME", funname).replace("%LABEL", Util.label(label.label)).replace("%PAYLOAD", label.getPayloadTypeRepr()));
			moduleContentBuffer.append('\n');
			shmemModuleContentBuffer.append(shmemWriterImplFormat.replace("%FUNNAME", funname));
			shmemModuleContentBuffer.append('\n');
		}
		
		HashMap<List<String>, Integer> branchDupCheck = new HashMap<>();	
		for(List<LabelAndPayload> branch: recvLabels) {
			
			List<String> labels = branch.stream().map(b -> b.label).collect(Collectors.toList());
			
			int count = branchDupCheck.getOrDefault(labels, 0);
			branchDupCheck.put(labels, count+1);
			
			String funname = labels.stream().map(Util::uncapitalise).collect(Collectors.joining("_or_")) 
					+ (count==0 ? "" : "_" + count);
			String tyvars = IntStream.range(0, branch.size()).mapToObj(i -> "p" + i).collect(Collectors.joining(" "));
			
			moduleArgBuffer.append(
					readerSigFormat
					.replace("%FUNNAME", funname)
					.replace("%LABELS", generatePolyvarBranch(branch, false)));
			moduleArgBuffer.append('\n');
			
			moduleContentBuffer.append(
					recvLabelFormat
					.replace("%FUNNAME", funname)
					.replace("%LABELS", generatePolyvarBranch(branch, true))
					.replace("%TYVARS", tyvars));
			moduleContentBuffer.append('\n');
			shmemModuleContentBuffer.append(shmemReaderImplFormat.replace("%FUNNAME", funname));
			shmemModuleContentBuffer.append('\n');
		}
		
		return peerRoleFormat
				.replace("%ROLE", peer.toString())
				.replace("%LABEL_MODULE_ARGS", moduleArgBuffer.toString())
				.replace("%LABEL_MODULE_CONTENT", moduleContentBuffer.toString())
				.replace("%SHMEM_LABEL_MODULE_CONTENT", shmemModuleContentBuffer.toString());
	}
	
	protected String generatePolyvarBranch(List<LabelAndPayload> branch, boolean abstract_tyvar) {
		int count = 0;
		StringBuffer buf = new StringBuffer();
		for(LabelAndPayload label : branch) {
			buf.append("`" + Util.label(label.label) + " of " + label.getPayloadTypeRepr() + " * " + (abstract_tyvar ? "" : "'") + "p" + count);
			if (count<branch.size()-1) {
				buf.append("|");
			}
			count++;
		}
		return "[" + buf.toString() + "]";
	}
	
	protected String generateLabelsWithFormat(String format, Set<String> labels) {
		StringBuffer buf = new StringBuffer();
		
		for(String label : labels) {
			String code;
			if("".equals(label)) {
				code = format.replace("%LABEL", "msg")
						.replaceAll("%FUNNAME", "none");
			} else {
				code = format.replace("%LABEL", Util.label(label))
						.replaceAll("%FUNNAME", Util.uncapitalise(label));
			}
			buf.append(code);
			buf.append("\n");
		}
		
		return buf.toString();
	}
	
	public Role getInitiator() throws ScribbleException {
		Role initiator = null;
		SState init = job.getContext().getSGraph(this.fullname).init;
		for(SAction action : init.getActions()) {
			if(initiator != null && !initiator.equals(action.subj)) {
				throw new RuntimeException("[OCaml] ??? multiple initiators ??? 1:"+initiator + " 2:" + action.subj);
			}
			initiator = action.subj;
		}
		return initiator;
	}
	
	protected String generateFunctorSigBody(Module main) {
		List<DataTypeDecl> decls = Util.functorParamDataTypeDecls(main);
		
		return decls.stream()
				.map(typ -> typParamFormat
						.replace("%OCAMLTYP", OCamlTypeBuilder.payloadTypeToString(typ))
						)
				.collect(Collectors.joining("\n"))+"\n";
	}
	
	protected String generateExternalTypeDefs(Module main) {
		List<DataTypeDecl> decls = Util.dataTypeDecls(main);
		
		return decls.stream()
				.map(typ -> typDefFormat
						.replace("%OCAMLTYP", OCamlTypeBuilder.payloadTypeToString(typ))
						.replace("%REALTYP",  
								typ.extSource.equals(Util.EXT_SOURCE_FUNCTOR) 
								? "Types." + OCamlTypeBuilder.payloadTypeToString(typ) 
								: typ.extName)
						)
				.collect(Collectors.joining("\n"))+"\n";
	}
	
	protected Set<PayloadElemType<?>> payloads(EState state) {
		return Util.labels(state, a -> true)
				.stream()
				.flatMap(l -> l.payloads.stream())
				.collect(Collectors.toSet());
	}
	
	protected List<LabelAndPayload> sendLabels(Role peer, EState state) {
		return Util.labels(state, a -> a.peer.equals(peer) && (a.isSend() || a.isRequest()));
	}
	
	
	protected List<List<LabelAndPayload>> recvLabels(Role peer, EState state) {
		return recvLabels(peer, state, new ArrayList<>());
	}

	protected List<List<LabelAndPayload>> recvLabels(Role peer, EState state, List<EState> visited) {
		if(visited.contains(state)) {
			return new ArrayList<>();
		}
		
		visited.add(state);
		
		List<LabelAndPayload> branchLabels = 
				state.getActions().stream()
				.filter(a -> a.peer.equals(peer) && (a.isReceive() || a.isAccept()))
				.map(a -> new LabelAndPayload(a.mid.toString(), a.payload.elems))
				.collect(Collectors.toList());
		
		HashSet<List<LabelAndPayload>> dupcheck = new HashSet<>();
		ArrayList<List<LabelAndPayload>> ret = new ArrayList<>();
		
		if (!branchLabels.isEmpty()) {
			ret.add(branchLabels);
			dupcheck.add(branchLabels);
		}
		
		for(EState succ : state.getSuccessors()) {
			for(List<LabelAndPayload> labels : recvLabels(peer, succ, visited)) {
				if (dupcheck.contains(labels)) continue;
				ret.add(labels);
				dupcheck.add(labels);
			}
		}
		return ret;
	}
}

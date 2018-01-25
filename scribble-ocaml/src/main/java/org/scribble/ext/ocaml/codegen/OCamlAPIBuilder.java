package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.scribble.ast.Module;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.main.Job;
import org.scribble.main.ScribbleException;
import org.scribble.model.MState;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.model.global.SGraph;
import org.scribble.model.global.SState;
import org.scribble.model.global.actions.SAction;
import org.scribble.type.name.GProtocolName;
import org.scribble.type.name.PayloadElemType;
import org.scribble.type.name.Role;

public class OCamlAPIBuilder {	
	public final Job job;
	public final GProtocolName fullname;
	public final GProtocolDecl protocol;
	public final HashMap<Role, EState> inits;

	public OCamlAPIBuilder(Job job, GProtocolName fullname) throws ScribbleException {
		this.job = job;
		this.fullname = fullname;
		this.protocol = (GProtocolDecl)this.job.getContext().getMainModule().getProtocolDecl(fullname.getSimpleName());
		this.inits = new HashMap<>();
		
		for (Role role : this.protocol.header.roledecls.getRoles()) {
			inits.put(role, job.getContext().getEGraph(this.fullname, role).init);
		}
	} 
	
	public static final String preamble = 
		  "(* Generated from scribble-ocaml https://github.com/keigoi/scribble-ocaml\n"
		+ " * This code should be compiled with scribble-ocaml-runtime\n"
		+ " * https://github.com/keigoi/scribble-ocaml-runtime *)\n"
		+ "open Scribble.Direct (* or: open Scribble_lwt *)";
		
	
	/**
	 * @return Generated OCaml program
	 */
	public String generateAPI() throws ScribbleException {
		// abstract local type
		String globaltype = "type " + Util.uncapitalise(fullname.getSimpleName().toString()) + "\n";
		
		String localtypes = generateTypes();
		
		String roles = generateRoles();
		
		return preamble + "\n" + globaltype + "\n" + localtypes + "\n" + roles;
	}

	public String generateTypes() throws ScribbleException {
		
		StringBuffer buf = new StringBuffer();
		Module module = this.job.getContext().getMainModule();
		List<Role> roles = module.getProtocolDecl(this.fullname.getSimpleName()).getHeader().roledecls.getRoles();
		
		for(Role role : roles) {
			OCamlTypeBuilder apigen = new OCamlTypeBuilder(this.job, module, this.fullname, role);
			buf.append(apigen.build());
		}
		return buf.toString();
	}
	
	public static final String selfRoleFormat =
			  "module %ROLE = struct\n"
			+ "  let initiate_%ROLE : unit -> ('c, 'c, %CONN_PARAMS %PROTOCOL_%ROLE sess) monad =\n"
		    + "    fun () -> Internal.__initiate ~myname:\"role_%ROLE\"\n\n"
		    + "%MODULE_CONTENT"
            + "end";
	
	public static final String peerRoleFormat = 
			  "  module %ROLE = struct\n"
			+ "    module Make(P:sig\n"
			+ "        type conn\n"
			+ "        val conn : conn Endpoint.conn_kind\n"
			+ "%LABEL_MODULE_ARGS"
			+ "      end) = struct\n"
			+ "      module P = P\n"
			+ "      let role : ([>`%ROLE of P.conn * 'lab], P.conn, 'lab) role = {_pack_role=(fun labels -> `%ROLE(labels)) ; _repr=\"role_%ROLE\"; _kind=P.conn}\n\n"
			+ "%LABEL_MODULE_CONTENT"
			+ "    end\n\n"
		    + "    module Shmem = struct\n"
			+ "      include Make(struct\n"
			+ "          type conn = Raw.t\n"
			+ "          let conn = Shmem\n"
			+ "%SHMEM_LABEL_MODULE_CONTENT"
			+ "        end)\n"
			+ "    end\n"
			+ "  end";
	
	public static final String writerSigFormat = 
			"        val write_%FUNNAME : conn -> [>`%LABEL of %PAYLOAD * 'p sess] -> unit io";

	public static final String readerSigFormat = 
			  "        val read_%FUNNAME : conn -> %LABELS io";

	public static final String sendLabelFormat =
			  "      let %LABEL : 'p. ([>`%LABEL of %PAYLOAD * 'p sess], P.conn, %PAYLOAD * 'p sess) label = {_pack_label=(fun payload -> `%LABEL(payload)); _send=P.write_%FUNNAME}";
	
	public static final String recvLabelFormat =
			  "      let receive_%FUNNAME  : type %TYVARS. (%LABELS, P.conn) labels = {_receive=P.read_%FUNNAME}";
			
	public static final String writerImplFormat = 
			  "      let write_%FUNNAME (conn:P.conn) : [>`%LABEL of %PAYLOAD * 'p sess] -> unit io = function\n"
			+ "        | `%LABEL(Data payload, _) -> failwith \"not implemented\" (* CHANGE HERE *)\n"
			+ "        | _ -> failwith \"impossible: write_%FUNNAME\"";
	
	public static final String readerImplFormat = 
			  "      let read_%FUNNAME (conn:P.conn) : type %TYVARS. %LABELS io =\n"
			+ "        failwith \"not implemented\" (* CHANGE HERE *)";
	
	public static final String shmemWriterImplFormat = 
			  "          let write_%FUNNAME = Raw.send";
	
	public static final String shmemReaderImplFormat = 
			  "          let read_%FUNNAME = Raw.receive";
	
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
			moduleArgBuffer.append(writerSigFormat.replace("%FUNNAME", funname).replace("%LABEL", Util.label(label.label)).replace("%PAYLOAD", label.payloadTypeRepr));
			moduleArgBuffer.append('\n');
			moduleContentBuffer.append(sendLabelFormat.replace("%FUNNAME", funname).replace("%LABEL", Util.label(label.label)).replace("%PAYLOAD", label.payloadTypeRepr));
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
			buf.append("`" + Util.label(label.label) + " of " + label.payloadTypeRepr + " * " + (abstract_tyvar ? "" : "'") + "p" + count);
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
	
	public boolean hasConnect() throws ScribbleException {
		
		SGraph global = job.getContext().getSGraph(this.fullname);
		Set<SAction> actions = MState.getReachableActions(global.init);
		
		for(SAction action : actions) {
			if(action.isConnect()) return true;
		}
		
		return false;
	}
	
	protected static final class LabelAndPayload {
		public final String label;
		public final String payloadTypeRepr;
		public LabelAndPayload(String label, List<PayloadElemType<?>> payloads) {
			this.label = label;
			this.payloadTypeRepr = OCamlTypeBuilder.payloadTypesToString(payloads); 
		}
		@Override
		public int hashCode() {
			return Objects.hash(label, payloadTypeRepr);
		}
		@Override
		public boolean equals(Object obj) {
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			LabelAndPayload other = (LabelAndPayload) obj;
			return Objects.equals(this.label, other.label) && Objects.equals(this.payloadTypeRepr, other.payloadTypeRepr);
		}
		@Override
		public String toString() {
			return label+"<" + payloadTypeRepr + ">";
		}
	}
	
	
	protected List<LabelAndPayload> sendLabels(Role peer, EState state) {
		return labels(state, a -> a.peer.equals(peer) && (a.isSend() || a.isRequest()), new ArrayList<>());
	}
	
	protected List<LabelAndPayload> labels(EState state, Predicate<EAction> predicate, List<EState> visited) {
		if(visited.contains(state)) {
			return new ArrayList<>();
		}
		
		visited.add(state);
		
		List<LabelAndPayload> labelsWithDup = 
				state.getActions().stream()
				.filter(predicate)
				.map(a -> new LabelAndPayload(a.mid.toString(), a.payload.elems))
				.collect(Collectors.toList());
		
		HashSet<LabelAndPayload> dupcheck = new HashSet<>();
		
		ArrayList<LabelAndPayload> ret = new ArrayList<>();
		for(LabelAndPayload label : labelsWithDup) {
			if (dupcheck.contains(label)) continue;
			ret.add(label);
			dupcheck.add(label);
		}
		
		for(EState succ : state.getSuccessors()) {
			for(LabelAndPayload label : labels(succ, predicate, visited)) {
				if (dupcheck.contains(label)) continue;
				ret.add(label);
				dupcheck.add(label);
			}
		}
		return ret;
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

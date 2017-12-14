package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

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
import org.scribble.type.name.Role;

public class OCamlAPIBuilder {	
	public final Job job;
	public final GProtocolName fullname;
	public final GProtocolDecl protocol;

	public OCamlAPIBuilder(Job job, GProtocolName fullname) {
		this.job = job;
		this.fullname = fullname;
		this.protocol = (GProtocolDecl)this.job.getContext().getMainModule().getProtocolDecl(fullname.getSimpleName());
	} 
	
	public static final String preamble = 
		  "(* Generated from scribble-ocaml https://github.com/keigoi/scribble-ocaml\n"
		+ " * This code should be compiled with scribble-ocaml-runtime\n"
		+ " * https://github.com/keigoi/scribble-ocaml-runtime *)\n"
		+ "open Scribble.Direct (* or: open Scribble_lwt *)";
	
	
	public static final String roleDeclFormat = 
			"let mk_role_%ROLE c : ([`%ROLE], _) role = Internal.__mkrole c \"role_%ROLE\"";
	
	public static final String connectorFormat = 
			"let connect_%ROLE : (%PROTOCOL,[`Implicit]) channel -> ('c, 'c, %CONN_PARAMS %PROTOCOL_%ROLE sess) monad =\n"
		  + "  fun ch ->\n"
          + "  Internal.__connect ~myname:\"role_%ROLE\" ch";
	
	public static final String acceptorFormat = 
			"let accept_%ROLE : (%PROTOCOL,[`Implicit]) channel -> ('c, 'c, %CONN_PARAMS %PROTOCOL_%ROLE sess) monad =\n"
		  + "  fun ch ->\n"
          + "  Internal.__accept ~myname:\"role_%ROLE\" ~cli_count:%CONNECTCOUNT ch";
	
	public static final String initiatorFormat = 
			"let initiate_%ROLE : unit -> ('c, 'c, %CONN_PARAMS %PROTOCOL_%ROLE sess) monad =\n"
		  + "  fun () ->\n"
          + "  Internal.__initiate ~myname:\"role_%ROLE\"";
	
	public static final String newChannelStandardFormat = 
			"let new_channel_%PROTOCOL : unit -> (%PROTOCOL,[`Implicit]) channel = new_channel";
	
	public static final String labelFormat =
			"let msg_%FUNNAME = {_pack=(fun a -> `%LABEL(a))}";
	
	/**
	 * @return Generated OCaml program
	 */
	public String generateAPI() throws ScribbleException {
		// abstract local type
		String globaltype = "type " + Util.uncapitalise(fullname.getSimpleName().toString()) + "\n";
		
		String localtypes = generateTypes();
		String roles = generateRoles();
		String connectors = 
				hasConnect() ? generateInitiators() 
						: generateStandardAcceptorAndConnectors();
		String labels = generateLabels();
		
		return preamble + "\n" + globaltype + "\n" + localtypes + "\n" + roles + "\n" + connectors + "\n" + labels;
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
	
	
	public String generateRoles() throws ScribbleException {
		return generateRolesWithFormat(roleDeclFormat, this.protocol.header.roledecls.getRoles());
	}
	
	public String generateStandardAcceptorAndConnectors() throws ScribbleException {
		Role initiator = getInitiator();
		
		List<Role> otherRoles = 
				this.protocol.header.roledecls.getRoles().stream()
				.filter((Role r) -> !r.equals(initiator))
				.collect(Collectors.toList());
		
		String acceptor = 
				generateRolesWithFormat(acceptorFormat, Arrays.asList(initiator))
				.replace("%CONNECTCOUNT", String.valueOf(otherRoles.size()));
		
		String connectors = generateRolesWithFormat(connectorFormat, otherRoles);
		
		String protname = Util.uncapitalise(fullname.getSimpleName().toString());
		String newchannel =
				newChannelStandardFormat.replace("%PROTOCOL", protname);
		
		return acceptor + "\n" + connectors + "\n" + newchannel;
	}
	
	public String generateInitiators() throws ScribbleException {
		List<Role> roles = this.protocol.header.roledecls.getRoles();
		
		return generateRolesWithFormat(initiatorFormat, roles);
	}
	
	public String generateLabels() throws ScribbleException {
		TreeSet<String> labels = new TreeSet<>(); 
		for(Role role : this.protocol.header.roledecls.getRoles()) {
			EState state = job.getContext().getEGraph(this.fullname, role).init;
			labels.addAll(labels(state));
		}
		return generateLabelsWithFormat(labelFormat, labels);
	}
	
	protected String generateRolesWithFormat(String format, List<Role> roles) throws ScribbleException {
		StringBuffer buf = new StringBuffer();
		
		for(Role role : roles) {
			String rolename = role.toString();
			String protname = Util.uncapitalise(fullname.getSimpleName().toString());
			String code = 
					format
					.replace("%ROLE", rolename)
					.replace("%PROTOCOL", protname)
					.replace("%CONN_PARAMS", Util.getRoleConnTypeParams(roles, role));
			buf.append(code);
			buf.append('\n');
		}
		
		return buf.toString();
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
						.replaceAll("%FUNNAME", label);
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
	
	
	protected Set<String> labels(EState state) {
		return labels(state, new ArrayList<>());
	}
	
	protected Set<String> labels(EState state, List<EState> visited) {
		
		if(visited.contains(state)) {
			return new HashSet<>();
		}
		
		visited.add(state);
		
		Set<String> labels = 
				state.getActions().stream()
				.map((EAction a) -> a.mid.toString())
				.collect(Collectors.toSet());
		
		for(EState succ : state.getSuccessors()) {
			labels.addAll(labels(succ, visited));
		}
		return labels;
	}
}

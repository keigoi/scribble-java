package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.ast.ProtocolDecl;
import org.scribble.main.Job;
import org.scribble.main.ScribbleException;
import org.scribble.model.MState;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.model.global.SGraph;
import org.scribble.model.global.SState;
import org.scribble.model.global.actions.SAction;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.sesstype.name.Role;

public class OCamlAPIBuilder {
	public final Job job;
	public final GProtocolName fullname;
	public final ProtocolDecl<?> protocol;

	public OCamlAPIBuilder(Job job, GProtocolName fullname, Role mainrole) {
		this.job = job;
		this.fullname = fullname;
		this.protocol = this.job.getContext().getMainModule().getProtocolDecl(fullname.getSimpleName());
	} 
	
	public static final String preamble = 
		  "(* Generated from scribble-ocaml https://github.com/keigoi/scribble-ocaml\n"
		+ " * This code should be compiled with session-ocaml (multiparty)\n"
		+ " * https://github.com/keigoi/session-ocaml/tree/multiparty *)\n"
		+ "open Multiparty";
	
	
	public static final String roleDeclFormat = 
			"let role_%ROLE : [`%ROLE] role = Internal.__mkrole \"%PROTOCOL_%ROLE\"";
	
	public static final String connectorFormat = 
			"let connect_%ROLE : 'pre 'post. (%PROTOCOL,[`ConnectFirst]) channel -> bindto:(empty, %PROTOCOL_%ROLE sess, 'pre, 'post) slot -> ('pre,'post,unit) monad =\n"
		  + "  fun ch ->\n"
          + "  Internal.__connect ~myname:\"%PROTOCOL_%ROLE\" ch";
	
	public static final String acceptorFormat = 
			"let accept_%ROLE : 'pre 'post. (%PROTOCOL,[`ConnectFirst]) channel -> bindto:(empty, %PROTOCOL_%ROLE sess, 'pre, 'post) slot -> ('pre,'post,unit) monad =\n"
		  + "  fun ch ->\n"
          + "  Internal.__accept ~myname:\"%PROTOCOL_%ROLE\" ~cli_count:%CONNECTCOUNT ch";
	
	public static final String initiatorFormat = 
			"let initiate_%ROLE : 'pre 'post. (%PROTOCOL,[`ConnectLater]) channel -> bindto:(empty, %PROTOCOL_%ROLE sess, 'pre, 'post) slot -> ('pre,'post,unit) monad =\n"
		  + "  fun ch ->\n"
          + "  Internal.__initiate ~myname:\"%PROTOCOL_%ROLE\" ch";
	
	public static final String newChannelStandardFormat = 
			"let new_channel_%PROTOCOL : unit -> (%PROTOCOL,[`ConnectLater]) channel = new_channel";
	
	public static final String newChannelExplicitConnectionFormat = 
			"let new_channel_%PROTOCOL () : (%PROTOCOL,[`ConnectLater]) channel = Internal.__new_connect_later_channel [%ROLES]";
	
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
				
		for(Role role : this.protocol.header.roledecls.getRoles()) {
			OCamlTypeBuilder apigen = new OCamlTypeBuilder(job, fullname, role, job.getContext().getEGraph(fullname, role));
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
		
		String initiators = generateRolesWithFormat(initiatorFormat, roles);
		
		String protname = Util.uncapitalise(fullname.getSimpleName().toString());
		String rolesstr = 
				roles.stream()
				.map((Role r) -> '"' + protname + "_" + r.toString() + '"')
				.collect(Collectors.joining(";"));
		String newchannel =
				newChannelExplicitConnectionFormat
				.replace("%PROTOCOL", protname)
				.replace("%ROLES",  rolesstr);;
				
		return initiators + "\n" + newchannel;
	}
	
	public String generateLabels() throws ScribbleException {
		HashSet<String> labels = new HashSet<>(); 
		for(Role role : this.protocol.header.roledecls.getRoles()) {
			EState state = job.getContext().getEGraph(fullname, role).init;
			labels.addAll(labels(state));
		}
		return generateLabelsWithFormat(labelFormat, labels);
	}
	
	protected String generateRolesWithFormat(String format, List<Role> roles) throws ScribbleException {
		StringBuffer buf = new StringBuffer();
		
		for(Role role : roles) {
			String rolename = role.toString();
			String protname = Util.uncapitalise(fullname.getSimpleName().toString());
			String code = format.replace("%ROLE", rolename).replace("%PROTOCOL", protname);
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

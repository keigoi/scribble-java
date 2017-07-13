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
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.sesstype.name.Role;

public class OCamlAPIBuilder {
	public final Job job;
	public final GProtocolName fullname;
	public final Role mainrole;
	public final ProtocolDecl<?> protocol;

	public OCamlAPIBuilder(Job job, GProtocolName fullname, Role mainrole) {
		this.job = job;
		this.fullname = fullname;
		this.mainrole = mainrole;
		this.protocol = this.job.getContext().getMainModule().getProtocolDecl(fullname.getSimpleName());
	} 
	
	public static final String preamble = 
		  "(* Generated from scribble-ocaml https://github.com/keigoi/scribble-ocaml\n"
		+ " * This code should be compiled with session-ocaml (multiparty)\n"
		+ " * https://github.com/keigoi/session-ocaml/tree/multiparty *)\n"
		+ "open Multiparty";
	
	
	public static final String roleDeclFormat = 
			"let role_%ROLE : [`%ROLE] role = __mkrole \"%PROTOCOL_%ROLE\"";
	
	public static final String connectorFormat = 
			"let connect_%ROLE : 'pre 'post. %PROTOCOL channel -> bindto:(empty, %PROTOCOL_%ROLE sess, 'pre, 'post) slot -> ('pre,'post,unit) monad =\n"
		  + "  fun ch ->\n"
          + "  __connect ~myname:\"%PROTOCOL_%ROLE\" ch";
	
	public static final String acceptorFormat = 
			"let accept_%ROLE : 'pre 'post. %PROTOCOL channel -> bindto:(empty, %PROTOCOL_%ROLE sess, 'pre, 'post) slot -> ('pre,'post,unit) monad =\n"
		  + "  fun ch ->\n"
          + "  __accept ~myname:\"%PROTOCOL_%ROLE\" ~cli_count:%CONNECTCOUNT ch";
	
	public static final String labelFormat =
			"let msg_%LABEL = {_pack=(fun a -> `%LABEL(a))}";
	
	/**
	 * @return Generated OCaml program
	 */
	public String generateAPI() throws ScribbleException {
		// abstract local type
		String globaltype = "type " + Util.uncapitalise(fullname.getSimpleName().toString()) + "\n";
		
		String localtypes = generateTypes();
		String roles = generateRoles();
		String connectors = generateAcceptorAndConnectors();
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
	
	public String generateAcceptorAndConnectors() throws ScribbleException {
		
		List<Role> otherRoles = 
				this.protocol.header.roledecls.getRoles().stream()
				.filter((Role r) -> !r.equals(this.mainrole))
				.collect(Collectors.toList());
		
		String acceptor = 
				generateRolesWithFormat(acceptorFormat, Arrays.asList(this.mainrole))
				.replace("%CONNECTCOUNT", String.valueOf(otherRoles.size()));
		
		String connectors = generateRolesWithFormat(connectorFormat, otherRoles);
		
		return acceptor + "\n" + connectors;
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
			String code = format.replace("%LABEL", label);
			buf.append(code);
			buf.append("\n");
		}
		
		return buf.toString();
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
				.filter((String s) -> !"".equals(s))
				.collect(Collectors.toSet());
		
		for(EState succ : state.getSuccessors()) {
			labels.addAll(labels(succ, visited));
		}
		return labels;
	}
	
}

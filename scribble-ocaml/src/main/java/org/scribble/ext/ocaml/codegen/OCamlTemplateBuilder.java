package org.scribble.ext.ocaml.codegen;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.ast.DataTypeDecl;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.main.Job;
import org.scribble.main.ScribbleException;
import org.scribble.model.MState;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.type.name.GProtocolName;
import org.scribble.type.name.Role;

public class OCamlTemplateBuilder {
	public final Job job;
	public final GProtocolName fullname;
	public final GProtocolDecl protocol;
	
	protected Indent indent = new Indent();
	
	public static final String templateFormat = 
			  "open Linocaml.Direct\n"
			+ "open Scribble.Direct\n\n"
			+ "\n"
			+ "let __dummy__ = failwith \"Stub: Please remove __dummy__ in .ml file.\"\n"
			+ "\n"
			+ "module %MODNAME_ =\n"
			+ "  %MODNAME.Make\n"
			+ "    (Scribble.Direct) (* or: Scribble_lwt *)\n"
			+ "%FUNCTORTYPEARG\n\n"
			+ "%MODULE_ASSIGNMENT\n\n"
			+ "%SHMEMCONNECTORS\n\n"
			+ "let main () =\n"
			+ "  let%lin #o = %MODNAME_.%ROLE.initiate_%ROLE () in\n"
			+ "%BODY\n"
			+ "\n"
			+ "let () ="
			+ "  run main ()";
	
	public static final String functorTypeArgFormat = 
			  "    (struct\n"
			+ "%TYPES"
			+ "    end)";
	
	public static final String typeArgFormat =
			  "      type %TYPEPARAM = %TYPEARG";
	
	public static final String moduleAssignFormat =
			"module %ROLE_PEER = %MODNAME_.%ROLE_ME.%ROLE_PEER.Shmem";
	
	public static final String shmemConnectorFormat =
			"let __%ROLE_PEER_connector = %MODNAME_.%ROLE_ME.%ROLE_PEER.Shmem.connector";
	
	public static final String shmemAcceptorFormat =
			"let __%ROLE_PEER_acceptor = %MODNAME_.%ROLE_ME.%ROLE_PEER.Shmem.acceptor";
	
	public OCamlTemplateBuilder(Job job, GProtocolName fullname) {
		this.job = job;
		this.fullname = fullname;
		this.protocol = (GProtocolDecl)this.job.getContext().getMainModule().getProtocolDecl(fullname.getSimpleName());
	}
	
	public String generateTemplates(Role role) throws ScribbleException {
		
		String moduleName = Util.capitalise(this.fullname.getLastElement().toString());
		List<Role> roles = this.protocol.getHeader().roledecls.getRoles();
				
		EState start = this.job.getContext().getEGraph(this.fullname, role).init;
		List<EState> toplevel = Util.getRecurringStates(start);
		
		StringBuffer buf = new StringBuffer();
		this.buildOperations(start, buf, toplevel);
		String body = buf.toString();
		
		return templateFormat
				.replace("%MODNAME", moduleName)
				.replace("%FUNCTORTYPEARG", generateTypeArgs())
				.replace("%MODULE_ASSIGNMENT", generateModuleAssignment(moduleName, role, roles))
				.replace("%SHMEMCONNECTORS", generateShmemConnectorsAndAcceptors(moduleName, role))
				.replace("%ROLE", role.toString())
				.replace("%BODY", body);
	}
	
	protected String generateModuleAssignment(String moduleName, Role me, List<Role> roles) {
		return roles.stream()
					.filter(peer -> !me.equals(peer))
					.map(peer -> 
						moduleAssignFormat
						.replace("%ROLE_PEER", peer.toString())
						.replace("%ROLE_ME", me.toString())
						.replace("%MODNAME", moduleName))
					.collect(Collectors.joining("\n"));
	}
	
	protected String generateTypeArgs() {
		List<DataTypeDecl> dtds = Util.functorParamDataTypeDecls(this.job.getContext().getMainModule());
		if (dtds.isEmpty()) {
			return "";
		} else {
			String types = dtds.stream()
					.map(d -> 
						typeArgFormat
						.replace("%TYPEPARAM", Util.uncapitalise(d.name.toString()))
						.replace("%TYPEARG", "unit"))
					.collect(Collectors.joining("\n")) + "\n";
			return functorTypeArgFormat.replace("%TYPES", types);
		}
	}
	
	protected String generateShmemConnectorsAndAcceptors(String moduleName, Role me) throws ScribbleException {
		return 
		connectingPeers(me).stream()
			.map(peer -> 
				shmemConnectorFormat
				.replace("%ROLE_ME", me.toString())
				.replace("%ROLE_PEER", peer.toString())
				.replace("%MODNAME", moduleName)
				)
			.collect(Collectors.joining("\n")) + "\n" +
		acceptingPeers(me).stream()
			.map(peer ->
				shmemAcceptorFormat
				.replace("%ROLE_ME", me.toString())
				.replace("%ROLE_PEER", peer.toString())
				.replace("%MODNAME", moduleName)
				)
			.collect(Collectors.joining("\n"));
		
	}
	
	protected Set<Role> connectingPeers(Role role) throws ScribbleException {
		EState start = this.job.getContext().getEGraph(this.fullname, role).init;
		return MState.getReachableActions(start).stream().filter(a -> a.isRequest()).map(a -> a.peer).collect(Collectors.toSet());
	}
	protected Set<Role> acceptingPeers(Role role) throws ScribbleException {
		EState start = this.job.getContext().getEGraph(this.fullname, role).init;
		return MState.getReachableActions(start).stream().filter(a -> a.isAccept()).map(a -> a.peer).collect(Collectors.toSet());
	}


	protected void buildOperations(EState curr, StringBuffer buf, List<EState> toplevel) {

		// indent_(buf);

		if (this.indent.curr() != 0 && toplevel.contains(curr)) {
			// recursion
			return;
		}

		switch (curr.getStateKind()) {
		case OUTPUT:
			boolean isDisconnect = curr.getActions().stream().anyMatch(a -> a.isDisconnect());
			if (isDisconnect) {
				disconnect(curr, buf, toplevel);
			} else {				
				output(curr, buf, toplevel);
			}
			break;
		case UNARY_INPUT:
			unaryInput(curr, buf, toplevel, false);
			break;
		case POLY_INPUT:
			polyInput(curr, buf, toplevel, false);
			break;
		case TERMINAL:
			this.indent(buf);
			buf.append("close\n");
			break;
		case ACCEPT:
			polyInput(curr, buf, toplevel, true);
			break;
		case WRAP_SERVER:
			throw new RuntimeException("TODO");
		default:
			throw new RuntimeException("Shouldn't get in here: " + curr);
		}
	}
	
	public void disconnect(EState curr, StringBuffer buf, List<EState> toplevel) {
		// labels and paylaods are ignored
		EAction action = Util.getSingleAction(curr);
		this.indent(buf);
		buf.append("let%lin #o = disconnect " + action.peer + ".role in\n");
		EState succ = curr.getSuccessor(action);
		buildOperations(succ, buf, toplevel);
	}
	
	public static void printOutput(StringBuffer buf, String op, EAction action) {
		op = op.replace("%ROLE", action.peer.toString());
		buf.append("let%lin #o = " 
				+ op + " (" 
				+ action.peer + ".role, " 
				+ action.peer + "." + Util.label(action.mid) + ", " 
				+ "__dummy__) in\n");
	}
	
	public static void printUnaryInput(StringBuffer buf, String op, List<EAction> actions) {
		op = op.replace("%ROLE", actions.get(0).peer.toString()); 
		String label = Util.label(actions.get(0).mid);
		buf.append("let%lin `" + label + "(_, #o) = ");
		buf.append(printInputPart(op, actions));
		buf.append(" in\n");
	}
	
	public static String printInputPart(String op, List<EAction> actions) {
		Role peer = actions.get(0).peer;
		op = op.replace("%ROLE", peer.toString());
		
		List<String> labels = actions.stream().map(a -> a.mid.toString()).collect(Collectors.toList());
		String funname = Util.receiveFunname(labels);
		
		return op + " (" 
				+ peer + ".role, " 
				+ peer + ".receive_" + funname + ")";	
	}
	
	public void output(EState curr, StringBuffer buf, List<EState> toplevel) {
		// output can contain both datatype and non-datatype payload

		String op = curr.getActions().get(0).isRequest() 
				? "connect __%ROLE_connector"
				: "send";

		if (curr.getActions().size() == 1) {
			EAction action = curr.getActions().get(0);
			
			this.indent(buf);
			printOutput(buf, op, action);
			
			buildOperations(curr.getSuccessor(action), buf, toplevel);
			
		} else if (curr.getActions().size() == 2) {
			this.indent(buf);
			buf.append("begin if __dummy__ then\n");
			
			EAction action1 = curr.getActions().get(0);
			this.indent.incr();
			this.indent(buf);
			printOutput(buf, op, action1);
			buildOperations(curr.getSuccessor(action1), buf, toplevel);
			this.indent.decr();
			
			this.indent(buf);
			buf.append("else\n");
			
			EAction action2 = curr.getActions().get(1);
			this.indent.incr();
			this.indent(buf);
			printOutput(buf, op, action2);
			buildOperations(curr.getSuccessor(action2), buf, toplevel);
			this.indent.decr();
			
			this.indent(buf);
			buf.append("end\n");
			
		} else {
			this.indent(buf);
			buf.append("begin match __dummy__ with\n");
			
			int[] count = {0};
			this.indent.iterate(buf, curr.getActions(), (EAction action) -> {
				
				buf.append("| " + count[0] + " ->\n");
				count[0]++;
				
				this.indent.incr();
				this.indent(buf);
				printOutput(buf, op, action);
				
				EState succ = curr.getSuccessor(action);
				buildOperations(succ, buf, toplevel);
				
				this.indent.decr();
			});
			this.indent(buf);
			buf.append("end\n");
		}
	}
	
	public void unaryInput(EState curr, StringBuffer buf, List<EState> toplevel, boolean accept) {
		String op = accept ? "accept __%ROLE_acceptor" : "receive";
		
		this.indent(buf);
		printUnaryInput(buf, op, curr.getActions());
		
		EState succ = curr.getSuccessor(curr.getActions().get(0));
		buildOperations(succ, buf, toplevel);
	}
	
	public void polyInput(EState curr, StringBuffer buf, List<EState> toplevel, boolean accept) {
		
		if (curr.getActions().size()==1) {
			unaryInput(curr, buf, toplevel, accept);
			return;
		}
		
		this.indent(buf);
		String op = accept ? "accept __%ROLE_acceptor" : "receive";
		
		List<EAction> actions = curr.getActions();
		buf.append("begin match%lin " + printInputPart(op, actions) + " with");
		
		this.indent.iterate(buf, curr.getActions(), (EAction action) -> {
			buf.append("| `" + Util.label(action.mid) + "(_, #o) ->\n");
			EState succ = curr.getSuccessor(action);
			
			this.indent.incr();
			buildOperations(succ, buf, toplevel);
			this.indent.decr();
		});
		this.indent(buf);
		buf.append("end\n");
	}
	
	protected void indent(StringBuffer buf) {
		this.indent.indent(buf);
	}
}

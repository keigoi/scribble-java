package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.scribble.ast.Module;
import org.scribble.main.Job;
import org.scribble.main.ScribbleException;
import org.scribble.model.endpoint.EGraph;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.type.name.DataType;
import org.scribble.type.name.GDelegationType;
import org.scribble.type.name.GProtocolName;
import org.scribble.type.name.LProtocolName;
import org.scribble.type.name.PayloadElemType;
import org.scribble.type.name.Role;

public class OCamlTypeBuilder {
	public final Job job;
	public final Module module;
	public final GProtocolName gpn;
	public final Role role;
	
	protected Map<Integer, String> names = new HashMap<>();
	protected int nameCounter = 1;
	
	public OCamlTypeBuilder(Job job, Module module, GProtocolName gpn, Role role) {
		this.job = job;
		this.module = module;
		this.gpn = gpn;
		this.role = role;
	}


	public String build() throws ScribbleException {
		StringBuffer buf = new StringBuffer();		
		EGraph graph = this.job.getContext().getEGraph(this.gpn, this.role);
		buf.append(buildTypes(graph.init));
		return buf.toString();
	}
	
	protected String buildTypes(EState start) throws ScribbleException {
		// to enable delegated type to be forward-ref'd, types are declared as mutually recursive types
		
		StringBuffer buf = new StringBuffer();
		String roleConnTypeParams = getRoleConnTypeParams();
		
		buf.append("type " + roleConnTypeParams + " ");
		
		// local types
		List<EState> toplevels = getRecurringStates(start);		
		buf.append(Util.uncapitalise(gpn.getSimpleName().toString()) + "_" + this.role 
				+ " = " + roleConnTypeParams + " " + getStateChanName(start) + "\n");
		
		for(EState me : toplevels) {
			buf.append("and " + roleConnTypeParams + " " + getStateChanName(me) + " =\n");
			buildTypes(me, buf, toplevels, 0);
			buf.append("\n");
		}
		return buf.toString();
	}
	
	protected String getRoleConnTypeParams() {
		List<Role> roles = module.getProtocolDecl(this.gpn.getSimpleName()).getHeader().roledecls.getRoles();
		return Util.getRoleConnTypeParams(roles, this.role);
	}
	
	// traverse the graph to gather recurring states which will be declared at the top level
	public static List<EState> getRecurringStates(EState start) {
		ArrayList<EState> recurring = new ArrayList<>(), visited = new ArrayList<>();
		recurring.add(start);
		getRecurringStates(start, recurring, visited);
		return recurring;
	}
	
	protected static void getRecurringStates(EState curr, List<EState> recurring, List<EState> visited) {
		// if visited twice, add that state to the toplevels
		if(visited.contains(curr) && !curr.isTerminal()) {
			if( !recurring.contains(curr) ) recurring.add(curr);
		} else {
			visited.add(curr);
			for(EAction action : curr.getActions()) {
				getRecurringStates(curr.getSuccessor(action), recurring, visited);				
			}
		}
	}
	
	protected static EAction getSingleAction(EState s) {
		List<EAction> actions = s.getActions();
		assert (actions.size() == 1);
		return actions.get(0);
	}

	// indent == -1 is the rightmost 
	protected static void indent(StringBuffer buf, int level) {
		for (int i = 0; i < (level + 1) * 2; i++) {
			buf.append(' ');
		}
	}

	protected String payloadTypesToString(List<PayloadElemType<?>> payloads) throws ScribbleException {
		if(payloads.isEmpty()) {
			return "unit data";
			
		} else if (checkPayloadIsDelegation(payloads)) {
			LProtocolName delegated = (LProtocolName)payloads.get(0);
			GProtocolName global = Util.getGlobalNameAndRole(this.job.getContext().getMainModule(), delegated).name;
			String ocamlname = global.getSimpleName().toString() + "."+ Util.uncapitalise(delegated.getSimpleName().toString());			
			return ocamlname + " sess";
			
		} else if (payloads.size()==1) {
			return Util.uncapitalise(payloads.get(0).toString()) + " data";
		} else {
			return "(" + payloads.stream()
					.map(PayloadElemType::toString)
					.map(Util::uncapitalise) // ad hoc renaming -- String -> string for example
					.collect(Collectors.joining(" * ")) + ") data";
		}
	}

	protected static boolean checkAllActions(List<EAction> actions, Predicate<EAction> pred) {
		Boolean found = null;
		for(EAction action : actions) {
			boolean test = pred.test(action);
			if(found != null && found.booleanValue() != test) {
				throw new RuntimeException("[OCaml] non-uniform EAction found");
			}
			if(test) {
				found = true;
			} else {
				found = false;
			}
		}
		return found;
	}

	protected static boolean checkPayloadIsDelegation(List<PayloadElemType<?>> payloads) {
		if (payloads.size() == 0) {
			return false;
		}
		List<PayloadElemType<?>> dataPayloads = payloads.stream()
				.filter((PayloadElemType<?> p) -> p.getClass().equals(DataType.class)).collect(Collectors.toList());

		if (payloads.size() == dataPayloads.size()) {

			return false;

		} else if (payloads.size() == 1) {

			PayloadElemType<?> payload = payloads.get(0);

			@SuppressWarnings("rawtypes")
			Class<? extends PayloadElemType> clazz = payload.getClass();

			if (clazz.equals(LProtocolName.class)) {
				return true;
			} else if (clazz.equals(GDelegationType.class)) {
				throw new RuntimeException("[OCaml] payload GDelegationType is not supported");
			} else {
				throw new RuntimeException("shouldn't get in here:" + clazz);
			}
		} else {
			throw new RuntimeException("[OCaml] non-datatype payload must contain exactly one element");
		}
	}

	protected void buildTypes(EState curr, StringBuffer buf, List<EState> toplevel, int level) throws ScribbleException {

		indent(buf, level);

		if (level != 0 && toplevel.contains(curr)) {
			buf.append(getRoleConnTypeParams() + " ");
			buf.append(getStateChanName(curr));
			return;
		}

		switch (curr.getStateKind()) {
		case OUTPUT:
			boolean isDisconnect = checkAllActions(curr.getActions(), (EAction a) -> a.isDisconnect());
			if (isDisconnect) {
				disconnect(curr, buf, toplevel, level);
			} else {				
				output(curr, buf, toplevel, level);
			}
			break;
		case UNARY_INPUT:
			unaryInput(curr, buf, toplevel, level);
			break;
		case POLY_INPUT:
			polyInput(curr, buf, toplevel, level, false);
			break;
		case TERMINAL:
			buf.append("[`close]");
			break;
		case ACCEPT:
			polyInput(curr, buf, toplevel, level, true);
			break;
		case WRAP_SERVER:
			throw new RuntimeException("TODO");
		default:
			throw new RuntimeException("Shouldn't get in here: " + curr);
		}
	}

	protected void output(EState curr, StringBuffer buf, List<EState> toplevel, int level) throws ScribbleException {
		// output can contain both datatype and non-datatype payload

		String prefix= "[`send of\n";
		buf.append(prefix);
		
		level++;
		indent(buf, level);
		buf.append("[");
		
		boolean middle = false;

		for (EAction action : curr.getActions()) {

			if (middle) {
				buf.append("\n");
				indent(buf, level);
				buf.append("|");
			}
			middle = true;
			
			String roleSuffix = action.isRequest() ? " connect" : "";

			List<PayloadElemType<?>> payloads = action.payload.elems;

			buf.append("`" + Util.label(action.mid) 
					+ " of ([`" + action.peer + "], 'c_" + action.peer + ") role" 
					+ roleSuffix 
					+ " * " + payloadTypesToString(payloads) 
					+ " *\n");

			EState succ = curr.getSuccessor(action);
			buildTypes(succ, buf, toplevel, level + 1);
			
			buf.append(" sess");
		}
		buf.append("]]");
		level--;
	}
	
	protected void disconnect(EState curr, StringBuffer buf, List<EState> toplevel, int level) throws ScribbleException  {
		// labels and paylaods are ignored
		EAction action = getSingleAction(curr);

		String prefix = "[`disconnect of ([`" + action.peer + "], 'c_" + action.peer + ") role *\n";
		buf.append(prefix);
				
		EState succ = curr.getSuccessor(action);
		buildTypes(succ, buf, toplevel, level + 1);
		buf.append(" sess]");
	}
	
	protected void unaryInput(EState curr, StringBuffer buf, List<EState> toplevel, int level) throws ScribbleException {
		EAction action = getSingleAction(curr);
		List<PayloadElemType<?>> payloads = action.payload.elems;
		buf.append("[`recv of ([`" + action.peer + "], 'c_" + action.peer + ") role * "
				+"[`" + Util.label(action.mid)
				+ " of " + payloadTypesToString(payloads) 
				+ " *\n");
		
		EState succ = curr.getSuccessor(action);
		buildTypes(succ, buf, toplevel, level + 1);
		buf.append(" sess]]");
	}
	
	protected Role getPeer(EState curr) {
		Role r = null;
		for(EAction action : curr.getActions()) {
			if(r == null) {
				r = action.peer;
			} else if(!r.equals(action.peer)) {
				throw new RuntimeException("receiving from different peer: " + r + " and " + action.peer); 
			}
		}
		return r;
	}
	
	protected void polyInput(EState curr, StringBuffer buf, List<EState> toplevel, int level, boolean accept) throws ScribbleException {
		String prefix;
		if(accept) {
			prefix = "[`accept of ";
		} else {
			prefix = "[`recv of ";
		}
		buf.append(prefix);
		
		Role peer = getPeer(curr);
		buf.append("([`" + peer + "], 'c_" + peer + ") role *\n");
		
		level++;
		indent(buf, level);
		buf.append("[");
		
		boolean mid = false;
		for (EAction action : curr.getActions()) {
			List<PayloadElemType<?>> payloads = action.payload.elems;
			if(mid) {
				buf.append("\n");
				indent(buf, level);
				buf.append("|");
			}
			mid = true;
			buf.append("`" + Util.label(action.mid) + " of " + payloadTypesToString(payloads) + " *\n");
			EState succ = curr.getSuccessor(action);
			buildTypes(succ, buf, toplevel, level + 1);
			buf.append(" sess");
		}
		buf.append("]]");
		level--;
	}

	// XXX copied from STStateChanAPIBuilder
	public String getStateChanName(EState s) {
		String name = this.names.get(s.id);
		if (name == null) {
			name = makeSTStateName(s);
			this.names.put(s.id, name);
		}
		return name;
	}
	
	protected String makeSTStateName(EState s) {
		String name = this.gpn.getSimpleName() + "_" + role + "_" + this.nameCounter++;
		return Util.uncapitalise(name);
	}

}

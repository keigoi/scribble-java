package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.scribble.main.Job;
import org.scribble.model.endpoint.EGraph;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.sesstype.name.DataType;
import org.scribble.sesstype.name.GDelegationType;
import org.scribble.sesstype.name.GProtocolName;
import org.scribble.sesstype.name.LProtocolName;
import org.scribble.sesstype.name.PayloadType;
import org.scribble.sesstype.name.Role;

public class OCamlTypeBuilder {
	public final Job job;
	public final GProtocolName gpn;
	public final Role role;
	public final EGraph graph;
	protected Map<Integer, String> names = new HashMap<>();
	protected int counter = 1; // XXX

	public OCamlTypeBuilder(Job job, GProtocolName gpn, Role role, EGraph graph) {
		super();
		this.job = job;
		this.gpn = gpn;
		this.role = role;
		this.graph = graph;
	}

	public String build() {
		return buildTypes(this.graph.init);
	}
	
	protected String buildTypes(EState start) {
		StringBuffer buf = new StringBuffer();
		// local types
		List<EState> toplevels = getRecurringStates(start);		
		buf.append("type " + Util.uncapitalise(gpn.getSimpleName().toString()) + "_" + this.role + " = " + getStateChanName(start) + "\n");
		for(EState me : toplevels) {
			buf.append("and " + getStateChanName(me) + " = \n");
			buildTypes(me, buf, toplevels, 0);
			buf.append("\n");
		}
		return buf.toString();
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

	protected static String payloadTypesToString(List<PayloadType<?>> payloads) {
		if(payloads.isEmpty()) {
			return "unit";
		} else {
			return payloads.stream()
					.map(PayloadType::toString)
					.map(Util::uncapitalise) // ad hoc renaming -- String -> string for example
					.collect(Collectors.joining(","));
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

	protected static boolean checkPayloadIsDelegation(List<PayloadType<?>> payloads) {
		if (payloads.size() == 0) {
			return false;
		}
		List<PayloadType<?>> dataPayloads = payloads.stream()
				.filter((PayloadType<?> p) -> p.getClass().equals(DataType.class)).collect(Collectors.toList());

		if (payloads.size() == dataPayloads.size()) {

			return false;

		} else if (payloads.size() == 1) {

			PayloadType<?> payload = payloads.get(0);

			@SuppressWarnings("rawtypes")
			Class<? extends PayloadType> clazz = payload.getClass();

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

	protected void buildTypes(EState curr, StringBuffer buf, List<EState> toplevel, int level) {

		indent(buf, level);

		if (level != 0 && toplevel.contains(curr)) {
			buf.append(getStateChanName(curr));
			return;
		}

		switch (curr.getStateKind()) {
		case OUTPUT:
			output(curr, buf, toplevel, level);
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

	protected void output(EState curr, StringBuffer buf, List<EState> toplevel, int level) {
		// output can contain both datatype and non-datatype payload

		boolean isConnect = checkAllActions(curr.getActions(), (EAction a) -> a.isConnect());
		boolean isDisConnect = checkAllActions(curr.getActions(), (EAction a) -> a.isDisconnect());
		String prefix;
		if(isConnect) {
			prefix = "[`connect of\n";
		} else if(isDisConnect) {
			prefix = "[`disconnect of\n";			
		} else {
			prefix = "[`send of\n";
		}
		buf.append(prefix);
		
		level++;
		indent(buf, level);
		buf.append("[");
		
		boolean middle = false;

		for (EAction action : curr.getActions()) {
			List<PayloadType<?>> payloads = action.payload.elems;

			if (middle) {
				buf.append("\n");
				indent(buf, level);
				buf.append("|");
			}

			checkPayloadIsDelegation(payloads);

			buf.append("`" + Util.label(action.mid) + " of [`" + action.peer + "] role * " + payloadTypesToString(payloads) + " *\n");

			EState succ = curr.getSuccessor(action);
			buildTypes(succ, buf, toplevel, level + 1);

			middle = true;
		}
		buf.append("]]");
		level--;
	}
	protected void unaryInput(EState curr, StringBuffer buf, List<EState> toplevel, int level) {
		EAction action = getSingleAction(curr);
		List<PayloadType<?>> payloads = action.payload.elems;
		if (checkPayloadIsDelegation(payloads)) {
			buf.append("[`deleg_recv of [`" + Util.label(action.mid) + " of [`" + action.peer + "] role * ");
			buf.append(payloads.get(0).toString() + "\n");
		} else {
			buf.append("[`recv of [`" + Util.label(action.mid) + " of [`" + action.peer + "] role * " + payloadTypesToString(payloads) + " *\n");
		}
		EState succ = curr.getSuccessor(action);
		buildTypes(succ, buf, toplevel, level + 1);
		buf.append("]]");
	}
	
	protected void polyInput(EState curr, StringBuffer buf, List<EState> toplevel, int level, boolean accept) {
		String prefix;
		if(accept) {
			prefix = "[`accept of\n";
		} else {
			prefix = "[`recv of\n";
		}
		buf.append(prefix);
		
		level++;
		indent(buf, level);
		buf.append("[");
		
		boolean mid = false;
		for (EAction action : curr.getActions()) {
			List<PayloadType<?>> payloads = action.payload.elems;
			if (checkPayloadIsDelegation(payloads)) {
				throw new RuntimeException("[OCaml] payload cannot contain delegation in multiple input branch");
			}
			if(mid) {
				buf.append("\n");
				indent(buf, level);
				buf.append("|");
			}
			mid = true;
			buf.append("`" + Util.label(action.mid) + " of [`" + action.peer + "] role * " + payloadTypesToString(payloads) + " *\n");
			EState succ = curr.getSuccessor(action);
			buildTypes(succ, buf, toplevel, level + 1);
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
		String name = this.gpn.getSimpleName() + "_" + role + "_" + this.counter++;
		return Util.uncapitalise(name);
	}

}

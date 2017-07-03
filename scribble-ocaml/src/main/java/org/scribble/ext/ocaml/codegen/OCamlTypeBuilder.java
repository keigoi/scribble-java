package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
		return build(this.graph.init);
	}
	
	protected String build(EState start) {
		ArrayList<EState> toplevels = new ArrayList<>(), visited = new ArrayList<>();
		toplevels.add(start);
		toplevel(start, toplevels, visited);
		
		StringBuffer buf = new StringBuffer();
		buf.append("type " + uncapitalise(gpn.getSimpleName().toString()) + " = " + getStateChanName(start) + "\n");
		for(EState me : toplevels) {
			buf.append("and " + getStateChanName(me) + " = \n");
			build_rec(me, buf, toplevels, 1, true);
			buf.append("\n");
		}
		return buf.toString();
	}
	
	// traverse the graph to gather recurring types which will be declared at the top level
	protected void toplevel(EState curr, List<EState> toplevels, List<EState> visited) {
		// if visited twice, add that state to the toplevels
		if(visited.contains(curr)) {
			if( !toplevels.contains(curr) ) toplevels.add(curr);
		} else {
			visited.add(curr);
			for(EAction action : curr.getActions()) {
				toplevel(curr.getSuccessor(action), toplevels, visited);				
			}
		}
	}

	protected static EAction getSingleAction(EState s) {
		List<EAction> actions = s.getActions();
		assert (actions.size() == 1);
		return actions.get(0);
	}

	protected static void indent(StringBuffer buf, int level) {
		for (int i = 0; i < level * 2; i++) {
			buf.append(' ');
		}
	}

	protected static String payloadTypesToString(List<PayloadType<?>> payloads) {
		if(payloads.isEmpty()) {
			return "unit";
		} else {
			return payloads.stream().map(PayloadType::toString).map(OCamlTypeBuilder::uncapitalise).collect(Collectors.joining(","));
		}
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

	protected void build_rec(EState curr, StringBuffer buf, List<EState> toplevel, int level, boolean init) {

		indent(buf, level);

		if (!init && toplevel.contains(curr)) {
			buf.append(getStateChanName(curr));
			return;
		}

		switch (curr.getStateKind()) {
		case OUTPUT: {
			// output can contain both datatype and non-datatype payload

			boolean mid = false;
			buf.append("[`send of \n");
			level++;
			indent(buf, level);
			buf.append("[");

			for (EAction action : curr.getActions()) {
				List<PayloadType<?>> payloads = action.payload.elems;

				if (mid) {
					buf.append("\n");
					indent(buf, level);
					buf.append("|");
				}

				checkPayloadIsDelegation(payloads);

				buf.append("`" + action.mid + " of [`" + action.peer + "] * " + payloadTypesToString(payloads) + " *\n");

				EState succ = curr.getSuccessor(action);
				build_rec(succ, buf, toplevel, level + 1, false);

				mid = true;
			}
			buf.append("]]");
			level--;
			break;
		}
		case UNARY_INPUT: {
			EAction action = getSingleAction(curr);
			List<PayloadType<?>> payloads = action.payload.elems;
			if (checkPayloadIsDelegation(payloads)) {
				buf.append("[`deleg_recv of [`" + action.mid + " of " + action.peer + " * ");
				buf.append(payloads.get(0).toString() + "\n");
			} else {
				buf.append("[`recv of [`" + action.mid + " of [`" + action.peer + "] * " + payloadTypesToString(payloads) + " *\n");
			}
			EState succ = curr.getSuccessor(action);
			build_rec(succ, buf, toplevel, level + 1, false);
			buf.append("]]");
			break;
		}
		case POLY_INPUT: {
			buf.append("[`recv of\n");
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
				buf.append("`" + action.mid + " of [`" + action.peer + "] * " + payloadTypesToString(payloads) + " *\n");
				EState succ = curr.getSuccessor(action);
				build_rec(succ, buf, toplevel, level + 1, false);
			}
			buf.append("]]");
			level--;
			break;
		}
		case TERMINAL: {
			buf.append("[`close]");
			break;
		}
		case ACCEPT:
			throw new RuntimeException("TODO");
		case WRAP_SERVER:
			throw new RuntimeException("TODO");
		default:
			throw new RuntimeException("Shouldn't get in here: " + curr);
		}
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

	// XXX copied from GSTStateChanAPIBuilder
	protected String makeSTStateName(EState s) {
		if (s.isTerminal()) {
			return "_EndState";
		}
		String name = this.gpn.getSimpleName() + "_" + role + "_" + this.counter++;
		// return (s.id == this.graph.init.id) ? name : "_" + name; // For
		// "protected" non-initial state channels
		return uncapitalise(name);
	}
	
	protected static String uncapitalise(String name) {
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}

}

package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.scribble.ast.DataTypeDecl;
import org.scribble.ast.Module;
import org.scribble.ast.global.GProtocolDecl;
import org.scribble.main.ScribbleException;
import org.scribble.model.MAction;
import org.scribble.model.MState;
import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.type.kind.ProtocolKind;
import org.scribble.type.name.GProtocolName;
import org.scribble.type.name.LProtocolName;
import org.scribble.type.name.MessageId;
import org.scribble.type.name.Role;

public class Util {
	public static final String EXT_SOURCE_FUNCTOR = "_functor";

	public static String capitalise(String name) {
		return Character.toUpperCase(name.charAt(0)) + name.substring(1);
	}

	public static String uncapitalise(String name) {
		return Character.toLowerCase(name.charAt(0)) + name.substring(1);
	}
	
	public static String label(MessageId<?> msg) {
		return label(msg.toString());
	}

	public static String label(String msg) {
		if(msg.matches("^[0-9].*")) {
			return "_" + msg;
		} else if("".equals(msg)) {
			return "msg";
		} else {
			return msg;
		}
	}
	
	public static 
		<T,
		L,
		A extends MAction<K>,
		S extends MState<L, A, S, K>,
		K extends ProtocolKind> T traverse(S state, T init, BiFunction<T, S, T> fun) {
		return traverse(state, init, fun, new ArrayList<>());
	}
	
	private static <T,
	L,
	A extends MAction<K>,
	S extends MState<L, A, S, K>,
	K extends ProtocolKind> T traverse(S state, T init, BiFunction<T, S, T> fun, List<S> visited) {
		if(visited.contains(state)) {
			return init;
		}
		
		visited.add(state);
		
		T accum = init;
		for(S succ : state.getSuccessors()) {
			accum = fun.apply(accum, succ);
		}
		return accum;
	}
	
	public static class GProtocolNameRole {
		public final GProtocolName name;
		public final Role role;
		public GProtocolNameRole(GProtocolName name, Role role) {
			this.name = name;
			this.role = role;
		}
	}
	
	// brain-dead approach to recover global name from a local name
	public static GProtocolName searchGlobalProtocolName(Module module, LProtocolName local) throws ScribbleException {
		String localname = local.getSimpleName().toString();
		for (GProtocolDecl global : module.getGlobalProtocolDecls()) {
			GProtocolName name = global.getHeader().getDeclName();
			String globalname = name.getSimpleName().toString();
			for (Role role : global.header.roledecls.getRoles()) {
				if (localname.equals(globalname + "_" + role.toString())) {
					return new GProtocolNameRole(new GProtocolName(module.getFullModuleName(), name), role).name;
				}
			}
		}
		throw new RuntimeException("[OCaml] cannot find GProtocolName from LProtocolName " + local);
	}

	public static String getRoleConnTypeParams(List<Role> roles, Role myrole) {
		StringBuffer buf = new StringBuffer();
		boolean multiple = false;
		for(Role role : roles) {
			if(role.equals(myrole)) continue;
			if( multiple ) {
				buf.append(", ");
			} else {
				multiple = true;				
			}
			buf.append("'c_" + role);
		}
		return multiple ? "(" + buf.toString() + ")" : buf.toString();
	}

	// traverse the graph to gather recurring states which will be declared at the top level
	public static List<EState> getRecurringStates(EState start) {
		ArrayList<EState> recurring = new ArrayList<>(), visited = new ArrayList<>();
		recurring.add(start);
		Util.getRecurringStates(start, recurring, visited);
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
	
	protected static String receiveFunname(List<String> labels) {
		return labels.stream().map(Util::uncapitalise).collect(Collectors.joining("_or_"));
	}

	
	protected static List<LabelAndPayload> labels(EState state, Predicate<EAction> predicate) {
		return Util.labels(state, predicate, new ArrayList<>());
	}
	
	private static List<LabelAndPayload> labels(EState state, Predicate<EAction> predicate, List<EState> visited) {
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

	protected static List<DataTypeDecl> dataTypeDecls(Module main) {
		return main.getNonProtocolDecls().stream()
				.filter(d -> d.isDataTypeDecl())
				.map(d -> (DataTypeDecl)d)
				.collect(Collectors.toList());
	}
	
	protected static List<DataTypeDecl> functorParamDataTypeDecls(Module main) {
		return dataTypeDecls(main).stream().filter(d -> d.extSource.equals(EXT_SOURCE_FUNCTOR)).collect(Collectors.toList());
	}
}

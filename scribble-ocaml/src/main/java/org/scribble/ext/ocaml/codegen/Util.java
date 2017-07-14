package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

import org.scribble.model.MAction;
import org.scribble.model.MState;
import org.scribble.sesstype.kind.ProtocolKind;
import org.scribble.sesstype.name.MessageId;

public class Util {

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
}

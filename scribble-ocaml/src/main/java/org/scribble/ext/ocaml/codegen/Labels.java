package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.scribble.model.endpoint.EState;
import org.scribble.model.endpoint.actions.EAction;
import org.scribble.type.name.Role;

public class Labels {

	public static List<LabelAndPayload> enumerateAllLabels(EState state) {
		return labels(state, a -> true, new ArrayList<>());
	}

	public static List<List<LabelAndPayload>> enumerateRecvLabels(Role peer, EState state) {
		return recvLabels(peer, state, new ArrayList<>());
	}

	public static List<LabelAndPayload> enumrateSendLabels(Role peer, EState state) {
		return labels(state, a -> a.peer.equals(peer) && (a.isSend() || a.isRequest()), new ArrayList<>());
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

	private static List<List<LabelAndPayload>> recvLabels(Role peer, EState state, List<EState> visited) {
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

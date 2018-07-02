package org.scribble.ext.ocaml.codegen;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.scribble.ast.DataTypeDecl;
import org.scribble.ast.Module;
import org.scribble.main.Job;
import org.scribble.model.endpoint.EState;
import org.scribble.type.name.DataType;
import org.scribble.type.name.PayloadElemType;

public class DataTypes {

	protected static List<DataTypeDecl> dataTypeDecls(Job job, Set<PayloadElemType<?>> payloads) {
		Module main = job.getContext().getMainModule();
		
		Set<DataTypeDecl> occurring = 
				payloads.stream()
				.filter(typ -> typ.isDataType())
				.map(typ -> main.getDataTypeDecl((DataType)typ))
				.collect(Collectors.toSet());
				
		return main.getNonProtocolDecls().stream()
				.filter(d -> occurring.contains(d))
				.map(d -> (DataTypeDecl)d)
				.collect(Collectors.toList());
	}

	protected static Set<PayloadElemType<?>> payloads(EState state) {
		return Labels.enumerateAllLabels(state)
				.stream()
				.flatMap(l -> l.payloads.stream())
				.collect(Collectors.toSet());
	}

}

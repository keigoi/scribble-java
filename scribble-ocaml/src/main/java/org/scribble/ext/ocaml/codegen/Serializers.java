package org.scribble.ext.ocaml.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.scribble.model.endpoint.EState;
import org.scribble.type.name.Role;

public class Serializers {
	
	public static final String serializerFormat = 
	  "  module %ROLE = struct\n"
	+ "    module Make(X:sig\n"
	+ "        type conn\n"
	+ "        val conn : conn Endpoint.conn_kind\n"
	+ "%LABEL_MODULE_ARGS"
	+ "      end) = struct\n"
	+ "      type conn = X.conn\n"
	+ "      let role : ([>`%ROLE of X.conn * 'lab], X.conn, 'lab) role =\n"
	+ "        {_pack_role=(fun labels -> `%ROLE(labels)) ; _repr=\"role_%ROLE\"; _kind=X.conn}\n\n"
	+ "%LABEL_MODULE_CONTENT"
	+ "    end\n\n"
	+ "    module Raw = struct\n"
	+ "      include Make(struct\n"
	+ "          type conn = Shmem.Raw.t\n"
	+ "          let conn = Shmem.MPSTChannel.Raw\n"
	+ "%SHMEM_LABEL_MODULE_CONTENT"
	+ "        end)\n"
	+ "    end\n"
	+ "  end";

	public static final String sendLabelFormat =
	    "      let %FUNNAME : 'p. ([>`%LABEL of %PAYLOAD * 'p sess], X.conn, %PAYLOAD * 'p sess) label =\n"
	  + "        {_pack_label=(fun payload -> `%LABEL(payload)); _send=X.write_%FUNNAME}";
	
	public static final String shmemWriterImplFormat = 
	  "          let write_%FUNNAME = Shmem.Raw.send";
	
	public static final String writerSigFormat = 
	"        val write_%FUNNAME : conn -> [>`%LABEL of %PAYLOAD * 'p sess] -> unit io";
	
	public static final String recvLabelFormat =
	    "      let receive_%FUNNAME  : type %TYVARS. (%LABELS, X.conn) labels =\n"
	  + "        {_receive=X.read_%FUNNAME}";
	
	public static final String readerSigFormat = 
	  "        val read_%FUNNAME : conn -> %LABELS io";
	
	public static final String shmemReaderImplFormat = 
	  "          let read_%FUNNAME = Shmem.Raw.receive";

	public static String generateSerializers(HashMap<Role, EState> inits, Role me, Role peer) {
		List<LabelAndPayload> sendLabels = new ArrayList<>(); 
		List<List<LabelAndPayload>> recvLabels = new ArrayList<>(); 
		EState state = inits.get(me);
		sendLabels.addAll(Labels.enumrateSendLabels(peer, state));
		recvLabels.addAll(Labels.enumerateRecvLabels(peer, state));
		
		StringBuffer moduleArgBuffer = new StringBuffer(), 
				moduleContentBuffer = new StringBuffer(),
				shmemModuleContentBuffer = new StringBuffer();
		
		HashMap<String, Integer> labelDupCheck = new HashMap<>();	
		for(LabelAndPayload label: sendLabels) {
			int count = labelDupCheck.getOrDefault(label.label, 0);
			labelDupCheck.put(label.label, count+1);
			String funname = Util.uncapitalise(Util.label(label.label)) + (count == 0 ? "" : "_" + count); 
			moduleArgBuffer.append(writerSigFormat.replace("%FUNNAME", funname).replace("%LABEL", Util.label(label.label)).replace("%PAYLOAD", label.getPayloadTypeRepr()));
			moduleArgBuffer.append('\n');
			moduleContentBuffer.append(sendLabelFormat.replace("%FUNNAME", funname).replace("%LABEL", Util.label(label.label)).replace("%PAYLOAD", label.getPayloadTypeRepr()));
			moduleContentBuffer.append('\n');
			shmemModuleContentBuffer.append(shmemWriterImplFormat.replace("%FUNNAME", funname));
			shmemModuleContentBuffer.append('\n');
		}
		
		HashMap<List<String>, Integer> branchDupCheck = new HashMap<>();	
		for(List<LabelAndPayload> branch: recvLabels) {
			
			List<String> labels = branch.stream().map(b -> Util.label(b.label)).collect(Collectors.toList());
			
			int count = branchDupCheck.getOrDefault(labels, 0);
			branchDupCheck.put(labels, count+1);
			
			String funname = labels.stream().map(Util::uncapitalise).collect(Collectors.joining("_or_")) 
					+ (count==0 ? "" : "_" + count);
			String tyvars = IntStream.range(0, branch.size()).mapToObj(i -> "p" + i).collect(Collectors.joining(" "));
			
			moduleArgBuffer.append(
					readerSigFormat
					.replace("%FUNNAME", funname)
					.replace("%LABELS", Util.generatePolyvarBranch(branch, false)));
			moduleArgBuffer.append('\n');
			
			moduleContentBuffer.append(
					recvLabelFormat
					.replace("%FUNNAME", funname)
					.replace("%LABELS", Util.generatePolyvarBranch(branch, true))
					.replace("%TYVARS", tyvars));
			moduleContentBuffer.append('\n');
			
			shmemModuleContentBuffer.append(shmemReaderImplFormat.replace("%FUNNAME", funname));
			shmemModuleContentBuffer.append('\n');
		}
		
		return serializerFormat
				.replace("%ROLE", peer.toString())
				.replace("%LABEL_MODULE_ARGS", moduleArgBuffer.toString())
				.replace("%LABEL_MODULE_CONTENT", moduleContentBuffer.toString())
				.replace("%SHMEM_LABEL_MODULE_CONTENT", shmemModuleContentBuffer.toString());
	}

}

package org.scribble.ext.ocaml.codegen;

import java.util.List;
import java.util.Objects;

import org.scribble.type.name.PayloadElemType;

final class LabelAndPayload {
	public final String label;
	public final List<PayloadElemType<?>> payloads;
	public LabelAndPayload(String label, List<PayloadElemType<?>> payloads) {
		this.label = label;
		this.payloads = payloads;
	}
	@Override
	public int hashCode() {
		return Objects.hash(label, getPayloadTypeRepr());
	}
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (getClass() != obj.getClass()) return false;
		LabelAndPayload other = (LabelAndPayload) obj;
		return Objects.equals(this.label, other.label) && Objects.equals(this.getPayloadTypeRepr(), other.getPayloadTypeRepr());
	}
	@Override
	public String toString() {
		return label+"<" + getPayloadTypeRepr() + ">";
	}
	public String getPayloadTypeRepr() {
		return OCamlTypeBuilder.payloadTypesToString(payloads);
	}
}
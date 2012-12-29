/*
 * Copyright 2009-11 www.scribble.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.scribble.protocol.parser.antlr;

import java.util.Stack;

import org.antlr.runtime.CommonToken;
import org.scribble.protocol.model.Parameter;
import org.scribble.protocol.model.RoleInstantiation;
import org.scribble.protocol.model.local.LCreate;

public class CreateModelAdaptor implements ModelAdaptor {

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public Object createModelObject(Stack<Object> components) {		
		LCreate ret=new LCreate();

		components.pop(); // )
		
		ret.getRoleInstantiations().addAll((java.util.List<RoleInstantiation>)components.pop());
		
		components.pop(); // (
		
		if (components.peek() instanceof CommonToken
				&& ((CommonToken)components.peek()).getText().equals(">")) {
			components.pop(); // >
			
			ret.getParameters().addAll((java.util.List<Parameter>)components.pop());

			components.pop(); // <
		}
		
		ret.setProtocol(((CommonToken)components.pop()).getText());
		
		components.pop(); // create

		components.push(ret);
			
		return ret;
	}

}

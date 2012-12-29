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

import java.util.logging.Logger;

import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.Token;
import org.antlr.runtime.TokenStream;

import org.scribble.protocol.model.Module;
import org.scribble.protocol.parser.IssueLogger;

/**
 * This class provides the antlr tree adaptor.
 *
 */
public class ProtocolTreeAdaptor implements org.antlr.runtime.tree.TreeAdaptor {
	
	private static final Logger LOG=Logger.getLogger(ProtocolTreeAdaptor.class.getName());

	private IssueLogger _logger=null;
	private java.util.Stack<Object> _components=new java.util.Stack<Object>();
    private ScribbleProtocolParser _parser=null;
    
	public ProtocolTreeAdaptor(IssueLogger logger) {
		_logger = logger;
	}
	
	public Module getModule() {
		return (_components.peek() instanceof Module ? (Module)_components.peek() : null);
	}
	
    /**
     * This method sets the parser.
     * 
     * @param parser The parser
     */
    public void setParser(ScribbleProtocolParser parser) {
        _parser = parser;
    }

    public void addChild(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

	public Object becomeRoot(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object becomeRoot(Token arg0, Object arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object create(Token arg0) {
		LOG.info("CREATE TOKEN="+arg0);
		_components.push(arg0);
		return null;
	}

	public Object create(int arg0, Token arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object create(int arg0, String arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object create(int arg0, Token arg1, String arg2) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object deleteChild(Object arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object dupNode(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object dupTree(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object errorNode(TokenStream arg0, Token arg1, Token arg2,
			RecognitionException arg3) {
		// TODO Auto-generated method stub
		return null;
	}

	public Object getChild(Object arg0, int arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getChildCount(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getChildIndex(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public Object getParent(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public String getText(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public Token getToken(Object arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	public int getTokenStartIndex(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getTokenStopIndex(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getType(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public int getUniqueID(Object arg0) {
		// TODO Auto-generated method stub
		return 0;
	}

	public boolean isNil(Object arg0) {
		// TODO Auto-generated method stub
		return false;
	}

	public Object nil() {
		// TODO Auto-generated method stub
		return null;
	}

	public void replaceChildren(Object arg0, int arg1, int arg2, Object arg3) {
		// TODO Auto-generated method stub
		
	}

	public Object rulePostProcessing(Object arg0) {
		Object ret=arg0;
		
        if (_parser != null
                && _parser.getRuleInvocationStack().size() > 0) {
            String ruleName=(String)_parser.getRuleInvocationStack().get(
                    _parser.getRuleInvocationStack().size()-1);
            
    		LOG.info("RULE POST PROCESSING="+ruleName);
    		
    		ModelAdaptor modelAdaptor=ModelAdaptorFactory.getModelAdaptor(ruleName);
    		
    		if (modelAdaptor != null) {
    			ret = modelAdaptor.createModelObject(_components);
    		}
        }

		return ret;
	}

	public void setChild(Object arg0, int arg1, Object arg2) {
		// TODO Auto-generated method stub
		
	}

	public void setChildIndex(Object arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

	public void setParent(Object arg0, Object arg1) {
		// TODO Auto-generated method stub
		
	}

	public void setText(Object arg0, String arg1) {
		// TODO Auto-generated method stub
		
	}

	public void setTokenBoundaries(Object arg0, Token arg1, Token arg2) {
		// TODO Auto-generated method stub
		
	}

	public void setType(Object arg0, int arg1) {
		// TODO Auto-generated method stub
		
	}

}

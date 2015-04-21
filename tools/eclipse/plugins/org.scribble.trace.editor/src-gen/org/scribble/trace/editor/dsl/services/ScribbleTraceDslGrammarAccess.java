/*
* generated by Xtext
*/
package org.scribble.trace.editor.dsl.services;

import com.google.inject.Singleton;
import com.google.inject.Inject;

import java.util.List;

import org.eclipse.xtext.*;
import org.eclipse.xtext.service.GrammarProvider;
import org.eclipse.xtext.service.AbstractElementFinder.*;

import org.eclipse.xtext.common.services.TerminalsGrammarAccess;

@Singleton
public class ScribbleTraceDslGrammarAccess extends AbstractGrammarElementFinder {
	
	
	public class TraceElements extends AbstractParserRuleElementFinder {
		private final ParserRule rule = (ParserRule) GrammarUtil.findRuleForName(getGrammar(), "Trace");
		private final Group cGroup = (Group)rule.eContents().get(1);
		private final Action cTraceAction_0 = (Action)cGroup.eContents().get(0);
		private final RuleCall cTracedefnParserRuleCall_1 = (RuleCall)cGroup.eContents().get(1);
		private final Assignment cRolesAssignment_2 = (Assignment)cGroup.eContents().get(2);
		private final RuleCall cRolesRoledefnParserRuleCall_2_0 = (RuleCall)cRolesAssignment_2.eContents().get(0);
		private final Assignment cStepsAssignment_3 = (Assignment)cGroup.eContents().get(3);
		private final RuleCall cStepsStepdefnParserRuleCall_3_0 = (RuleCall)cStepsAssignment_3.eContents().get(0);
		
		//Trace:
		//	{Trace} Tracedefn roles+=Roledefn* steps+=Stepdefn*;
		public ParserRule getRule() { return rule; }

		//{Trace} Tracedefn roles+=Roledefn* steps+=Stepdefn*
		public Group getGroup() { return cGroup; }

		//{Trace}
		public Action getTraceAction_0() { return cTraceAction_0; }

		//Tracedefn
		public RuleCall getTracedefnParserRuleCall_1() { return cTracedefnParserRuleCall_1; }

		//roles+=Roledefn*
		public Assignment getRolesAssignment_2() { return cRolesAssignment_2; }

		//Roledefn
		public RuleCall getRolesRoledefnParserRuleCall_2_0() { return cRolesRoledefnParserRuleCall_2_0; }

		//steps+=Stepdefn*
		public Assignment getStepsAssignment_3() { return cStepsAssignment_3; }

		//Stepdefn
		public RuleCall getStepsStepdefnParserRuleCall_3_0() { return cStepsStepdefnParserRuleCall_3_0; }
	}

	public class SentenceElements extends AbstractParserRuleElementFinder {
		private final ParserRule rule = (ParserRule) GrammarUtil.findRuleForName(getGrammar(), "Sentence");
		private final RuleCall cIDTerminalRuleCall = (RuleCall)rule.eContents().get(1);
		
		//Sentence:
		//	ID+;
		public ParserRule getRule() { return rule; }

		//ID+
		public RuleCall getIDTerminalRuleCall() { return cIDTerminalRuleCall; }
	}

	public class TracedefnElements extends AbstractParserRuleElementFinder {
		private final ParserRule rule = (ParserRule) GrammarUtil.findRuleForName(getGrammar(), "Tracedefn");
		private final Group cGroup = (Group)rule.eContents().get(1);
		private final Keyword cTraceKeyword_0 = (Keyword)cGroup.eContents().get(0);
		private final RuleCall cIDTerminalRuleCall_1 = (RuleCall)cGroup.eContents().get(1);
		private final Group cGroup_2 = (Group)cGroup.eContents().get(2);
		private final Keyword cByKeyword_2_0 = (Keyword)cGroup_2.eContents().get(0);
		private final RuleCall cSentenceParserRuleCall_2_1 = (RuleCall)cGroup_2.eContents().get(1);
		private final Group cGroup_2_2 = (Group)cGroup_2.eContents().get(2);
		private final Keyword cShowsKeyword_2_2_0 = (Keyword)cGroup_2_2.eContents().get(0);
		private final RuleCall cSentenceParserRuleCall_2_2_1 = (RuleCall)cGroup_2_2.eContents().get(1);
		private final Keyword cSemicolonKeyword_3 = (Keyword)cGroup.eContents().get(3);
		
		//Tracedefn:
		//	"trace" ID ("by" Sentence ("shows" Sentence)?)? ";";
		public ParserRule getRule() { return rule; }

		//"trace" ID ("by" Sentence ("shows" Sentence)?)? ";"
		public Group getGroup() { return cGroup; }

		//"trace"
		public Keyword getTraceKeyword_0() { return cTraceKeyword_0; }

		//ID
		public RuleCall getIDTerminalRuleCall_1() { return cIDTerminalRuleCall_1; }

		//("by" Sentence ("shows" Sentence)?)?
		public Group getGroup_2() { return cGroup_2; }

		//"by"
		public Keyword getByKeyword_2_0() { return cByKeyword_2_0; }

		//Sentence
		public RuleCall getSentenceParserRuleCall_2_1() { return cSentenceParserRuleCall_2_1; }

		//("shows" Sentence)?
		public Group getGroup_2_2() { return cGroup_2_2; }

		//"shows"
		public Keyword getShowsKeyword_2_2_0() { return cShowsKeyword_2_2_0; }

		//Sentence
		public RuleCall getSentenceParserRuleCall_2_2_1() { return cSentenceParserRuleCall_2_2_1; }

		//";"
		public Keyword getSemicolonKeyword_3() { return cSemicolonKeyword_3; }
	}

	public class ModuleElements extends AbstractParserRuleElementFinder {
		private final ParserRule rule = (ParserRule) GrammarUtil.findRuleForName(getGrammar(), "Module");
		private final Group cGroup = (Group)rule.eContents().get(1);
		private final RuleCall cIDTerminalRuleCall_0 = (RuleCall)cGroup.eContents().get(0);
		private final Group cGroup_1 = (Group)cGroup.eContents().get(1);
		private final Keyword cFullStopKeyword_1_0 = (Keyword)cGroup_1.eContents().get(0);
		private final RuleCall cIDTerminalRuleCall_1_1 = (RuleCall)cGroup_1.eContents().get(1);
		
		//Module:
		//	ID ("." ID)*;
		public ParserRule getRule() { return rule; }

		//ID ("." ID)*
		public Group getGroup() { return cGroup; }

		//ID
		public RuleCall getIDTerminalRuleCall_0() { return cIDTerminalRuleCall_0; }

		//("." ID)*
		public Group getGroup_1() { return cGroup_1; }

		//"."
		public Keyword getFullStopKeyword_1_0() { return cFullStopKeyword_1_0; }

		//ID
		public RuleCall getIDTerminalRuleCall_1_1() { return cIDTerminalRuleCall_1_1; }
	}

	public class RoledefnElements extends AbstractParserRuleElementFinder {
		private final ParserRule rule = (ParserRule) GrammarUtil.findRuleForName(getGrammar(), "Roledefn");
		private final Group cGroup = (Group)rule.eContents().get(1);
		private final Keyword cRoleKeyword_0 = (Keyword)cGroup.eContents().get(0);
		private final RuleCall cIDTerminalRuleCall_1 = (RuleCall)cGroup.eContents().get(1);
		private final Group cGroup_2 = (Group)cGroup.eContents().get(2);
		private final Keyword cSimulatingKeyword_2_0 = (Keyword)cGroup_2.eContents().get(0);
		private final RuleCall cModuleParserRuleCall_2_1 = (RuleCall)cGroup_2.eContents().get(1);
		private final Keyword cProtocolKeyword_2_2 = (Keyword)cGroup_2.eContents().get(2);
		private final RuleCall cIDTerminalRuleCall_2_3 = (RuleCall)cGroup_2.eContents().get(3);
		private final Group cGroup_2_4 = (Group)cGroup_2.eContents().get(4);
		private final Keyword cAsKeyword_2_4_0 = (Keyword)cGroup_2_4.eContents().get(0);
		private final RuleCall cIDTerminalRuleCall_2_4_1 = (RuleCall)cGroup_2_4.eContents().get(1);
		private final Keyword cSemicolonKeyword_3 = (Keyword)cGroup.eContents().get(3);
		
		//Roledefn:
		//	"role" ID ("simulating" Module "protocol" ID ("as" ID)?)? ";";
		public ParserRule getRule() { return rule; }

		//"role" ID ("simulating" Module "protocol" ID ("as" ID)?)? ";"
		public Group getGroup() { return cGroup; }

		//"role"
		public Keyword getRoleKeyword_0() { return cRoleKeyword_0; }

		//ID
		public RuleCall getIDTerminalRuleCall_1() { return cIDTerminalRuleCall_1; }

		//("simulating" Module "protocol" ID ("as" ID)?)?
		public Group getGroup_2() { return cGroup_2; }

		//"simulating"
		public Keyword getSimulatingKeyword_2_0() { return cSimulatingKeyword_2_0; }

		//Module
		public RuleCall getModuleParserRuleCall_2_1() { return cModuleParserRuleCall_2_1; }

		//"protocol"
		public Keyword getProtocolKeyword_2_2() { return cProtocolKeyword_2_2; }

		//ID
		public RuleCall getIDTerminalRuleCall_2_3() { return cIDTerminalRuleCall_2_3; }

		//("as" ID)?
		public Group getGroup_2_4() { return cGroup_2_4; }

		//"as"
		public Keyword getAsKeyword_2_4_0() { return cAsKeyword_2_4_0; }

		//ID
		public RuleCall getIDTerminalRuleCall_2_4_1() { return cIDTerminalRuleCall_2_4_1; }

		//";"
		public Keyword getSemicolonKeyword_3() { return cSemicolonKeyword_3; }
	}

	public class StepdefnElements extends AbstractParserRuleElementFinder {
		private final ParserRule rule = (ParserRule) GrammarUtil.findRuleForName(getGrammar(), "Stepdefn");
		private final RuleCall cMessagetransferParserRuleCall = (RuleCall)rule.eContents().get(1);
		
		//Stepdefn:
		//	Messagetransfer;
		public ParserRule getRule() { return rule; }

		//Messagetransfer
		public RuleCall getMessagetransferParserRuleCall() { return cMessagetransferParserRuleCall; }
	}

	public class MessagetransferElements extends AbstractParserRuleElementFinder {
		private final ParserRule rule = (ParserRule) GrammarUtil.findRuleForName(getGrammar(), "Messagetransfer");
		private final Group cGroup = (Group)rule.eContents().get(1);
		private final Action cMessagetransferAction_0 = (Action)cGroup.eContents().get(0);
		private final RuleCall cIDTerminalRuleCall_1 = (RuleCall)cGroup.eContents().get(1);
		private final Group cGroup_2 = (Group)cGroup.eContents().get(2);
		private final Keyword cLeftParenthesisKeyword_2_0 = (Keyword)cGroup_2.eContents().get(0);
		private final Group cGroup_2_1 = (Group)cGroup_2.eContents().get(1);
		private final Assignment cParametersAssignment_2_1_0 = (Assignment)cGroup_2_1.eContents().get(0);
		private final RuleCall cParametersParameterParserRuleCall_2_1_0_0 = (RuleCall)cParametersAssignment_2_1_0.eContents().get(0);
		private final Group cGroup_2_1_1 = (Group)cGroup_2_1.eContents().get(1);
		private final Keyword cCommaKeyword_2_1_1_0 = (Keyword)cGroup_2_1_1.eContents().get(0);
		private final Assignment cParametersAssignment_2_1_1_1 = (Assignment)cGroup_2_1_1.eContents().get(1);
		private final RuleCall cParametersParameterParserRuleCall_2_1_1_1_0 = (RuleCall)cParametersAssignment_2_1_1_1.eContents().get(0);
		private final Keyword cRightParenthesisKeyword_2_2 = (Keyword)cGroup_2.eContents().get(2);
		private final Keyword cFromKeyword_3 = (Keyword)cGroup.eContents().get(3);
		private final RuleCall cIDTerminalRuleCall_4 = (RuleCall)cGroup.eContents().get(4);
		private final Keyword cToKeyword_5 = (Keyword)cGroup.eContents().get(5);
		private final RuleCall cIDTerminalRuleCall_6 = (RuleCall)cGroup.eContents().get(6);
		private final Group cGroup_7 = (Group)cGroup.eContents().get(7);
		private final Keyword cCommaKeyword_7_0 = (Keyword)cGroup_7.eContents().get(0);
		private final RuleCall cIDTerminalRuleCall_7_1 = (RuleCall)cGroup_7.eContents().get(1);
		private final Keyword cSemicolonKeyword_8 = (Keyword)cGroup.eContents().get(8);
		
		//Messagetransfer:
		//	{Messagetransfer} ID ("(" (parameters+=Parameter ("," parameters+=Parameter)*)? ")")? "from" ID "to" ID ("," ID)* ";";
		public ParserRule getRule() { return rule; }

		//{Messagetransfer} ID ("(" (parameters+=Parameter ("," parameters+=Parameter)*)? ")")? "from" ID "to" ID ("," ID)* ";"
		public Group getGroup() { return cGroup; }

		//{Messagetransfer}
		public Action getMessagetransferAction_0() { return cMessagetransferAction_0; }

		//ID
		public RuleCall getIDTerminalRuleCall_1() { return cIDTerminalRuleCall_1; }

		//("(" (parameters+=Parameter ("," parameters+=Parameter)*)? ")")?
		public Group getGroup_2() { return cGroup_2; }

		//"("
		public Keyword getLeftParenthesisKeyword_2_0() { return cLeftParenthesisKeyword_2_0; }

		//(parameters+=Parameter ("," parameters+=Parameter)*)?
		public Group getGroup_2_1() { return cGroup_2_1; }

		//parameters+=Parameter
		public Assignment getParametersAssignment_2_1_0() { return cParametersAssignment_2_1_0; }

		//Parameter
		public RuleCall getParametersParameterParserRuleCall_2_1_0_0() { return cParametersParameterParserRuleCall_2_1_0_0; }

		//("," parameters+=Parameter)*
		public Group getGroup_2_1_1() { return cGroup_2_1_1; }

		//","
		public Keyword getCommaKeyword_2_1_1_0() { return cCommaKeyword_2_1_1_0; }

		//parameters+=Parameter
		public Assignment getParametersAssignment_2_1_1_1() { return cParametersAssignment_2_1_1_1; }

		//Parameter
		public RuleCall getParametersParameterParserRuleCall_2_1_1_1_0() { return cParametersParameterParserRuleCall_2_1_1_1_0; }

		//")"
		public Keyword getRightParenthesisKeyword_2_2() { return cRightParenthesisKeyword_2_2; }

		//"from"
		public Keyword getFromKeyword_3() { return cFromKeyword_3; }

		//ID
		public RuleCall getIDTerminalRuleCall_4() { return cIDTerminalRuleCall_4; }

		//"to"
		public Keyword getToKeyword_5() { return cToKeyword_5; }

		//ID
		public RuleCall getIDTerminalRuleCall_6() { return cIDTerminalRuleCall_6; }

		//("," ID)*
		public Group getGroup_7() { return cGroup_7; }

		//","
		public Keyword getCommaKeyword_7_0() { return cCommaKeyword_7_0; }

		//ID
		public RuleCall getIDTerminalRuleCall_7_1() { return cIDTerminalRuleCall_7_1; }

		//";"
		public Keyword getSemicolonKeyword_8() { return cSemicolonKeyword_8; }
	}

	public class ParameterElements extends AbstractParserRuleElementFinder {
		private final ParserRule rule = (ParserRule) GrammarUtil.findRuleForName(getGrammar(), "Parameter");
		private final Group cGroup = (Group)rule.eContents().get(1);
		private final Assignment cTypeAssignment_0 = (Assignment)cGroup.eContents().get(0);
		private final RuleCall cTypeSTRINGTerminalRuleCall_0_0 = (RuleCall)cTypeAssignment_0.eContents().get(0);
		private final Group cGroup_1 = (Group)cGroup.eContents().get(1);
		private final Keyword cEqualsSignKeyword_1_0 = (Keyword)cGroup_1.eContents().get(0);
		private final Assignment cValueAssignment_1_1 = (Assignment)cGroup_1.eContents().get(1);
		private final RuleCall cValueSTRINGTerminalRuleCall_1_1_0 = (RuleCall)cValueAssignment_1_1.eContents().get(0);
		
		//Parameter:
		//	type=STRING ("=" value=STRING)?;
		public ParserRule getRule() { return rule; }

		//type=STRING ("=" value=STRING)?
		public Group getGroup() { return cGroup; }

		//type=STRING
		public Assignment getTypeAssignment_0() { return cTypeAssignment_0; }

		//STRING
		public RuleCall getTypeSTRINGTerminalRuleCall_0_0() { return cTypeSTRINGTerminalRuleCall_0_0; }

		//("=" value=STRING)?
		public Group getGroup_1() { return cGroup_1; }

		//"="
		public Keyword getEqualsSignKeyword_1_0() { return cEqualsSignKeyword_1_0; }

		//value=STRING
		public Assignment getValueAssignment_1_1() { return cValueAssignment_1_1; }

		//STRING
		public RuleCall getValueSTRINGTerminalRuleCall_1_1_0() { return cValueSTRINGTerminalRuleCall_1_1_0; }
	}
	
	
	private final TraceElements pTrace;
	private final SentenceElements pSentence;
	private final TracedefnElements pTracedefn;
	private final ModuleElements pModule;
	private final RoledefnElements pRoledefn;
	private final StepdefnElements pStepdefn;
	private final MessagetransferElements pMessagetransfer;
	private final ParameterElements pParameter;
	
	private final Grammar grammar;

	private final TerminalsGrammarAccess gaTerminals;

	@Inject
	public ScribbleTraceDslGrammarAccess(GrammarProvider grammarProvider,
		TerminalsGrammarAccess gaTerminals) {
		this.grammar = internalFindGrammar(grammarProvider);
		this.gaTerminals = gaTerminals;
		this.pTrace = new TraceElements();
		this.pSentence = new SentenceElements();
		this.pTracedefn = new TracedefnElements();
		this.pModule = new ModuleElements();
		this.pRoledefn = new RoledefnElements();
		this.pStepdefn = new StepdefnElements();
		this.pMessagetransfer = new MessagetransferElements();
		this.pParameter = new ParameterElements();
	}
	
	protected Grammar internalFindGrammar(GrammarProvider grammarProvider) {
		Grammar grammar = grammarProvider.getGrammar(this);
		while (grammar != null) {
			if ("org.scribble.trace.editor.dsl.ScribbleTraceDsl".equals(grammar.getName())) {
				return grammar;
			}
			List<Grammar> grammars = grammar.getUsedGrammars();
			if (!grammars.isEmpty()) {
				grammar = grammars.iterator().next();
			} else {
				return null;
			}
		}
		return grammar;
	}
	
	
	public Grammar getGrammar() {
		return grammar;
	}
	

	public TerminalsGrammarAccess getTerminalsGrammarAccess() {
		return gaTerminals;
	}

	
	//Trace:
	//	{Trace} Tracedefn roles+=Roledefn* steps+=Stepdefn*;
	public TraceElements getTraceAccess() {
		return pTrace;
	}
	
	public ParserRule getTraceRule() {
		return getTraceAccess().getRule();
	}

	//Sentence:
	//	ID+;
	public SentenceElements getSentenceAccess() {
		return pSentence;
	}
	
	public ParserRule getSentenceRule() {
		return getSentenceAccess().getRule();
	}

	//Tracedefn:
	//	"trace" ID ("by" Sentence ("shows" Sentence)?)? ";";
	public TracedefnElements getTracedefnAccess() {
		return pTracedefn;
	}
	
	public ParserRule getTracedefnRule() {
		return getTracedefnAccess().getRule();
	}

	//Module:
	//	ID ("." ID)*;
	public ModuleElements getModuleAccess() {
		return pModule;
	}
	
	public ParserRule getModuleRule() {
		return getModuleAccess().getRule();
	}

	//Roledefn:
	//	"role" ID ("simulating" Module "protocol" ID ("as" ID)?)? ";";
	public RoledefnElements getRoledefnAccess() {
		return pRoledefn;
	}
	
	public ParserRule getRoledefnRule() {
		return getRoledefnAccess().getRule();
	}

	//Stepdefn:
	//	Messagetransfer;
	public StepdefnElements getStepdefnAccess() {
		return pStepdefn;
	}
	
	public ParserRule getStepdefnRule() {
		return getStepdefnAccess().getRule();
	}

	//Messagetransfer:
	//	{Messagetransfer} ID ("(" (parameters+=Parameter ("," parameters+=Parameter)*)? ")")? "from" ID "to" ID ("," ID)* ";";
	public MessagetransferElements getMessagetransferAccess() {
		return pMessagetransfer;
	}
	
	public ParserRule getMessagetransferRule() {
		return getMessagetransferAccess().getRule();
	}

	//Parameter:
	//	type=STRING ("=" value=STRING)?;
	public ParameterElements getParameterAccess() {
		return pParameter;
	}
	
	public ParserRule getParameterRule() {
		return getParameterAccess().getRule();
	}

	//terminal ID:
	//	"^"? ("a".."z" | "A".."Z" | "_") ("a".."z" | "A".."Z" | "_" | "0".."9")*;
	public TerminalRule getIDRule() {
		return gaTerminals.getIDRule();
	} 

	//terminal INT returns ecore::EInt:
	//	"0".."9"+;
	public TerminalRule getINTRule() {
		return gaTerminals.getINTRule();
	} 

	//terminal STRING:
	//	"\"" ("\\" . / * 'b'|'t'|'n'|'f'|'r'|'u'|'"'|"'"|'\\' * / | !("\\" | "\""))* "\"" | "\'" ("\\" .
	//	/ * 'b'|'t'|'n'|'f'|'r'|'u'|'"'|"'"|'\\' * / | !("\\" | "\'"))* "\'";
	public TerminalRule getSTRINGRule() {
		return gaTerminals.getSTRINGRule();
	} 

	//terminal ML_COMMENT:
	//	"/ *"->"* /";
	public TerminalRule getML_COMMENTRule() {
		return gaTerminals.getML_COMMENTRule();
	} 

	//terminal SL_COMMENT:
	//	"//" !("\n" | "\r")* ("\r"? "\n")?;
	public TerminalRule getSL_COMMENTRule() {
		return gaTerminals.getSL_COMMENTRule();
	} 

	//terminal WS:
	//	(" " | "\t" | "\r" | "\n")+;
	public TerminalRule getWSRule() {
		return gaTerminals.getWSRule();
	} 

	//terminal ANY_OTHER:
	//	.;
	public TerminalRule getANY_OTHERRule() {
		return gaTerminals.getANY_OTHERRule();
	} 
}
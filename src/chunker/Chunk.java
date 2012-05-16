package chunker;

import java.util.*;
import java.util.regex.*;

/**
 * This class is a translation of the Python NP-chunker written by
 * Beata B. Megyesi. It has been slightly rewritten with a reduced set
 * of functionality (atleast from the interface POV). It is still quadratic
 * with regards to the number of words in each sentence, but that worst
 * case is a lot less likely to occur. The runtime has been reduced from
 * about 3h to about 4mins on the Socialstyrelsen and Medicin corpora.
 *
 * Copyright (c) Beata B. Megyesi
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, and/or sublicense copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
 * CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
public class Chunk
{
	private Parser parser = null;
	private TreeBuilder tb = null;

	// If a sentence contains more words than 'maxTokens' it will be
	// split up into blocks of 'maxTokens' tokens. Each block will be
	// treated as a separate sentence
	private static final int maxTokens = 20;

	private enum FunctionNames
	{
		// This is the initial function
		START,

		p_sent_0,
		p_S_base1_20,
		p_S_base2_10,
		p_S_term_30,
		p_S_nonterm_30,
		p_S_nonterm1_30,
		p_S_nonterm2_30,
		p_S_nonterm3_500,
		p_Q_0_20,
		p_Q_1_term_30,
		p_Q_1_nonterm_30,
		p_default_10,
		p_phrase_2a_30,
		p_phrase_2a_40,
		p_phrase_2b_40,
		p_phrase_3a_50,
		p_phrase_3b_50,
		p_phrase_3c_50,
		p_phrase_4_60,
		p_phrase_5_70,
		p_advp_term_90,
		p_det_90,
		p_det_poss_pron_term_90,
		p_det_poss_pron_nonterm_90,
		p_adj_sing_plur_100,
		p_ap_min_sing_plur_110,
		p_ap_min_120,
		p_ap_min_sing_cont_130,
		p_ap1_140,
		p_ap2_150,
		p_ap_max_150,
		p_conj_del_minq_160,
		p_prop_comp_noun_cont_170,
		p_comp_noun_cont_170,
		p_siffer_170,
		p_np1_180,
		p_np_rest_180,
		p_np2_190,
		p_np_sif_190,
		p_np_com_1_200,
		p_np_com_2_200,
		p_np_com_3_200,
		p_np3_210,
		p_np_com_210,
		p_np_prop_210,
		p_nppc_210,
		p_np_comp_210,
		p_pp1_220,
		p_vc_term_220,
		p_pp2_230,
		p_vc_term_list_230,
		p_pp_mellan_240,
		p_pp_konj_240,
		p_vc_term_list_sv_240,
		p_infp_250
	}

	private enum TokenTypes
	{
		// This group below are single token types, which represent one token
		sent_adv,
		here_there,
		prep_mellan,
		det,
		n_gen,
		prop_n_gen,
		adv,
		konj,
		subj,
		prep,
		adj_sing,
		adj_plur,
		adj_sing_plur,
		num,
		pron,
		poss_pron,
		comp_noun,
		prop_comp_noun,
		prop_noun,
		com_noun,
		inf,
		inf_verb,
		part,
		fin_verb,
		sup_verb,
		imp_verb,
		conj_verb,
		del_min,
		del_maj,
		del_paren,
		interj,
		u_o,
		particip,
		konj_verb,

		// This group below are composite token types, which represents
		// atleast one token
		// Only the type needed to create an NP (noun-phrase) is included
		// All but NP are in alphabetic order
		NP,

		ADJPLUR,
		ADJSING,
		ADVP,
		ADVP_Q,
		ADVP_S,
		AP,
		APMIN,
		APMINPLURCONT_L,
		APMINPLURCONT_S,
		APMINPLUR_L,
		APMINPLUR_S,
		APMINSINGCONT_L,
		APMINSINGCONT_S,
		APMINSING_L,
		APMINSING_S,
		APMAX,
		AP_S,
		AP_Q,
		COMNOUN_S,
		COMPNOUNCONT_L,
		COMPNOUNCONT_S,
		DELMIN_Q,
		DET,
		DET_Q,
		DETPOSSPRON,
		DETPOSSPRON_Q,
		HERETHERE_Q,
		KONJDELMINQ,
		KONJ_Q,
		NPCOM_L,
		NPCOM1_L,
		NPCOM2_L,
		NPCOM3_L,
		NPCOMP_L,
		NGEN_Q,
		NPPC_L,
		NPPROP_L,
		NPREST_L,
		NPSIF_L,
		NUM_Q,
		NUM_S,
		NUMP,
		PROPCOMPNOUNCONT_L,
		PROPCOMPNOUNCONT_S,
		PROPNOUN_S,

		// The types below are used for special cases when building the ASTs
		NONE,
		EOF,
		START,

		// The types below are used to build ASTs but not needed for NPs
		COMPNOUN_S,
		INFP,
		INFVERB_S,
		NP_S,
		PART_Q,
		PHRASE,
		PHRASE_S,
		PP,
		PP_Q,
		PP_S,
		SADVP_S,
		SENT,
		VC
	}

	public Chunk()
	{
		parser = new Parser();
		tb = new TreeBuilder();
	}

	public String[] parse_input(String[] lines)
	{
		List<String> nps = new ArrayList<String>();

		for(String line : lines)
		{
			if(line.trim().equals(""))
				continue;

			// Cut the line up in segment of max 100 tokens each
			// This is due to the chunking being quadratic wrt the number of
			// words in a sentence
			String[] formattedLines = formatLine(line);

			for(String fLine : formattedLines)
			{
				List<Token> tokens = scan(fLine);

				if(tokens.size() < 1)
					continue;

				AST parseTree = null;

				try
				{
					parseTree = parse(tokens);
				}
				catch(Exception e)
				{
//					System.out.println(e.getMessage());
					continue;
				}

				nps.addAll(extractNPs(parseTree));
			}
		}

		String[] array = new String[nps.size()];
		return nps.toArray(array);
	}

	private String[] formatLine(String line)
	{
		String[] tokens = line.split(" ");

		if(tokens.length > maxTokens)
		{
			ArrayList<String> lines = new ArrayList<String>();

			// Split the line up in segments of max 100 tokens
			int start = 0, end = 0, count = 0;
			for(int i = 0; i < tokens.length; i++)
			{
				end += (count == 0 ? 0 : 1) + tokens[i].length();
				count++;

				if(count >= maxTokens)
				{
					lines.add(line.substring(start, end));

					if(end+1 <= line.length())
						end++;

					start = end;
					count = 0;
				}
			}

			if(end > start)
				lines.add(line.substring(start, end));

			String[] array = new String[lines.size()];
			return lines.toArray(array);
		}
		else
			return new String[] {line};
	}

	private List<String> extractNPs(AST tree)
	{
		List<String> nps = new ArrayList<String>();

		for(Object kid : tree.kids)
			nps.addAll(extractNPs((AST)kid));

		if(((Token)tree.type).type.equals(TokenTypes.NP))
			nps.add(extractNP(tree.kids));

		return nps;
	}

	private String extractNP(List<AST> kids)
	{
		String np = "";

		int i = 0;
		for(AST kid : kids)
		{
			np += (i++ == 0 ? "" : " ");

			if(((Token)kid.type).word != null)
				np += ((Token)kid.type).word;
			else
				np += extractNP(kid.kids);
		}

		return np;
	}

	private AST parse(List<Token> tokens) throws Exception
	{
		tb.reset();
		AST pt = tb.parse(tokens);

		return pt;
	}

	private List<Token> scan(String line)
	{
		return parser.tokenize(line);
	}

	private class Token
	{
		private TokenTypes type = null;
		private String word = null;
		private String tag = null;

		public Token(TokenTypes type)
		{
			this.type = type;
		}

		public Token(TokenTypes type, String word, String tag)
		{
			this.type = type;
			this.word = word;
			this.tag = tag;
		}

		public int compareTo(Token t)
		{
			return type.compareTo(t.type);
		}

		@Override
		public String toString()
		{
			if(word != null && tag != null)
				return type + " = " + word + "/" + tag;
			else
				return type.toString();
		}
	}

	private class Parser
	{
		// Patterns that checks for the word aswell as tag
		private Map<Pattern,TokenTypes> wordPatterns = null;
		// Patterns that only checks for the tag
		private Map<Pattern,TokenTypes> miscPatterns = null;

		public Parser()
		{
			wordPatterns = new HashMap<Pattern,TokenTypes>();
			miscPatterns = new HashMap<Pattern,TokenTypes>();

			addPatterns();
		}

		public List<Token> tokenize(String s)
		{
			List<Token> tokens = new ArrayList<Token>();

			// Split the string into a list of 'word/tag'
			String[] wordsTags = s.split(" ");

			// Create a token object for each word n tag
			for(String wt : wordsTags)
			{
				// Split up into word and tag
				int index = wt.lastIndexOf('/');
				String word = null;

				try
				{
					word = wt.substring(0, index);
				}
				catch(Exception e)
				{
					System.out.print(wt);
				}

				String tag = wt.substring(index+1, wt.length());

				boolean match = false;

				// First check against the wordpatterns

				for(Pattern p : wordPatterns.keySet())
				{
					if(p.matcher(wt).matches())
					{
						tokens.add(new Token(wordPatterns.get(p), word, tag));
						match = true;
						break;
					}
				}

				if(match)
					continue;

				// If no match was found, search through the rest of the patterns
				for(Pattern p : miscPatterns.keySet())
				{
					if(p.matcher(tag).matches())
					{
						tokens.add(new Token(miscPatterns.get(p), word, tag));
						break;
					}
				}

				// If we get to this point the string is assumed to be a
				// sequence of white space chars, do nothing
			}

			return tokens;
		}

		private void addPatterns()
		{
			// There are two types of patterns, one that checks for words
			// (the first three) and one that doesn't (the rest)

			// Obs! If there is no pattern match at all, it is assumed to be
			//      one or more white spaces

			wordPatterns.put(Pattern.compile("[aA]ldrig/R...|[aA]lltid/R...|" +
					"[aA]lltså/R...|[bB]ara/R...|[dD]it/R...|[dD]ock/R...|" +
					"[dD]ärför/R...|[fF]aktiskt/R...|[gG]enast/R...|" +
					"[gG]ivetvis/R...|[hH]eller/R...|[hH]it/R...|" +
					"[hH]ittills/R...|[hH]ur/R...|[iI]från/R...|[iI]nte/R...|" +
					"[jJ]u/R...|[kK]anske/R...|[nN]aturligtvis/R...|" +
					"[nN]u/R...|[nN]og/R...|[nN]ämligen/R...|[nN]är/R...|" +
					"[nN]ödvändigtvis/R...|[oO]ckså/R...|[oO]fta/R...|" +
					"[pP]lötsligt/R...|[sS]äkert/R...|[uU]pp/R...|" +
					"[vV]ad/R...|[vV]arför/R...|[vV]isserligen/R...|" +
					"[äÄ]ndå/R...|[äÄ]ven/R..."), TokenTypes.sent_adv);
			wordPatterns.put(Pattern.compile("[HhDd]är/RG0S"), TokenTypes.here_there);
			wordPatterns.put(Pattern.compile("[Mm]ellan/SPS"), TokenTypes.prep_mellan);

			miscPatterns.put(Pattern.compile("D......"), TokenTypes.det);
			miscPatterns.put(Pattern.compile("NC..G@.S"), TokenTypes.n_gen);
			miscPatterns.put(Pattern.compile("NP00G@0S"), TokenTypes.prop_n_gen);
			miscPatterns.put(Pattern.compile("R..."), TokenTypes.adv);
			miscPatterns.put(Pattern.compile("CC."), TokenTypes.konj);
			miscPatterns.put(Pattern.compile("CSS"), TokenTypes.subj);
			miscPatterns.put(Pattern.compile("SP."), TokenTypes.prep);
			miscPatterns.put(Pattern.compile("A...S..."), TokenTypes.adj_sing);
			miscPatterns.put(Pattern.compile("A...P..."), TokenTypes.adj_plur);
			miscPatterns.put(Pattern.compile("A...0..."), TokenTypes.adj_sing_plur);
			miscPatterns.put(Pattern.compile("M......"), TokenTypes.num);
			miscPatterns.put(Pattern.compile("P[FEHI]......"), TokenTypes.pron);
			miscPatterns.put(Pattern.compile("PS......"), TokenTypes.poss_pron);
			miscPatterns.put(Pattern.compile("(NC...@.C|V@000C)"), TokenTypes.comp_noun);
			miscPatterns.put(Pattern.compile("NP000@0C"), TokenTypes.prop_comp_noun);
			miscPatterns.put(Pattern.compile("NP00N@.S"), TokenTypes.prop_noun);
			miscPatterns.put(Pattern.compile("NC..[N0]@.[SA]"), TokenTypes.com_noun);
			miscPatterns.put(Pattern.compile("CIS"), TokenTypes.inf);
			miscPatterns.put(Pattern.compile("V@N..."), TokenTypes.inf_verb);
			miscPatterns.put(Pattern.compile("QS"), TokenTypes.part);
			miscPatterns.put(Pattern.compile("V@I[IP].."), TokenTypes.fin_verb);
			miscPatterns.put(Pattern.compile("V@IU.."), TokenTypes.sup_verb);
			miscPatterns.put(Pattern.compile("(V@M...|V@000A)"), TokenTypes.imp_verb);
			miscPatterns.put(Pattern.compile("V@S..."), TokenTypes.conj_verb);
			miscPatterns.put(Pattern.compile("FI"), TokenTypes.del_min);
			miscPatterns.put(Pattern.compile("FE"), TokenTypes.del_maj);
			miscPatterns.put(Pattern.compile("FP"), TokenTypes.del_paren);
			miscPatterns.put(Pattern.compile("I"), TokenTypes.interj);
			miscPatterns.put(Pattern.compile("XF"), TokenTypes.u_o);
		}
	}

	/**
	 * This class corresponds to the GenericParser in the Python code
	 */
	private class TreeBuilder
	{
		private Rules rules = null;
		// It's actually rule to function name
		private Hashtable<Rule,FunctionNames> rule2func = null;

		// This contains a mapping of the LHS of each rule and the
		// first symbol of every possible RHS linked to the LHS
		private Hashtable<TokenTypes,HashSet<TokenTypes>> first = null;

		private Rule startRule = null;
		private Rule sentRule = null;

		public TreeBuilder()
		{
			rules = new Rules();
			rule2func = new Hashtable<Rule,FunctionNames>();

			collectRules();

			augment();

			makeFIRST();
		}

		private void augment()
		{
			startRule = new Rule(TokenTypes.START, new TokenTypes[]
				{TokenTypes.SENT, TokenTypes.EOF});

			rule2func.put(startRule, FunctionNames.START);

			rules.put(TokenTypes.START, new Rule[] {startRule});
		}

		public AST parse(List<Token> tokens) throws Exception
		{
			Tree tree = new Tree();

			tokens.add(new Token(TokenTypes.EOF, null, null));

			List<State> states = new ArrayList<State>();
			State state = new State();
			state.append(new StateItem(startRule, 0, 0));
			states.add(state);

			String prevError = null;
			boolean done = false;
			int i = 0;

			while(!done)
			{
				boolean breaked = false;

				for( ; i < tokens.size(); i++)
				{
					states.add(new State());

					if(states.get(i).size() == 0)
					{
						breaked = true;
						break;
					}

					buildState(tokens.get(i), states, i, tree);
					breaked = false;
				}

				if(!breaked)
					i--;

				boolean error = false;
				if(i < tokens.size()-1)
					error = true;
				else
				{
					State tmpState = states.get(i+1);
					State cmpState = new State();
					cmpState.append(new StateItem(startRule, 2, 0));

					error = !tmpState.equals(cmpState);
				}

				if(error)
				{
					// If we get the same error twice we need to rollback even further
					// Alternatively start over by a recursive call :)
					if(prevError != null && tokens.get(i-1).word.equals(prevError))
					{
//						throw new Exception("Syntax error at or near '" + tokens.get(i-1) + "' token");

						// We've gotten the same error twice, restart with a
						// recursive call
						tokens.remove(tokens.size()-1);
						updateSentRule();
						return parse(tokens);
					}

//					tokens.remove(tokens.size()-1);
//					System.err.println("Syntax error at or near '" + tokens.get(i-1) + "' token");
					prevError = tokens.get(i-1).word;
//					throw new Exception("Syntax error");

					updateSentRule();
//					return parse(tokens);

					// Roll back and try again
					if(states.size() > 0)
						states.remove(states.size()-1);
					if(states.size() > 0)
						states.remove(states.size()-1);
					if(i > 0)
						i--;
				}
				else
				{
					prevError = null;
					done = true;
				}
			}

			Tuple<StateItem,Integer> tuple =
					new Tuple<StateItem,Integer>(new StateItem(startRule, 2, 0), i+1);
			AST rv = buildTree(tokens, tree, tuple);

			tokens.remove(tokens.size()-1);

			return rv;
		}

		private void updateSentRule()
		{
			TokenTypes[] rhs = new TokenTypes[sentRule.rhs.length+1];

			for(int i = 0; i < sentRule.rhs.length; i++)
				rhs[i] = sentRule.rhs[i];

			rhs[rhs.length-1] = TokenTypes.PHRASE;

			sentRule.rhs = rhs;
		}

		public void reset()
		{
			resetSentRule();
		}

		private void resetSentRule()
		{
			sentRule.rhs = new TokenTypes[] {TokenTypes.PHRASE};
		}

		private AST buildTree(List<Token> tokens, Tree tree,
				Tuple<StateItem,Integer> root)
		{
			List stack = new ArrayList();
			buildTree_r(stack, tokens, tokens.size()-1, tree, root);
			return (AST)stack.get(0);
		}

		private int buildTree_r(List stack, List<Token> tokens,
				int tokpos, Tree tree, Tuple<StateItem,Integer> root)
		{
			Rule rule = root.fst().rule;
			int pos = root.fst().position;
			int parent = root.fst().parent;
			int state = root.snd();

//			System.out.println(rule);

			while(pos > 0)
			{
				Tuple<StateItem,Integer> want =
						new Tuple<StateItem, Integer>(new StateItem(rule, pos, parent), state);

				if(!tree.hasKey(want))
				{
					pos = pos - 1;
					state = state -1;
					stack.add(0, tokens.get(tokpos));
					tokpos = tokpos - 1;
				}
				else
				{
					List<Tuple<StateItem,Integer>> children = tree.get(want);
					Tuple<StateItem,Integer> child = null;

					if(children.size() > 1)
						child = ambiguity(children);
					else
						child = children.get(0);

					tokpos = buildTree_r(stack, tokens, tokpos, tree, child);

					pos = pos - 1;
					state = child.fst().getParent();
				}
			}

			TokenTypes[] rhs = rule.rhs;

			int len = rhs.length;
			List subList = new ArrayList();

			for(int i = 0; i < len; i++)
				subList.add(i, stack.remove(0));

			// Call function here
			// result = self.rule2func[rule](stack[:len(rhs)])
			FunctionNames funcName = rule2func.get(rule);

			Object result = callFunc(funcName, subList);

			stack.add(0, result);

			return tokpos;
		}

		private Tuple<StateItem,Integer> ambiguity(List<Tuple<StateItem,Integer>> children)
		{
			List<Tuple<Integer,FunctionNames>> sortlist =
						new ArrayList<Tuple<Integer,FunctionNames>>();
			Hashtable<FunctionNames,Integer> name2index =
					new Hashtable<FunctionNames,Integer>();

			for(int i = 0; i < children.size(); i++)
			{
				Rule rule = children.get(i).fst().getRule();

				FunctionNames name = rule2func.get(rule);
				sortlist.add(new Tuple<Integer, FunctionNames>(i, name));
				name2index.put(name, i);
			}

			sort(sortlist);
			List<FunctionNames> list = new ArrayList<FunctionNames>();
			for(Tuple<Integer,FunctionNames> t : sortlist)
				list.add(t.snd());

			return children.get(name2index.get(resolve(list)));
		}

		private void sort(List<Tuple<Integer,FunctionNames>> list)
		{
			// TODO: Should this be ascending or descending order
			//       Atm it is descending
			for(int i = 0; i < list.size(); i++)
			{
				for(int j = 0; j < list.size()-1; j++)
				{
					if(list.get(j).fst() < list.get(j+1).fst())
					{
						Tuple<Integer,FunctionNames> tmp = list.remove(j);
						list.add(j+1, tmp);
					}
				}
			}
		}

		private FunctionNames resolve(List<FunctionNames> list)
		{
			int maxPrio = -1;
			FunctionNames maxString = null;

			for(FunctionNames name : list)
			{
				String[] tmp = name.toString().split("_");
				int prio = Integer.parseInt(tmp[tmp.length-1]);

				if(prio > maxPrio)
				{
					maxPrio = prio;
					maxString = name;
				}
			}

			if(maxString != null)
				return maxString;
			else
				return list.get(0);
		}

		private void buildState(Token token, List<State> states, int i, Tree tree)
		{
			Hashtable<TokenTypes,Tuple<StateItem,Integer>> needsCompletion =
					new Hashtable<TokenTypes,Tuple<StateItem,Integer>>();
			State state = states.get(i);
			HashSet<TokenTypes> predicted = new HashSet<TokenTypes>();

			int nr = 100000;
			boolean ns = false;

			if(nr == i)
				System.out.println("In buildState");

			for(int j = 0; j < state.items().size(); j++)
			{
				StateItem item = state.items().get(j);

				Rule rule = item.getRule();
				int pos = item.getPosition();
				int parent = item.getParent();

				TokenTypes lhs = rule.lhs;
				TokenTypes[] rhs = rule.rhs;

//				printRule(rule);

				/**
				 * A -> a . (completer)
				 */
				if(pos == rhs.length)
				{
					if(nr == i)
						System.out.println("STAGE 0");

					if(rhs.length == 0)
						needsCompletion.put(lhs, new Tuple<StateItem,Integer>(item, i));

					List<StateItem> items = states.get(parent).items();
					for(int k = 0; k < items.size(); k++)
					{
						StateItem pitem = items.get(k);

						if(pitem.equals(item))
						{
							if(nr == i)
								System.out.println("STAGE 1");
							break;
						}

						Rule prule = pitem.getRule();
						int ppos = pitem.getPosition();
						int pparent = pitem.getParent();

						TokenTypes[] prhs = prule.rhs;

						if(ppos < prhs.length && prhs[ppos] == lhs)
						{
							if(nr == i)
							{
								System.out.println(ppos);
								System.out.println(prhs[ppos]);
								System.out.println("STAGE 2");
							}

							StateItem newState = new StateItem(prule, ppos+1, pparent);

							if(!state.contains(newState))
							{
								state.append(newState);
								tree.addNewKey(newState, i);
							}

							tree.append(newState, i, item, i);
						}
					}

					continue;
				}

				TokenTypes nextSym = rhs[pos];

				if(ns)
					System.out.println("nextSym: " + nextSym);

				/**
				 * A -> a . B (predictor)
				 */
				if(rules.hasKey(nextSym))
				{
					if(nr == i)
						System.out.println("STAGE 3");

					if(needsCompletion.containsKey(nextSym))
					{
						if(nr == i)
							System.out.println("STAGE 4");

						StateItem newState = new StateItem(rule, pos+1, parent);
						Tuple<StateItem,Integer> olditem_i = needsCompletion.get(nextSym);

						if(!state.contains(newState))
						{
							state.append(newState);
							tree.addNewKey(newState, i);
						}

						tree.append(newState, i, olditem_i.fst(), olditem_i.snd());
					}

					if(predicted.contains(nextSym))
					{
						if(nr == i)
							System.out.println("STAGE 5");
						continue;
					}

					predicted.add(nextSym);

					TokenTypes ttype = token.type;

					// TODO: Make sure this is correct
//					if(!ttype.equals(_EOF) /*&& !ttype.equals("None")*/)
					if(ttype != TokenTypes.EOF)
					{
						if(nr == i)
							System.out.println("STAGE 6");

						for(Rule prule : rules.get(nextSym))
						{
							if(nr == i)
								System.out.println("STAGE 7");

							StateItem newState = new StateItem(prule, 0, i);

							TokenTypes[] prhs = prule.rhs;

							if(prhs.length == 0)
							{
								if(nr == i)
									System.out.println("STAGE 8");

								state.append(newState);
								continue;
							}

							TokenTypes prhs0 = prhs[0];
							if(!rules.hasKey(prhs0))
							{
								if(nr == i)
									System.out.println("STAGE 9");

								if(prhs0.equals(ttype))
									state.append(newState);

								continue;
							}

							HashSet<TokenTypes> fst = first.get(prhs0);
							if(!fst.contains(TokenTypes.NONE) && !fst.contains(ttype))
								continue;

							if(nr == i)
								System.out.println("STAGE 10");
							state.append(newState);
						}

						continue;
					}

					for(Rule prule : rules.get(nextSym))
					{
						TokenTypes[] prhs = prule.rhs;

						if(prhs.length > 0 && !rules.hasKey(prhs[0]) && ttype != prhs[0])
							continue;

						if(nr == i)
							System.out.println("STAGE 11");

						state.append(new StateItem(prule, 0, i));
					}
				}
				else if(token.type.equals(nextSym))
				{
					if(nr == i)
						System.out.println("STAGE 12");
					states.get(i+1).append(new StateItem(rule, pos+1, parent));
				}
			}

			if(nr == i)
				System.out.println("");
		}

		private void makeFIRST()
		{
			UnionList union = new UnionList();
			first = new Hashtable<TokenTypes,HashSet<TokenTypes>>();

			for(Rule[] rulelist : rules.values())
			{
				for(Rule r : rulelist)
				{
					TokenTypes lhs = r.lhs;
					TokenTypes[] rhs = r.rhs;

					if(!first.containsKey(lhs))
						first.put(lhs, new HashSet<TokenTypes>());

					if(rhs.length == 0)
					{
						first.get(lhs).add(TokenTypes.NONE);
						continue;
					}

					TokenTypes sym = rhs[0];
					if(!rules.hasKey(sym))
						first.get(lhs).add(sym);
					else
						union.add(sym, lhs);
				}
			}

			boolean changes = true;
			while(changes)
			{
				changes = false;

				for(Tuple<TokenTypes,TokenTypes> t : union.get())
				{
					TokenTypes src = t.fst(), dest = t.snd();

					int destlen = first.get(dest).size();
					first.get(dest).addAll(first.get(src));

					if(first.get(dest).size() != destlen)
						changes = true;
				}
			}
		}

		private Object callFunc(FunctionNames name, List args)
		{
			List<Object> kids = new ArrayList<Object>();

			switch(name)
			{
				// This is the functionality from the PhraseParser_1 that is
				// compiled on the fly in the Python code
				// The code above is from the basic GenericParser
				case p_sent_0:
					// return AST(type=Token(type="sent"), kids=args)
					return new AST(new Token(TokenTypes.SENT), args);
				case p_S_base1_20:
				case p_S_base2_10:
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_S_term_30:
					AST tmp = new AST(args.get(0));
					kids.add(tmp);
					kids.addAll(((AST)args.get(1)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_S_nonterm_30:
				case p_S_nonterm1_30:
				case p_S_nonterm2_30:
					kids.add(args.get(0));
					kids.addAll(((AST)args.get(1)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_Q_0_20:
					return new AST(new Token(TokenTypes.NONE));
				case p_Q_1_term_30:
					kids.add(new AST(args.get(0)));
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_Q_1_nonterm_30:
					kids.add(args.get(0));
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_default_10:
				case p_det_90:
				case p_det_poss_pron_term_90:
				case p_adj_sing_plur_100:
					return new AST(args.get(0));
				case p_phrase_2a_30:
				case p_phrase_2a_40:
				case p_phrase_2b_40:
				case p_phrase_3a_50:
				case p_phrase_3b_50:
				case p_phrase_3c_50:
				case p_phrase_4_60:
				case p_phrase_5_70:
				case p_det_poss_pron_nonterm_90:
				case p_ap1_140:
				case p_ap2_150:
				case p_conj_del_minq_160:
				case p_np_com_210:
					return args.get(0);
				case p_advp_term_90:
					kids.add(new AST(args.get(0)));
					return new AST(new Token(TokenTypes.ADVP), kids);
				case p_ap_min_sing_plur_110:
					kids.addAll(((AST)args.get(0)).kids);
					kids.add(args.get(1));
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_ap_min_120:
					kids.addAll(((AST)args.get(0)).kids);
					return new AST(new Token(TokenTypes.APMIN), kids);
				case p_ap_min_sing_cont_130:
					kids.add(new AST(args.get(0)));
					kids.addAll(((AST)args.get(1)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_ap_max_150:
					kids.addAll(((AST)args.get(0)).kids);
					kids.addAll(((AST)args.get(1)).kids);
					kids.addAll(((AST)args.get(2)).kids);
					kids.addAll(((AST)args.get(3)).kids);
					return new AST(new Token(TokenTypes.APMAX), kids);
				case p_prop_comp_noun_cont_170:
					kids.addAll(((AST)args.get(0)).kids);
					kids.addAll(((AST)args.get(1)).kids);
					kids.add(new AST(args.get(2)));
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_comp_noun_cont_170:
					kids.addAll(((AST)args.get(0)).kids);
					kids.add(new AST(args.get(1)));
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_siffer_170:
					// return AST(type=Token(type=TokenTypes.NUMP), kids=args[0]._kids + [AST(type=args[1])] + args[2]._kids)
					kids.addAll(((AST)args.get(0)).kids);
					kids.add(new AST(args.get(1)));
					kids.addAll(((AST)args.get(2)).kids);
					return new AST(new Token(TokenTypes.NUMP), kids);
				case p_np1_180:
					// return AST(type=Token(type=TokenTypes.NP), kids=args[0]._kids)
					kids.addAll(((AST)args.get(0)).kids);
					return new AST(new Token(TokenTypes.NP), kids);
				case p_np_rest_180:
					// return AST(type=Token(type="list"), kids=[args[0]] + args[1]._kids)
					kids.add(args.get(0));
					kids.addAll(((AST)args.get(1)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_np2_190:
					// return AST(type=Token(type=TokenTypes.NP), kids=args[0]._kids)
					kids.addAll(((AST)args.get(0)).kids);
					return new AST(new Token(TokenTypes.NP), kids);
				case p_np_sif_190:
					// return AST(type=Token(type="list"), kids=args[0]._kids + args[1]._kids + [args[2]])
					kids.addAll(((AST)args.get(0)).kids);
					kids.addAll(((AST)args.get(1)).kids);
					kids.add(args.get(2));
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_np_com_1_200:
					// return AST(type=Token(type="list"), kids=[AST(type=args[0])] + args[1]._kids)
					kids.add(new AST(args.get(0)));
					kids.addAll(((AST)args.get(1)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_np_com_2_200:
					// return AST(type=Token(type="list"), kids=args[0]._kids +
					//		[AST(type=args[1])] + args[2]._kids + args[3]._kids +
					//		args[4]._kids + args[5]._kids + args[6]._kids +
					//		[AST(type=args[7])] + args[8]._kids)
					kids.addAll(((AST)args.get(0)).kids);
					kids.add(new AST(args.get(1)));
					kids.addAll(((AST)args.get(2)).kids);
					kids.addAll(((AST)args.get(3)).kids);
					kids.addAll(((AST)args.get(4)).kids);
					kids.addAll(((AST)args.get(5)).kids);
					kids.addAll(((AST)args.get(6)).kids);
					kids.add(new AST(args.get(7)));
					kids.addAll(((AST)args.get(8)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_np_com_3_200:
					// return AST(type=Token(type="list"), kids=args[0]._kids +
					//		args[1]._kids + args[2]._kids + args[3]._kids +
					//		args[4]._kids + args[5]._kids + args[6]._kids +
					//		[AST(type=args[7])] + args[8]._kids)
					kids.addAll(((AST)args.get(0)).kids);
					kids.addAll(((AST)args.get(1)).kids);
					kids.addAll(((AST)args.get(2)).kids);
					kids.addAll(((AST)args.get(3)).kids);
					kids.addAll(((AST)args.get(4)).kids);
					kids.addAll(((AST)args.get(5)).kids);
					kids.addAll(((AST)args.get(6)).kids);
					kids.add(new AST(args.get(7)));
					kids.addAll(((AST)args.get(8)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_np3_210:
					// return AST(type=Token(type=TokenTypes.NP), kids=args[0]._kids)
					kids.addAll(((AST)args.get(0)).kids);
					return new AST(new Token(TokenTypes.NP), kids);
				case p_np_prop_210:
					// return AST(type=Token(type="list"), kids=args[0]._kids +
					//		args[1]._kids + args[2]._kids + [AST(type=args[3])] +
					//		args[4]._kids)
					kids.addAll(((AST)args.get(0)).kids);
					kids.addAll(((AST)args.get(1)).kids);
					kids.addAll(((AST)args.get(2)).kids);
					kids.add(new AST(args.get(3)));
					kids.addAll(((AST)args.get(4)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_nppc_210:
					// return AST(type=Token(type="list"), kids=[AST(type=args[0])] +
					//		args[1]._kids + args[2]._kids + [AST(type=args[3])] +
					//		args[4]._kids)
					kids.add(new AST(args.get(0)));
					kids.addAll(((AST)args.get(1)).kids);
					kids.addAll(((AST)args.get(2)).kids);
					kids.add(new AST(args.get(3)));
					kids.addAll(((AST)args.get(4)).kids);
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_np_comp_210:
					// return AST(type=Token(type="list"), kids=args[0]._kids +
					//		args[1]._kids + [AST(type=args[2])] + args[3]._kids +
					//		[AST(type=args[4])] + args[5]._kids + args[6]._kids +
					//		args[7]._kids + [AST(type=args[8])] + args[9]._kids)
					kids.add(new AST(args.get(0)));
					kids.add(new AST(args.get(1)));
					kids.addAll(((AST)args.get(2)).kids);
					kids.add(new AST(args.get(3)));
					kids.addAll(((AST)args.get(4)).kids);
					kids.add(new AST(args.get(5)));
					kids.add(new AST(args.get(6)));
					kids.add(new AST(args.get(7)));
					kids.addAll(((AST)args.get(8)).kids);
					kids.add(new AST(args.get(9)));
					return new AST(new Token(TokenTypes.NONE), kids);
				case p_pp1_220:
					// return AST(type=Token(type=TokenTypes.PP), kids=[AST(type=args[0]), args[1]])
					kids.add(new AST(args.get(0)));
					kids.add(args.get(1));
					return new AST(new Token(TokenTypes.PP), kids);
				case p_vc_term_220:
					// return AST(type=Token(type=TokenTypes.VC) , kids=[AST(type=args[0])])
					kids.add(new AST(args.get(0)));
					return new AST(new Token(TokenTypes.VC), kids);
				case p_pp2_230:
					// return AST(type=Token(type=TokenTypes.PP), kids=[AST(type=args[0]), args[1]])
					kids.add(new AST(args.get(0)));
					kids.add(args.get(1));
					return new AST(new Token(TokenTypes.PP), kids);
				case p_vc_term_list_230:
					// return AST(type=Token(type=TokenTypes.VC) , kids=[AST(type=args[0])] + args[1]._kids)
					kids.add(new AST(args.get(0)));
					kids.addAll(((AST)args.get(1)).kids);
					return new AST(new Token(TokenTypes.VC), kids);
				case p_pp_mellan_240:
					// return AST(type=Token(type=TokenTypes.PP), kids=[AST(type=args[0]),
					//		args[1], AST(type=args[2]), args[3]])
					kids.add(new AST(args.get(0)));
					kids.add(args.get(1));
					kids.add(new AST(args.get(2)));
					kids.add(args.get(3));
					return new AST(new Token(TokenTypes.PP), kids);
				case p_pp_konj_240:
					// return AST(type=Token(type=TokenTypes.PP), kids=[AST(type=args[0]),
					//		AST(type=args[1]), AST(type=args[2]), args[3]])
					kids.add(new AST(args.get(0)));
					kids.add(new AST(args.get(1)));
					kids.add(new AST(args.get(2)));
					kids.add(args.get(3));
					return new AST(new Token(TokenTypes.PP), kids);
				case p_vc_term_list_sv_240:
					// return AST(type=Token(type=TokenTypes.VC), kids=[AST(type=args[0])] +
					//		args[1]._kids + [AST(type=args[2])])
					kids.add(new AST(args.get(0)));
					kids.addAll(((AST)args.get(1)).kids);
					kids.add(new AST(args.get(2)));
					return new AST(new Token(TokenTypes.VC), kids);
				case p_infp_250:
					// return AST(type=Token(type=TokenTypes.INFP), kids=[AST(type=args[0])] +
					//		args[1]._kids + [AST(type=args[2])] + args[3]._kids)
					kids.add(new AST(args.get(0)));
					kids.addAll(((AST)args.get(1)).kids);
					kids.add(new AST(args.get(2)));
					kids.addAll(((AST)args.get(3)).kids);
					return new AST(new Token(TokenTypes.INFP), kids);

				// This is the function represented as a lambda expression
				// in the augment() function
				case START:
					return args.get(0);
			}

			// No function match
			return null;
		}

		private void collectRules()
		{
			Rule r1, r2, r3, r4, r5, r6;

			r1 = new Rule(TokenTypes.ADJPLUR, new TokenTypes[] {TokenTypes.adj_plur});
			rule2func.put(r1, FunctionNames.p_adj_sing_plur_100);
			r2 = new Rule(TokenTypes.ADJPLUR, new TokenTypes[] {TokenTypes.adj_sing_plur});
			rule2func.put(r2, FunctionNames.p_adj_sing_plur_100);
			rules.put(TokenTypes.ADJPLUR, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.ADJSING, new TokenTypes[] {TokenTypes.adj_sing});
			rule2func.put(r1, FunctionNames.p_adj_sing_plur_100);
			r2 = new Rule(TokenTypes.ADJSING, new TokenTypes[] {TokenTypes.adj_sing_plur});
			rule2func.put(r2, FunctionNames.p_adj_sing_plur_100);
			rules.put(TokenTypes.ADJSING, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.ADVP, new TokenTypes[] {TokenTypes.adv});
			rule2func.put(r1, FunctionNames.p_advp_term_90);
			r2 = new Rule(TokenTypes.ADVP, new TokenTypes[] {TokenTypes.here_there});
			rule2func.put(r2, FunctionNames.p_advp_term_90);
			rules.put(TokenTypes.ADVP, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.ADVP_Q, new TokenTypes[] {TokenTypes.ADVP});
			rule2func.put(r1, FunctionNames.p_Q_1_nonterm_30);
			r2 = new Rule(TokenTypes.ADVP_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.ADVP_Q, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.ADVP_S, new TokenTypes[]
				{TokenTypes.ADVP, TokenTypes.ADVP_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm_30);
			r2 = new Rule(TokenTypes.ADVP_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.ADVP_S, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.AP, new TokenTypes[] {TokenTypes.APMIN});
			rule2func.put(r1, FunctionNames.p_ap1_140);
			r2 = new Rule(TokenTypes.AP, new TokenTypes[] {TokenTypes.APMAX});
			rule2func.put(r2, FunctionNames.p_ap2_150);
			rules.put(TokenTypes.AP, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.AP_Q, new TokenTypes[] {TokenTypes.AP});
			rule2func.put(r1, FunctionNames.p_Q_1_nonterm_30);
			r2 = new Rule(TokenTypes.AP_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.AP_Q, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.AP_S, new TokenTypes[]
				{TokenTypes.AP, TokenTypes.AP_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm_30);
			r2 = new Rule(TokenTypes.AP_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.AP_S, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.APMAX, new TokenTypes[]
				{TokenTypes.APMINPLUR_L, TokenTypes.APMINPLUR_S,
				 TokenTypes.APMINPLURCONT_L, TokenTypes.APMINPLURCONT_S});
			rule2func.put(r1, FunctionNames.p_ap_max_150);
			r2 = new Rule(TokenTypes.APMAX, new TokenTypes[]
				{TokenTypes.APMINSING_L, TokenTypes.APMINSING_S,
				 TokenTypes.APMINSINGCONT_L, TokenTypes.APMINSINGCONT_S});
			rule2func.put(r2, FunctionNames.p_ap_max_150);
			rules.put(TokenTypes.APMAX, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.APMIN, new TokenTypes[] {TokenTypes.APMINSING_L});
			rule2func.put(r1, FunctionNames.p_ap_min_120);
			r2 = new Rule(TokenTypes.APMIN, new TokenTypes[] {TokenTypes.APMINPLUR_L});
			rule2func.put(r2, FunctionNames.p_ap_min_120);
			rules.put(TokenTypes.APMIN, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.APMINPLURCONT_L, new TokenTypes[]
				{TokenTypes.del_min, TokenTypes.APMINPLUR_L});
			rule2func.put(r1, FunctionNames.p_ap_min_sing_cont_130);
			r2 = new Rule(TokenTypes.APMINPLURCONT_L, new TokenTypes[]
				{TokenTypes.konj, TokenTypes.APMINPLUR_L});
			rule2func.put(r2, FunctionNames.p_ap_min_sing_cont_130);
			rules.put(TokenTypes.APMINPLURCONT_L, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.APMINPLURCONT_S, new TokenTypes[]
				{TokenTypes.APMINPLURCONT_L, TokenTypes.APMINPLURCONT_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm_30);
			rules.put(TokenTypes.APMINPLURCONT_S, new Rule[] {r1});

			r1 = new Rule(TokenTypes.APMINPLUR_L, new TokenTypes[]
				{TokenTypes.ADVP_S, TokenTypes.ADJPLUR});
			rule2func.put(r1, FunctionNames.p_ap_min_sing_plur_110);
			rules.put(TokenTypes.APMINPLUR_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.APMINPLUR_S, new TokenTypes[]
				{TokenTypes.APMINPLUR_L, TokenTypes.APMINPLUR_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm2_30);
			r2 = new Rule(TokenTypes.APMINPLUR_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.APMINPLUR_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.APMINSING_L, new TokenTypes[]
				{TokenTypes.ADVP_S, TokenTypes.ADJSING});
			rule2func.put(r1, FunctionNames.p_ap_min_sing_plur_110);
			rules.put(TokenTypes.APMINSING_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.APMINSING_S, new TokenTypes[]
				{TokenTypes.APMINSING_L, TokenTypes.APMINSING_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm1_30);
			r2 = new Rule(TokenTypes.APMINSING_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.APMINSING_S, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.APMINSINGCONT_L, new TokenTypes[]
				{TokenTypes.del_min, TokenTypes.APMINSING_L});
			rule2func.put(r1, FunctionNames.p_ap_min_sing_cont_130);
			r2 = new Rule(TokenTypes.APMINSINGCONT_L, new TokenTypes[]
				{TokenTypes.konj, TokenTypes.APMINSING_L});
			rule2func.put(r2, FunctionNames.p_ap_min_sing_cont_130);
			rules.put(TokenTypes.APMINSINGCONT_L, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.APMINSINGCONT_S, new TokenTypes[] {});
			rule2func.put(r1, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.APMINSINGCONT_S, new Rule[] {r1});

			r1 = new Rule(TokenTypes.APMINPLURCONT_S, new TokenTypes[] {});
			rule2func.put(r1, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.APMINPLURCONT_S, new Rule[] {r1});

			r1 = new Rule(TokenTypes.COMNOUN_S, new TokenTypes[]
				{TokenTypes.com_noun, TokenTypes.COMNOUN_S});
			rule2func.put(r1, FunctionNames.p_S_term_30);
			r2 = new Rule(TokenTypes.COMNOUN_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.COMNOUN_S, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.COMPNOUN_S, new TokenTypes[]
				{TokenTypes.comp_noun, TokenTypes.COMPNOUN_S});
			rule2func.put(r1, FunctionNames.p_S_term_30);
			r2 = new Rule(TokenTypes.COMPNOUN_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.COMPNOUN_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.COMPNOUNCONT_L, new TokenTypes[]
				{TokenTypes.KONJDELMINQ, TokenTypes.comp_noun});
			rule2func.put(r1, FunctionNames.p_comp_noun_cont_170);
			rules.put(TokenTypes.COMPNOUNCONT_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.COMPNOUNCONT_S, new TokenTypes[]
				{TokenTypes.COMPNOUNCONT_L, TokenTypes.COMPNOUNCONT_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm_30);
			r2 = new Rule(TokenTypes.COMPNOUNCONT_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.COMPNOUNCONT_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.DELMIN_Q, new TokenTypes[] {TokenTypes.del_min});
			rule2func.put(r1, FunctionNames.p_Q_1_term_30);
			r2 = new Rule(TokenTypes.DELMIN_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.DELMIN_Q, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.DET, new TokenTypes[] {TokenTypes.det});
			rule2func.put(r1, FunctionNames.p_det_90);
			r2 = new Rule(TokenTypes.DET, new TokenTypes[] {TokenTypes.n_gen});
			rule2func.put(r2, FunctionNames.p_det_90);
			r3 = new Rule(TokenTypes.DET, new TokenTypes[] {TokenTypes.prop_n_gen});
			rule2func.put(r3, FunctionNames.p_det_90);
			rules.put(TokenTypes.DET, new Rule[] {r1, r2, r3});

			r1 = new Rule(TokenTypes.DET_Q, new TokenTypes[] {TokenTypes.DET});
			rule2func.put(r1, FunctionNames.p_Q_1_nonterm_30);
			r2 = new Rule(TokenTypes.DET_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.DET_Q, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.DETPOSSPRON, new TokenTypes[] {TokenTypes.poss_pron});
			rule2func.put(r1, FunctionNames.p_det_poss_pron_term_90);
			r2 = new Rule(TokenTypes.DETPOSSPRON, new TokenTypes[] {TokenTypes.DET});
			rule2func.put(r2, FunctionNames.p_det_poss_pron_nonterm_90);
			rules.put(TokenTypes.DETPOSSPRON, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.DETPOSSPRON_Q, new TokenTypes[] {TokenTypes.DETPOSSPRON});
			rule2func.put(r1, FunctionNames.p_Q_1_nonterm_30);
			r2 = new Rule(TokenTypes.DETPOSSPRON_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.DETPOSSPRON_Q, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.HERETHERE_Q, new TokenTypes[] {TokenTypes.here_there});
			rule2func.put(r1, FunctionNames.p_Q_1_term_30);
			r2 = new Rule(TokenTypes.HERETHERE_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.HERETHERE_Q, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.INFVERB_S, new TokenTypes[]
				{TokenTypes.inf_verb, TokenTypes.INFVERB_S});
			rule2func.put(r1, FunctionNames.p_S_term_30);
			r2 = new Rule(TokenTypes.INFVERB_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.INFVERB_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.INFP, new TokenTypes[]
				{TokenTypes.inf, TokenTypes.ADVP_S, TokenTypes.inf_verb, TokenTypes.PART_Q});
			rule2func.put(r1, FunctionNames.p_infp_250);
			r2 = new Rule(TokenTypes.INFP, new TokenTypes[]
				{TokenTypes.inf, TokenTypes.SADVP_S, TokenTypes.inf_verb, TokenTypes.PART_Q});
			rule2func.put(r2, FunctionNames.p_infp_250);
			rules.put(TokenTypes.INFP, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.KONJDELMINQ, new TokenTypes[] {TokenTypes.DELMIN_Q});
			rule2func.put(r1, FunctionNames.p_conj_del_minq_160);
			r2 = new Rule(TokenTypes.KONJDELMINQ, new TokenTypes[] {TokenTypes.KONJ_Q});
			rule2func.put(r2, FunctionNames.p_conj_del_minq_160);
			rules.put(TokenTypes.KONJDELMINQ, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.KONJ_Q, new TokenTypes[] {TokenTypes.konj});
			rule2func.put(r1, FunctionNames.p_Q_1_term_30);
			r2 = new Rule(TokenTypes.KONJ_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.KONJ_Q, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.NGEN_Q, new TokenTypes[] {TokenTypes.n_gen});
			rule2func.put(r1, FunctionNames.p_Q_1_term_30);
			r2 = new Rule(TokenTypes.NGEN_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.NGEN_Q, new Rule[] {r2, r1});

			r1 = new Rule(TokenTypes.NP, new TokenTypes[] {TokenTypes.NPREST_L});
			rule2func.put(r1, FunctionNames.p_np1_180);
			r2 = new Rule(TokenTypes.NP, new TokenTypes[] {TokenTypes.NPSIF_L});
			rule2func.put(r2, FunctionNames.p_np2_190);
			r3 = new Rule(TokenTypes.NP, new TokenTypes[] {TokenTypes.NPPC_L});
			rule2func.put(r3, FunctionNames.p_np3_210);
			r4 = new Rule(TokenTypes.NP, new TokenTypes[] {TokenTypes.NPCOMP_L});
			rule2func.put(r4, FunctionNames.p_np3_210);
			r5 = new Rule(TokenTypes.NP, new TokenTypes[] {TokenTypes.NPCOM_L});
			rule2func.put(r5, FunctionNames.p_np3_210);
			r6 = new Rule(TokenTypes.NP, new TokenTypes[] {TokenTypes.NPPROP_L});
			rule2func.put(r6, FunctionNames.p_np3_210);
			rules.put(TokenTypes.NP, new Rule[] {r1, r2, r3, r4, r5, r6});

			r1 = new Rule(TokenTypes.NP_S, new TokenTypes[] {TokenTypes.NP, TokenTypes.NP_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm_30);
			r2 = new Rule(TokenTypes.NP_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.NP_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.NPCOM_L, new TokenTypes[] {TokenTypes.NPCOM1_L});
			rule2func.put(r1, FunctionNames.p_np_com_210);
			r2 = new Rule(TokenTypes.NPCOM_L, new TokenTypes[] {TokenTypes.NPCOM2_L});
			rule2func.put(r2, FunctionNames.p_np_com_210);
			r3 = new Rule(TokenTypes.NPCOM_L, new TokenTypes[] {TokenTypes.NPCOM3_L});
			rule2func.put(r3, FunctionNames.p_np_com_210);
			rules.put(TokenTypes.NPCOM_L, new Rule[] {r1, r2, r3});

			r1 = new Rule(TokenTypes.NPCOM1_L, new TokenTypes[]
				{TokenTypes.pron, TokenTypes.HERETHERE_Q});
			rule2func.put(r1, FunctionNames.p_np_com_1_200);
			rules.put(TokenTypes.NPCOM1_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.NPCOM2_L, new TokenTypes[]
				{TokenTypes.DET_Q, TokenTypes.poss_pron, TokenTypes.AP_Q,
				 TokenTypes.NGEN_Q, TokenTypes.AP_S, TokenTypes.NUM_Q,
				 TokenTypes.AP_Q, TokenTypes.com_noun, TokenTypes.COMNOUN_S});
			rule2func.put(r1, FunctionNames.p_np_com_2_200);
			rules.put(TokenTypes.NPCOM2_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.NPCOM3_L, new TokenTypes[]
				{TokenTypes.DET_Q, TokenTypes.HERETHERE_Q, TokenTypes.NUM_S,
				 TokenTypes.AP_Q, TokenTypes.NGEN_Q, TokenTypes.AP_S,
				 TokenTypes.NUM_S, TokenTypes.com_noun, TokenTypes.COMNOUN_S});
			rule2func.put(r1, FunctionNames.p_np_com_3_200);
			rules.put(TokenTypes.NPCOM3_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.NPCOMP_L, new TokenTypes[]
				{TokenTypes.DET_Q, TokenTypes.AP_Q, TokenTypes.comp_noun,
				 TokenTypes.COMPNOUNCONT_S, TokenTypes.konj, TokenTypes.NGEN_Q,
				 TokenTypes.NUM_Q, TokenTypes.AP_Q, TokenTypes.com_noun,
				 TokenTypes.COMNOUN_S});
			rule2func.put(r1, FunctionNames.p_np_comp_210);
			rules.put(TokenTypes.NPCOMP_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.NPPC_L, new TokenTypes[]
				{TokenTypes.prop_comp_noun, TokenTypes.PROPCOMPNOUNCONT_S,
				 TokenTypes.KONJ_Q, TokenTypes.com_noun, TokenTypes.COMNOUN_S});
			rule2func.put(r1, FunctionNames.p_nppc_210);
			r2 = new Rule(TokenTypes.NPPC_L, new TokenTypes[]
				{TokenTypes.prop_comp_noun, TokenTypes.PROPCOMPNOUNCONT_S,
				 TokenTypes.KONJ_Q, TokenTypes.prop_noun, TokenTypes.PROPNOUN_S});
			rule2func.put(r2, FunctionNames.p_nppc_210);
			rules.put(TokenTypes.NPPC_L, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.NPPROP_L, new TokenTypes[]
				{TokenTypes.DET_Q, TokenTypes.NUM_Q, TokenTypes.AP_Q,
				 TokenTypes.prop_noun, TokenTypes.PROPNOUN_S});
			rule2func.put(r1, FunctionNames.p_np_prop_210);
			rules.put(TokenTypes.NPPROP_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.NPREST_L, new TokenTypes[]
				{TokenTypes.DETPOSSPRON, TokenTypes.AP_Q});
			rule2func.put(r1, FunctionNames.p_np_rest_180);
			rules.put(TokenTypes.NPREST_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.NPSIF_L, new TokenTypes[]
				{TokenTypes.DETPOSSPRON_Q, TokenTypes.ADVP_Q, TokenTypes.NUMP});
			rule2func.put(r1, FunctionNames.p_np_sif_190);
			rules.put(TokenTypes.NPSIF_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.NUM_Q, new TokenTypes[] {TokenTypes.NUMP});
			rule2func.put(r1, FunctionNames.p_Q_1_nonterm_30);
			r2 = new Rule(TokenTypes.NUM_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.NUM_Q, new Rule[] {r2, r1});

			// TODO: The Python code contains a duplicate rule for 'NUM_S ::='
			r1 = new Rule(TokenTypes.NUM_S, new TokenTypes[]
				{TokenTypes.num, TokenTypes.NUM_S});
			rule2func.put(r1, FunctionNames.p_S_term_30);
			r2 = new Rule(TokenTypes.NUM_S, new TokenTypes[]
				{TokenTypes.NUMP, TokenTypes.NUM_S});
			rule2func.put(r2, FunctionNames.p_S_nonterm_30);
			r3 = new Rule(TokenTypes.NUM_S, new TokenTypes[] {});
			rule2func.put(r3, FunctionNames.p_S_base1_20);
			r4 = new Rule(TokenTypes.NUM_S, new TokenTypes[] {});
			rule2func.put(r4, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.NUM_S, new Rule[] {r3, r4, r2, r1});

			r1 = new Rule(TokenTypes.NUMP, new TokenTypes[]
				{TokenTypes.AP_S, TokenTypes.num, TokenTypes.NUM_S});
			rule2func.put(r1, FunctionNames.p_siffer_170);
			r2 = new Rule(TokenTypes.NUMP, new TokenTypes[]
				{TokenTypes.ADVP_S, TokenTypes.num, TokenTypes.NUM_S});
			rule2func.put(r2, FunctionNames.p_siffer_170);
			rules.put(TokenTypes.NUMP, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.PART_Q, new TokenTypes[] {TokenTypes.part});
			rule2func.put(r1, FunctionNames.p_Q_1_term_30);
			r2 = new Rule(TokenTypes.PART_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.PART_Q, new Rule[] {r1, r2});

			Rule[] rs = new Rule[41];

			rs[0] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.here_there});
			rule2func.put(rs[0], FunctionNames.p_default_10);

			rs[1] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.particip});
			rule2func.put(rs[1], FunctionNames.p_default_10);

			rs[2] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.prep_mellan});
			rule2func.put(rs[2], FunctionNames.p_default_10);

			rs[3] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.sent_adv});
			rule2func.put(rs[3], FunctionNames.p_default_10);

			rs[4] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.adj_plur});
			rule2func.put(rs[4], FunctionNames.p_default_10);

			rs[5] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.adj_sing});
			rule2func.put(rs[5], FunctionNames.p_default_10);

			rs[6] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.adj_sing_plur});
			rule2func.put(rs[6], FunctionNames.p_default_10);

			rs[7] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.adv});
			rule2func.put(rs[7], FunctionNames.p_default_10);

			rs[8] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.com_noun});
			rule2func.put(rs[8], FunctionNames.p_default_10);

			rs[9] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.comp_noun});
			rule2func.put(rs[9], FunctionNames.p_default_10);

			rs[10] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.conj_verb});
			rule2func.put(rs[10], FunctionNames.p_default_10);

			rs[11] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.del_maj});
			rule2func.put(rs[11], FunctionNames.p_default_10);

			rs[12] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.del_min});
			rule2func.put(rs[12], FunctionNames.p_default_10);

			rs[13] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.del_paren});
			rule2func.put(rs[13], FunctionNames.p_default_10);

			rs[14] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.det});
			rule2func.put(rs[14], FunctionNames.p_default_10);

			rs[15] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.fin_verb});
			rule2func.put(rs[15], FunctionNames.p_default_10);

			rs[16] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.imp_verb});
			rule2func.put(rs[16], FunctionNames.p_default_10);

			rs[17] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.inf});
			rule2func.put(rs[17], FunctionNames.p_default_10);

			rs[18] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.inf_verb});
			rule2func.put(rs[18], FunctionNames.p_default_10);

			rs[19] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.interj});
			rule2func.put(rs[19], FunctionNames.p_default_10);

			rs[20] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.konj});
			rule2func.put(rs[20], FunctionNames.p_default_10);

			rs[21] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.n_gen});
			rule2func.put(rs[21], FunctionNames.p_default_10);

			rs[22] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.num});
			rule2func.put(rs[22], FunctionNames.p_default_10);

			rs[23] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.part});
			rule2func.put(rs[23], FunctionNames.p_default_10);

			rs[24] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.poss_pron});
			rule2func.put(rs[24], FunctionNames.p_default_10);

			rs[25] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.prep});
			rule2func.put(rs[25], FunctionNames.p_default_10);

			rs[26] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.pron});
			rule2func.put(rs[26], FunctionNames.p_default_10);

			rs[27] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.prop_comp_noun});
			rule2func.put(rs[27], FunctionNames.p_default_10);

			rs[28] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.prop_n_gen});
			rule2func.put(rs[28], FunctionNames.p_default_10);

			rs[29] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.prop_noun});
			rule2func.put(rs[29], FunctionNames.p_default_10);

			rs[30] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.subj});
			rule2func.put(rs[30], FunctionNames.p_default_10);

			rs[31] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.sup_verb});
			rule2func.put(rs[31], FunctionNames.p_default_10);

			rs[32] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.u_o});
			rule2func.put(rs[32], FunctionNames.p_default_10);

			rs[33] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.DETPOSSPRON});
			rule2func.put(rs[33], FunctionNames.p_phrase_2a_30);

			rs[34] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.ADVP});
			rule2func.put(rs[34], FunctionNames.p_phrase_2a_40);

			rs[35] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.VC});
			rule2func.put(rs[35], FunctionNames.p_phrase_2b_40);

			rs[36] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.AP});
			rule2func.put(rs[36], FunctionNames.p_phrase_3a_50);

			rs[37] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.INFP});
			rule2func.put(rs[37], FunctionNames.p_phrase_3b_50);

			rs[38] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.NUMP});
			rule2func.put(rs[38], FunctionNames.p_phrase_3c_50);

			rs[39] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.NP});
			rule2func.put(rs[39], FunctionNames.p_phrase_4_60);

			rs[40] = new Rule(TokenTypes.PHRASE, new TokenTypes[] {TokenTypes.PP});
			rule2func.put(rs[40], FunctionNames.p_phrase_5_70);

			rules.put(TokenTypes.PHRASE, rs);

			r1 = new Rule(TokenTypes.PHRASE_S, new TokenTypes[]
				{TokenTypes.PHRASE_S, TokenTypes.PHRASE});
			rule2func.put(r1, FunctionNames.p_S_nonterm3_500);
			r2 = new Rule(TokenTypes.PHRASE_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base2_10);
			rules.put(TokenTypes.PHRASE_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.PP, new TokenTypes[]
				{TokenTypes.prep, TokenTypes.AP});
			rule2func.put(r1, FunctionNames.p_pp1_220);
			r2 = new Rule(TokenTypes.PP, new TokenTypes[]
				{TokenTypes.prep, TokenTypes.NP});
			rule2func.put(r2, FunctionNames.p_pp2_230);
			r3 = new Rule(TokenTypes.PP, new TokenTypes[]
				{TokenTypes.prep_mellan, TokenTypes.NP, TokenTypes.konj, TokenTypes.NP});
			rule2func.put(r3, FunctionNames.p_pp_mellan_240);
			r4 = new Rule(TokenTypes.PP, new TokenTypes[]
				{TokenTypes.prep, TokenTypes.konj, TokenTypes.prep, TokenTypes.NP});
			rule2func.put(r4, FunctionNames.p_pp_konj_240);
			rules.put(TokenTypes.PP, new Rule[] {r1, r2, r3, r4});

			r1 = new Rule(TokenTypes.PP_Q, new TokenTypes[] {TokenTypes.PP});
			rule2func.put(r1, FunctionNames.p_Q_1_nonterm_30);
			r2 = new Rule(TokenTypes.PP_Q, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_Q_0_20);
			rules.put(TokenTypes.PP_Q, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.PP_S, new TokenTypes[]
				{TokenTypes.PP, TokenTypes.PP_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm_30);
			r2 = new Rule(TokenTypes.PP_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.PP_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.PROPCOMPNOUNCONT_L, new TokenTypes[]
				{TokenTypes.KONJDELMINQ, TokenTypes.AP_Q, TokenTypes.prop_comp_noun});
			rule2func.put(r1, FunctionNames.p_prop_comp_noun_cont_170);
			rules.put(TokenTypes.PROPCOMPNOUNCONT_L, new Rule[] {r1});

			r1 = new Rule(TokenTypes.PROPCOMPNOUNCONT_S, new TokenTypes[]
				{TokenTypes.PROPCOMPNOUNCONT_L, TokenTypes.PROPCOMPNOUNCONT_S});
			rule2func.put(r1, FunctionNames.p_S_nonterm_30);
			r2 = new Rule(TokenTypes.PROPCOMPNOUNCONT_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.PROPCOMPNOUNCONT_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.PROPNOUN_S, new TokenTypes[]
				{TokenTypes.prop_noun, TokenTypes.PROPNOUN_S});
			rule2func.put(r1, FunctionNames.p_S_term_30);
			r2 = new Rule(TokenTypes.PROPNOUN_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.PROPNOUN_S, new Rule[] {r1, r2});

			r1 = new Rule(TokenTypes.SADVP_S, new TokenTypes[]
				{TokenTypes.sent_adv, TokenTypes.SADVP_S});
			rule2func.put(r1, FunctionNames.p_S_term_30);
			r2 = new Rule(TokenTypes.SADVP_S, new TokenTypes[] {});
			rule2func.put(r2, FunctionNames.p_S_base1_20);
			rules.put(TokenTypes.SADVP_S, new Rule[] {r1, r2});

			// This is the rule that is sent to the constructor if this class
			// Start with this line and add PHRASE tokens as we go along
			//		SENT ::= PHRASE
			sentRule = new Rule(TokenTypes.SENT, new TokenTypes[] {TokenTypes.PHRASE});
			rule2func.put(sentRule, FunctionNames.p_sent_0);
			rules.put(TokenTypes.SENT, new Rule[] {sentRule});

			r1 = new Rule(TokenTypes.VC, new TokenTypes[] {TokenTypes.imp_verb});
			rule2func.put(r1, FunctionNames.p_vc_term_220);
			r2 = new Rule(TokenTypes.VC, new TokenTypes[] {TokenTypes.inf_verb});
			rule2func.put(r2, FunctionNames.p_vc_term_220);
			r3 = new Rule(TokenTypes.VC, new TokenTypes[] {TokenTypes.sup_verb});
			rule2func.put(r3, FunctionNames.p_vc_term_220);
			r4 = new Rule(TokenTypes.VC, new TokenTypes[] {TokenTypes.konj_verb});
			rule2func.put(r4, FunctionNames.p_vc_term_220);
			r5 = new Rule(TokenTypes.VC, new TokenTypes[]
				{TokenTypes.fin_verb, TokenTypes.INFVERB_S});
			rule2func.put(r5, FunctionNames.p_vc_term_list_230);
			r6 = new Rule(TokenTypes.VC, new TokenTypes[]
				{TokenTypes.fin_verb, TokenTypes.INFVERB_S, TokenTypes.sup_verb});
			rule2func.put(r6, FunctionNames.p_vc_term_list_sv_240);
			rules.put(TokenTypes.VC, new Rule[] {r1, r2, r3, r4, r5, r6});
		}

		private class Tree
		{
			private Hashtable<Tuple<StateItem,Integer>,List<Tuple<StateItem,Integer>>> tree = null;

			public Tree()
			{
				tree = new Hashtable<Tuple<StateItem,Integer>,List<Tuple<StateItem,Integer>>>();
			}

			public boolean hasKey(Tuple<StateItem,Integer> t)
			{
				for(Tuple<StateItem,Integer> key : tree.keySet())
					if(key.fst().equals(t.fst()) && key.snd().equals(t.snd()))
						return true;

				return false;
			}

			public void addNewKey(StateItem si, int i)
			{
				tree.put(new Tuple<StateItem,Integer>(si, i),
						new ArrayList<Tuple<StateItem, Integer>>());
			}

			public void append(StateItem keyItem, int keyI, StateItem valItem, int valI)
			{
				for(Tuple<StateItem,Integer> t : tree.keySet())
				{
					if(t.fst().equals(keyItem) && t.snd().equals(keyI))
					{
						tree.get(t).add(new Tuple<StateItem,Integer>(valItem, valI));
						return;
					}
				}
			}

			public List<Tuple<StateItem,Integer>> get(Tuple<StateItem,Integer> t)
			{
				for(Tuple<StateItem,Integer> key : tree.keySet())
					if(key.fst().equals(t.fst()) && key.snd().equals(t.snd()))
						return tree.get(key);

				return null;
			}

			public void remove(int i)
			{
				ArrayList<Tuple<StateItem,Integer>> toRemove =
						new ArrayList<Tuple<StateItem,Integer>>();

				for(Tuple<StateItem,Integer> t : tree.keySet())
					if(i == t.snd())
						toRemove.add(t);

				for(Tuple<StateItem,Integer> t : toRemove)
					tree.remove(t);
			}

			@Override
			public String toString()
			{
				return tree.toString();
			}
		}

		private class State
		{
			private List<StateItem> items = null;

			public State()
			{
				items = new ArrayList<StateItem>();
			}

			public StateItem getItem(int i)
			{
				return items.get(i);
			}

			public void append(StateItem s)
			{
				items.add(s);
			}

			public int size()
			{
				return items.size();
			}

			public List<StateItem> items()
			{
				return items;
			}

			public boolean contains(StateItem s)
			{
				for(StateItem si : items)
					if(si.equals(s))
						return true;

				return false;
			}

			@Override
			public boolean equals(Object o)
			{
				if(o instanceof State)
				{
					State s = (State)o;

					if(items.size() != s.items.size())
						return false;

					for(int i = 0; i < items.size(); i++)
						if(!items.get(i).equals(s.items.get(i)))
							return false;

					return true;
				}
				else
					return false;
			}

			@Override
			public int hashCode()
			{
				return super.hashCode();
			}

			@Override
			public String toString()
			{
				String res = "[";

				for(int i = 0; i < items.size(); i++)
					res += (i == 0 ? "" : ", ") + items.get(i);

				res += "]";

				return res;
			}
		}

		private class StateItem
		{
			private Rule rule = null;
			private int position = 0;
			private int parent = 0;

			public StateItem(Rule rule, int position, int parent)
			{
				this.rule = rule;
				this.position = position;
				this.parent = parent;
			}

			public int getParent()
			{
				return parent;
			}

			public int getPosition()
			{
				return position;
			}

			public Rule getRule()
			{
				return rule;
			}

			@Override
			public boolean equals(Object o)
			{
				if(o instanceof StateItem)
				{
					StateItem si = (StateItem)o;

					return rule.equals(si.rule) && position == si.position &&
							parent == si.parent;
				}
				else
					return false;
			}

			@Override
			public int hashCode()
			{
				return super.hashCode();
			}

			@Override
			public String toString()
			{
				return "<" + rule.toString() + "," + position + "," + parent + ">";
			}
		}

		private class Rule
		{
			private TokenTypes lhs = null;
			private TokenTypes[] rhs = null;

			public Rule(TokenTypes lhs, TokenTypes[] rhs)
			{
				this.lhs = lhs;
				this.rhs = rhs;
			}

			@Override
			public boolean equals(Object o)
			{
				if(o instanceof Rule)
				{
					Rule r = (Rule)o;

					if(lhs != r.lhs || rhs.length != r.rhs.length)
						return false;

					for(int i = 0; i < rhs.length; i++)
						if(rhs[i] != r.rhs[i])
							return false;

					return true;
				}
				else
					return false;
			}

			@Override
			public int hashCode()
			{
				return super.hashCode();
			}

			@Override
			public String toString()
			{
				String result = lhs + " ::=";

				for(int i = 0; i < rhs.length; i++)
					result+= " " + rhs[i];

				return result;
			}
		}

		private class Rules
		{
			private Hashtable<TokenTypes,Rule[]> rules = null;

			public Rules()
			{
				rules = new Hashtable<TokenTypes,Rule[]>();
			}

			public boolean hasKey(TokenTypes key)
			{
				return rules.containsKey(key);
			}

			public void put(TokenTypes key, Rule[] rs)
			{
				rules.put(key, rs);
			}

			public Rule[] get(TokenTypes key)
			{
				Rule[] rs = rules.get(key);
				if(rs == null)
					return new Rule[0];
				else
					return rs;
			}

			public Collection<Rule[]> values()
			{
				return rules.values();
			}
		}

		private class UnionList
		{
			private List<Tuple<TokenTypes,TokenTypes>> list = null;

			public UnionList()
			{
				list = new ArrayList<Tuple<TokenTypes,TokenTypes>>();
			}

			public List<Tuple<TokenTypes,TokenTypes>> get()
			{
				return list;
			}

			public void add(TokenTypes sym, TokenTypes lhs)
			{
				for(Tuple<TokenTypes,TokenTypes> t : list)
					if(sym == t.fst() && lhs == t.snd())
						return;

				list.add(new Tuple<TokenTypes,TokenTypes>(sym, lhs));
			}
		}
	}

	private class AST
	{
		private Object type = null;
		private List kids = null;

		public AST(Object type)
		{
			this.type = type;
			this.kids = new ArrayList<Object>();
		}

		public AST(Object type, List children)
		{
			this.type = type;
			this.kids = children;
		}

		public Object get(int i)
		{
			return kids.get(i);
		}
	}
}

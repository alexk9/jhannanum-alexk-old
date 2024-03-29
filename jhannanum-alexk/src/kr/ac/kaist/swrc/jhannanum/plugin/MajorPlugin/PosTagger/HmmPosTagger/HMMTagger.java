/*  Copyright 2010, 2011 Semantic Web Research Center, KAIST

This file is part of JHanNanum.

JHanNanum is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

JHanNanum is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with JHanNanum.  If not, see <http://www.gnu.org/licenses/>   */

package kr.ac.kaist.swrc.jhannanum.plugin.MajorPlugin.PosTagger.HmmPosTagger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Iterator;

import kr.ac.kaist.swrc.jhannanum.comm.Eojeol;
import kr.ac.kaist.swrc.jhannanum.comm.Sentence;
import kr.ac.kaist.swrc.jhannanum.comm.SetOfSentences;
import kr.ac.kaist.swrc.jhannanum.plugin.MajorPlugin.PosTagger.PosTagger;
import kr.ac.kaist.swrc.jhannanum.share.JSONReader;

/**
 * Hidden Markov Model based Part Of Speech Tagger.
 * 
 * It is a POS Tagger plug-in which is a major plug-in of phase 3 in HanNanum work flow. It uses
 * Hidden Markov Model regarding the features of Korean Eojeol to choose the most promising morphological
 * analysis results of each eojeol for entire sentence.
 * 
 * @author Sangwon Park (hudoni@world.kaist.ac.kr), CILab, SWRC, KAIST
 */
public class HMMTagger implements PosTagger {
	/**
	 * Node for the markov model.
	 * @author Sangwon Park (hudoni@world.kaist.ac.kr), CILab, SWRC, KAIST
	 */
	private class MNode {
		/** eojeol */
		private Eojeol eojeol;
		
		/** 어절 태그 */
		private String wp_tag;
		
		/** the probability of this node - P(T, W) */
		private double prob_wt;
		
		/** the accumulated probability from start to this node */
		private double prob;
		
		/** back pointer for viterbi algorithm */
		private int backptr;
		
		/** the index for the next sibling */
		private int sibling;
	}
	
	/**
	 * Header of an eojeol.
	 * @author Sangwon Park (hudoni@world.kaist.ac.kr), CILab, SWRC, KAIST
	 */
	private class WPhead {
		/** the index of the node for an eojeol */
		private int iIdxOfNodeForEojeol;
	}
	
	/** log 0.01 - smoothing factor */
	private static double SF = -4.60517018598809136803598290936873;
	
	/** the array of nodes for each eojeol */
	private WPhead[] aEojeol = null;
	
	/** the last index of eojeol list */
	private int iLastIdxOfaEojeol = 0;

	/** the nodes for the markov model */
	private MNode[] aNodeForMM = null;
	
	/** the last index of the markov model  */
	private int iLastIdxOfMM = 0;

	/** for the probability P(W|T) */
	private ProbabilityDBM pwt_pos_tf = null;
	
	/** for the probability P(T|T) */
	private ProbabilityDBM ptt_pos_tf = null;
	
	/** for the probability P(T|T) for eojeols */
	private ProbabilityDBM ptt_wp_tf = null;

	/** the statistic file for the probability P(T|W) for morphemes */
	/** 형태소에 대한 P(T|W) 통계 파일 */
	private String PWT_POS_TDBM_FILE;
	
	/** the statistic file for the probability P(T|T) for morphemes */
	/** 형태소에 대한 P(T|T) 통계 파일 */
	private String PTT_POS_TDBM_FILE;
	
	/** the statistic file for the probability P(T|T) for eojeols */
	/** 어절에 대한 P(T|T) 통계 파일 */
	private String PTT_WP_TDBM_FILE;

	/** the default probability */
	final static private double PCONSTANT = -20.0;
	
	/** lambda value */
	final static private double LAMBDA = 0.9;

	/** lambda 1 */
	final static private double Lambda1 = LAMBDA;
	
	/** lambda 2 */
	final static private double Lambda2 = 1.0 - LAMBDA;
	
	Connection conn_pwt_pos = null;
	Connection conn_ptt_pos = null;
	Connection conn_ptt_wp = null;
	
	@Override
	public Sentence tagPOS(SetOfSentences sos) {
		int v = 0, prev_v = 0, w = 0;
		ArrayList<String> plainEojeolArray = sos.getPlainEojeolArray();
		ArrayList<Eojeol[]> eojeolSetArray = sos.getEojeolSetArray();
		
		// initialization
		reset();
		
		Iterator<String> plainEojeolIter = plainEojeolArray.iterator();
		for (Eojeol[] eojeolSet : eojeolSetArray) {
			String plainEojeol = null;
			if (plainEojeolIter.hasNext()) {
				plainEojeol = plainEojeolIter.next();
			} else {
				break;
			}
			w = new_wp(plainEojeol);
			
			for (int i = 0; i < eojeolSet.length; i++) {
				String now_tag;
				double probability;
	
				now_tag = PhraseTag.getPhraseTag(eojeolSet[i].getTags());
				probability = compute_wt(eojeolSet[i]);
				
				v = new_mnode(eojeolSet[i], now_tag, probability);
				if (i == 0) {
					aEojeol[w].iIdxOfNodeForEojeol = v;
					prev_v = v;
				} else {
					aNodeForMM[prev_v].sibling = v;
					prev_v = v;
				}
			}
		}
		
		// gets the final result by running viterbi
		return end_sentence(sos);
	}

	@Override
	public void initialize(String baseDir, String configFile) throws Exception  {
		Class.forName("org.sqlite.JDBC");
	    conn_pwt_pos = DriverManager.getConnection("jdbc:sqlite:db/pwt_pos.db");
	    conn_ptt_pos = DriverManager.getConnection("jdbc:sqlite:db/ptt_pos.db");
	    conn_ptt_wp = DriverManager.getConnection("jdbc:sqlite:db/ptt_wp.db");
		
		
		aEojeol = new WPhead[5000];
		for (int i = 0; i < 5000; i++) {
			aEojeol[i] = new WPhead();
		}
		iLastIdxOfaEojeol = 1;

		aNodeForMM = new MNode[10000];
		for (int i = 0; i < 10000; i++) {
			aNodeForMM[i] = new MNode();
		}
		iLastIdxOfMM = 1;

		JSONReader json = new JSONReader(configFile);
		PWT_POS_TDBM_FILE = baseDir + "/" + json.getValue("pwt.pos");
		PTT_POS_TDBM_FILE = baseDir + "/" + json.getValue("ptt.pos");
		PTT_WP_TDBM_FILE = baseDir + "/" + json.getValue("ptt.wp");
			
		pwt_pos_tf = new ProbabilityDBM(PWT_POS_TDBM_FILE);
		ptt_wp_tf = new ProbabilityDBM(PTT_WP_TDBM_FILE);
		ptt_pos_tf = new ProbabilityDBM(PTT_POS_TDBM_FILE);
		
		
		
	}
	
	@Override
	public void shutdown() {

	}

	/**
	 * Computes P(T_i, W_i) of the specified eojeol.
	 * @param eojeol - the eojeol to compute the probability
	 * @return P(T_i, W_i) of the specified eojeol
	 */
	private double compute_wt(Eojeol eojeol)
	{
		double current = 0.0, tbigram, tunigram, lexicon;

		String tag;
		String bitag;
		String oldtag;

		tag = eojeol.getTag(0);

		/* the probability of P(t1|t0)
		 * 시작부는 반드시 bnk로 시작한다. bnk-xxx 는 P(xxx|bnk)를 의미한다. 
		 */
		bitag = "bnk-" + tag;

		double[] prob = null;

		if ((prob = ptt_pos_tf.get(bitag)) != null) {
			/* current = P(t1|t0) */
			tbigram = prob[0];
		} else {
			/* current = P(t1|t0) = 0.01 */
			tbigram = PCONSTANT;
		}

		/* the probability of P(t1) */
		if ((prob = ptt_pos_tf.get(tag)) != null) {
			/* current = P(t1) */
			tunigram = prob[0];
		} else { 
			/* current = P(t1) = 0.01 */
			tunigram = PCONSTANT;
		}

		/* the probability of P(w|t) */
		if ((prob = pwt_pos_tf.get(eojeol.getMorpheme(0) + "/" + tag)) != null) {
			/* current *= P(w|t1) */
			lexicon = prob[0];
		} else {
			/* current = P(w|t1) = 0.01 */
			lexicon = PCONSTANT;
		}

		/*                              
		 * current = P(w|t1) * P(t1|t0) ~= P(w|t1) * (P(t1|t0))^Lambda1 * (P(t1))^Lambda2 (Lambda1 + Lambda2 = 1)
		 */ 
//		current = lexicon + Lambda1*tbigram + Lambda2*tunigram;

		/* 
		 * current = P(w|t1)/P(t1) * P(t1|t0)/P(t1)
		 */
//		current = lexicon - tunigram + tbigram - tunigram;

		/* 
		 * current = P(w|t1) * P(t1|t0)
		 */
//		current = lexicon + tbigram ;
		
		/* 
		 * current = P(w|t1) * P(t1|t0) / P(t1)
		 */
		current = lexicon + tbigram - tunigram;
		oldtag = tag;


		for (int i = 1; i < eojeol.length; i++) {
			tag = eojeol.getTag(i);

			/* P(t_i|t_i-1) */
			bitag = oldtag + "-" + tag;

			if ((prob = ptt_pos_tf.get(bitag)) != null) {
				tbigram = prob[0];
			} else { 
				tbigram=PCONSTANT;
			}

			/* P(w|t) */
			if ((prob = pwt_pos_tf.get(eojeol.getMorpheme(i) + "/" + tag)) != null) {
				/* current *= P(w|t) */
				lexicon = prob[0];
			} else {
				lexicon = PCONSTANT;
			}

			/* P(t) */
			if ((prob = ptt_pos_tf.get(tag)) != null) {
				/* current = P(t) */
				tunigram = prob[0];
			} else { 
				/* current = P(t)=0.01 */
				tunigram = PCONSTANT;
			}

//			current += lexicon - tunigram + tbigram - tunigram;
//			current += lexicon + tbigram;
			current += lexicon + tbigram - tunigram;

			oldtag = tag;
		}

		/* the blank at the end of eojeol */
		/* 끝부분은 반드시 bnk로 끝난다. */
		bitag = tag + "-bnk";

		/* P(bnk|t_last) */
		if ((prob = ptt_pos_tf.get(bitag)) != null) {
			tbigram = prob[0];
		} else { 
			tbigram = PCONSTANT;
		}

		/* P(bnk) */
		if ((prob = ptt_pos_tf.get("bnk")) != null) {
			tunigram = prob[0];
		} else { 
			tunigram=PCONSTANT;
		}

		/* P(w|bnk) = 1, and ln(1) = 0 */
//		current += 0 - tunigram + tbigram - tunigram;
//		current += 0 + tbigram;
		current += 0 + tbigram - tunigram;

		return current;
	}

	/**
	 * Runs viterbi to get the final morphological analysis result which has the highest probability.
	 * 비터비 알고리즘을 통해서 최상의 확률을 갖는 형태소 분석 결과를 얻는다.
	 * @param sos - 형태소 분석의 모든 후보 
	 * @return 최상의 확률을 갖는 형태소 분석 결과
	 */
	private Sentence end_sentence(SetOfSentences sos) {
		int i, j, k;

		/* Ceartes the last node */
		i = new_wp(" ");
		aEojeol[i].iIdxOfNodeForEojeol = new_mnode(null, "SF", 0);

		/* Runs viterbi */
		for (i = 1; i < iLastIdxOfaEojeol - 1; i++) {
			for (j = aEojeol[i].iIdxOfNodeForEojeol; j != 0; j = aNodeForMM[j].sibling) { 
				for (k = aEojeol[i+1].iIdxOfNodeForEojeol; k != 0; k = aNodeForMM[k].sibling) {
					update_prob_score(j, k);
				}
			}
		}

		i = sos.length;
		Eojeol[] eojeols = new Eojeol[i];
		for (k = aEojeol[i].iIdxOfNodeForEojeol; k != 0; k = aNodeForMM[k].backptr) {
			eojeols[--i] = aNodeForMM[k].eojeol;
		}

		return new Sentence(sos.getDocumentID(), sos.getSentenceID(), sos.isEndOfDocument(), sos.getPlainEojeolArray().toArray(new String[0]), eojeols);
	}

	/**
	 * Adds a new node for the markov model.
	 * @param eojeol - the eojeol to add
	 * @param wp_tag - the eojeol tag
	 * @param prob - the probability P(w|t)
	 * @return the index of the new node
	 */
	private int new_mnode(Eojeol eojeol, String wp_tag, double prob) {
		aNodeForMM[iLastIdxOfMM].eojeol = eojeol;
		aNodeForMM[iLastIdxOfMM].wp_tag = wp_tag;
		aNodeForMM[iLastIdxOfMM].prob_wt = prob;
		aNodeForMM[iLastIdxOfMM].backptr = 0;
		aNodeForMM[iLastIdxOfMM].sibling = 0;
		return iLastIdxOfMM++;
	}
	
	/**
	 * Adds a new header of an eojeol.
	 * @param str - the plain string of the eojeol
	 * @return the index of the new header
	 */
	private int new_wp(String str) {
		aEojeol[iLastIdxOfaEojeol].iIdxOfNodeForEojeol = 0;
		return iLastIdxOfaEojeol++;
	}

	/**
	 * Resets the model.
	 */
	private void reset() {
		iLastIdxOfaEojeol = 1;
		iLastIdxOfMM = 1;
	}

	/**
	 * Updates the probability regarding the transition between two eojeols.
	 * @param from - the previous eojeol
	 * @param to - the current eojeol
	 */
	private void update_prob_score(int from, int to) {
		double PTT;
		double[] prob = null;
		double P;

		/* the traisition probability P(T_i,T_i-1) */
		prob = ptt_wp_tf.get(aNodeForMM[from].wp_tag + "-" +aNodeForMM[to].wp_tag);
		
		if (prob == null) {
			/* ln(0.01). Smoothing Factor */
			PTT = SF;
		} else {
			PTT = prob[0];
		}
		
		/* P(T_i,T_i-1) / P(T_i) */
		prob = ptt_wp_tf.get(aNodeForMM[to].wp_tag);
		
		if (prob != null) {
			PTT -= prob[0];
		}
		
		/* P(T_i,T_i-1) / (P(T_i) * P(T_i-1)) */
//		prob = ptt_wp_tf.get(mn[from].wp_tag);
//		
//		if (prob != null) {
//			PTT -= prob[0];
//		}

		if (aNodeForMM[from].backptr == 0) {
			aNodeForMM[from].prob = aNodeForMM[from].prob_wt;
		}

		/* 
		 * P = the accumulated probability to the previous eojeol * transition probability * the probability of current eojeol
		 * PTT = P(T_i|T_i-1) / P(T_i)
		 * mn[to].prob_wt = P(T_i, W_i)
 		 */
		P = aNodeForMM[from].prob + PTT + aNodeForMM[to].prob_wt;

		// for debugging
//		System.out.format("P:%f\t%s(%d:%s):%f -> %f -> %s(%d:%s):%f\n", P, mn[from].eojeol, 
//				from, mn[from].wp_tag, mn[from].prob, PTT, 
//				mn[to].eojeol, to, mn[to].wp_tag, mn[to].prob_wt );
	
		if (aNodeForMM[to].backptr == 0 || P > aNodeForMM[to].prob) {
			aNodeForMM[to].backptr = from;
			aNodeForMM[to].prob = P; 
		}
	}
}

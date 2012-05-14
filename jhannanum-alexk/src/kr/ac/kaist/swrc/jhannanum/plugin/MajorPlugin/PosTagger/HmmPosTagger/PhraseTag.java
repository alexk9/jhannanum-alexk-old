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

/**
 * This class is to generate the eojeol tag which represents the features of morphemes in an eojeol.
 * @author Sangwon Park (hudoni@world.kaist.ac.kr), CILab, SWRC, KAIST
 */
public class PhraseTag {
	/**
	 * 어절 내의 형태소 순서에 기반하여 어절태그를 생성한다. 어절 태그값은 다음과 같다.
	 * N(체언), P(용언), M(수식언), I(독립언), J(관계언), E(어미), X(접사), S(기호), F(외국어)
	 * @param tags - the sequence of morpheme tags in an eojeol
	 * @return the eojeol tag
	 */
	public static String getPhraseTag(String[] tags) {
		char[] res = {'.', '.'};
		int end = tags.length - 1;
		
		if (tags.length < 4) {
			String[] tmp = {"", "", "", ""};
	
			/* 초기화 */
			for (int i = 0 ; i < tags.length; i++) {
				tmp[i] = tags[i];
			}
			tags = tmp;
		}

		if (tags.length <= 0 || tags[0].length() == 0) {
			return String.copyValueOf(res);
		}

		// checks the tags in order
		switch (tags[0].charAt(0)) {
		case 'm':
			/*
			 * maj		// 접속부사 
			 * mag		// 일반부사
			 * mad		// 지시부사  
			 */
			if (tags[0].startsWith("ma")) {
				if (tags[1].startsWith("p")) {
					res[0] = 'P';
				} else if (tags[1].startsWith("x")) {
					res[0] = 'P';
				} else if (tags[1].startsWith("jcp")) {
					res[0] = 'P';
				} else {
					res[0] = 'A';
				}
			}
			/*
			 * mmd		// 지시관형사 
			 * mma		// 성상관형사  
			 */			
			else if (tags[0].matches("m^a.*")) {
				if (tags[end].startsWith("j")) {
					res[0] = 'N';
				} else if (tags[1].startsWith("n")) {
					res[0] = 'N';
				} else if (tags[1].startsWith("p")) {
					res[0] = 'P';
				} else {
					res[0] = 'M';
				}
			}
			break;

		case 'e':
			/*
				ecc		// 대등적 연결어미
				ecs		// 종속적 연결어미
			 */
			if (tags[0].startsWith("ecc") || tags[0].startsWith("ecs")) {
				res[0] = 'C';
			}
			break;

		case 'f':
			res[0] = 'N';
			break;

		case 'i':
			if (tags[1].startsWith("j")) {
				res[0] = 'N';
			} else {
				res[0] = 'I';
			}
			break;

		case 'n'://명사계열
			/* 두번째 태그
				xsvv	// 동사 파생 접미사 -- 동사 뒤
				xsva	// 동사 파생 접미사 -- 동작명사 뒤
				xsvn	// 동사 파생 접미사 -- 일반명사 뒤
				xsms	// 형용사 파생 접미사 -- 상태명사 뒤
				xsmn	// 형용사 파생 접미사 -- 일반명사 뒤
			 */
			if (tags[1].matches("x.(v|m).*")) {
				/* 세번째, 네번째 태그
					xsnu	// 명사 파생 접미사 -- 단위 뒤
					xsnca	// 명사 파생 접미사 -- 일반명사 뒤--동작명사화
					xsncc	// 명사 파생 접미사 -- 일반명사 뒤
					xsna	// 명사 파생 접미사 -- 동작성 뒤
					xsns	// 명사 파생 접미사 -- 상태성 뒤
					xsnp	// 명사 파생 접미사 -- 인명1,3 뒤
					xsnx	// 명사 파생 접미사 -- 모든 명사 뒤
				 */
				if (tags[2].matches("..n.*") || tags[3].matches("..n.*")) {
					res[0] = 'N';
				} else {
					res[0] = 'P';
				}
			}
			/* 두번째 태그
				xsnu	// 명사 파생 접미사 -- 단위 뒤
				xsnca	// 명사 파생 접미사 -- 일반명사 뒤--동작명사화
				xsncc	// 명사 파생 접미사 -- 일반명사 뒤
				xsna	// 명사 파생 접미사 -- 동작성 뒤
				xsns	// 명사 파생 접미사 -- 상태성 뒤
				xsnp	// 명사 파생 접미사 -- 인명1,3 뒤
				xsnx	// 명사 파생 접미사 -- 모든 명사 뒤

			 */
			else if (tags[1].matches("x.n.*")) {
				res[0] = 'N';
			}
			/* 두번째 태그
				npp		// 인칭대명사 
				npd		// 지시대명사 				
			 */
			else if (tags[1].startsWith("p")) {
				/* 세번째, 네번째 태그
					xsnu	// 명사 파생 접미사 -- 단위 뒤
					xsnca	// 명사 파생 접미사 -- 일반명사 뒤--동작명사화
					xsncc	// 명사 파생 접미사 -- 일반명사 뒤
					xsna	// 명사 파생 접미사 -- 동작성 뒤
					xsns	// 명사 파생 접미사 -- 상태성 뒤
					xsnp	// 명사 파생 접미사 -- 인명1,3 뒤
					xsnx	// 명사 파생 접미사 -- 모든 명사 뒤
				 */				
				if (tags[2].matches("..n.*") || tags[3].matches("..n.*")) {
					res[0] = 'N';
				} else {
					res[0] = 'P';
				}
			} 
			else {
				res[0] = 'N';
			}
			break;

		/*
			pvd		// 지시 동사 
			pvg		// 일반 동사 
			pad		// 지시형용사 
			paa		// 성상형용사 
			px		// 보조용언 
		 */
		case 'p':
			/* 두번째 태그
				xsam	// 부사 파생 접미사  -- 형용사 뒤
				xsas	// 부사 파생 접미사 -- 상태명사 뒤
			 */
			if (tags[1].startsWith("xsa")) {
				res[0] = 'A';
			} 
			/* 두번째 태그
				etn		// 명사형 어미
			   세번째 태그
			    n~ //명사계열
			 */
			else if (tags[1].startsWith("etn") || tags[2].startsWith("n")) {
				res[0] = 'N';
			} else {
				res[0] = 'P';
			}
			break;

			/*
			sp		// 쉼표
			sf		// 마침표
			sl		// 여는 따옴표 및 묶음표
			sr		// 닫는 따옴표 및 묶음표
			sd		// 이음표
			se		// 줄임표
			su		// 단위 기호
			sy		// 기타 기호
			 */
		case 's':
			/* 두번째 태그
			   su		// 단위 기호
			   세번째 태그
			   jcs		// 주격조사 
				jco		// 목적격조사 
				jcc		// 보격조사 
				jcm		// 관형격조사 
				jcv		// 호격조사 
				jca		// 부사격조사 
				jcj		// 접속격조사 
				jct		// 공동격조사 
				jcr		// 인용격조사 
				jxc		// 통용보조사 
				jxf		// 종결보조사 
				jp		// 서술격조사 
			 */
			if (tags[1].startsWith("su") || tags[2].startsWith("j")) {
				res[0] = 'N';
			}
			/* 세번째 태그
                n~ //명사계열
              	마지막 태그
               j~//조사계열
			 */
			else if (tags[2].startsWith("n") || tags[end].startsWith("j")) {
				res[0] = 'N';
			} 
			else {
				res[0] = 'S';
			}
			/*
			 첫번째 태그
			 sf		// 마침표
			 두번째 태그
			 	sp		// 쉼표
				sf		// 마침표
				sl		// 여는 따옴표 및 묶음표
				sr		// 닫는 따옴표 및 묶음표
				sd		// 이음표
				se		// 줄임표
				su		// 단위 기호
				sy		// 기타 기호
			 */
			if (tags[0].startsWith("sf") || tags[1].startsWith("s")) {
				res[1] = 'F';
			}
			break;

		case 'x':
			/*
				xp		// 접두사 
				xsnu	// 명사 파생 접미사 -- 단위 뒤
				xsnca	// 명사 파생 접미사 -- 일반명사 뒤--동작명사화
				xsncc	// 명사 파생 접미사 -- 일반명사 뒤
				xsna	// 명사 파생 접미사 -- 동작성 뒤
				xsns	// 명사 파생 접미사 -- 상태성 뒤
				xsnp	// 명사 파생 접미사 -- 인명1,3 뒤
				xsnx	// 명사 파생 접미사 -- 모든 명사 뒤
				xsvv	// 동사 파생 접미사 -- 동사 뒤
				xsva	// 동사 파생 접미사 -- 동작명사 뒤
				xsvn	// 동사 파생 접미사 -- 일반명사 뒤
				xsms	// 형용사 파생 접미사 -- 상태명사 뒤
				xsmn	// 형용사 파생 접미사 -- 일반명사 뒤
				xsam	// 부사 파생 접미사  -- 형용사 뒤
				xsas	// 부사 파생 접미사 -- 상태명사 뒤
			 */
			if (tags[0].startsWith("xsn") || tags[0].startsWith("xp")) {
				res[0] = 'N';
			}
			break;
		}

		// checks the last tag
		String lastTag = tags[end];
		switch (lastTag.charAt(0)) {
		case 'e':
			/*
				ecc		// 대등적 연결어미 
				ecs		// 종속적 연결어미 
				ecx		// 보조적 연결어미 
			 */
			if (lastTag.startsWith("ecc") || lastTag.startsWith("ecs") || lastTag.startsWith("ecx")) {
				res[1] = 'C';
			}
			//ef		// 종결어미 
			else if (lastTag.startsWith("ef")) {
				res[1] = 'F';
			} 
			//etm		// 관형사형 어미 
			else if (lastTag.startsWith("etm")) {
				res[1] = 'M';
			}
			//etn		// 명사형 어미
			else if (lastTag.startsWith("etn")) {
				res[1] = 'N';
			}
			break;

		case 'j':
			//jcv		// 호격조사 
			if (lastTag.startsWith("jcv")) {
				res[0] = 'I';
			}
			/*
				jxc		// 통용보조사 
				jxf		// 종결보조사 
			 */
			else if (lastTag.startsWith("jx")) {
				if (res[0] == 'A') {
					res[1] = 'J';
				} else {
					res[1] = 'X';
				}
			}
			//jcj		// 접속격조사 
			else if (lastTag.startsWith("jcj")) {
				if (res[0] == 'A'){
					res[1] = 'J';
				} else {
					res[1] = 'Y';
				}
			}
			//jca		// 부사격조사 
			else if (lastTag.startsWith("jca")) {
				res[1] = 'A';
			} 
			//jcm		// 관형격조사 
			else if (lastTag.startsWith("jcm")) {
				if (res[0] == 'A') {
					res[1] = 'J';
				} else {
					res[1] = 'M';
				}
			}
			/*
				jcs		// 주격조사 
				jco		// 목적격조사 
				jcc		// 보격조사 
				jct		// 공동격조사 
				jcr		// 인용격조사 
			 */
			else if (lastTag.startsWith("jc")) {
				res[1] = 'J';
			}
			break;
			
		case 'm':
			/*
				mmd		// 지시관형사
				mma		// 성상관형사
			 */
			if (lastTag.matches("m^a.*")) {
				res[1] = 'M';
			}
			/*
				mag		// 일반부사 
			 */
			else if (lastTag.startsWith("mag")) {
				res[1] = 'A';
			}
			break;

		case 'n':
			// n~ 명사계열
			if (lastTag.startsWith("n")) {
				res[0] = 'N';
			}
			break;
		
		case 'x':
			/*
			xsam	// 부사 파생 접미사  -- 형용사 뒤
			xsas	// 부사 파생 접미사 -- 상태명사 뒤
			 */
			if (lastTag.startsWith("xsa")) {
				res[1] = 'A';
			}
			break;
			
			
		}

		// post processing
		if (res[0] == res[1]) {
			res[1] = '.';
		} else if (res[0] == '.') {
			res[0] = res[1];
			res[1] = '.';
		}

		if (res[0] == 'A') {
			if (res[1] == 'M') {
				res[0] = 'N';
			}
		} else if (res[0] == 'M') {
			if (res[1] == 'A') {
				res[0] = 'A';
			} else if (res[1] == 'F') {
				res[0] = 'N';
			} else if (res[1] == 'C') {
				res[0] = 'N';
			}
		} else if (res[0] == 'I') {
			if (res[1] == 'M' || res[1] == 'J') {
				res[0] = 'N';
			} else if (res[1] == 'C') {
				res[0] = 'P';
			} else if( res[1] == 'F') {
				res[0] = 'N';
			}
		}

		if (res[0] == res[1]) {
			res[1] = '.';
		}

		return String.copyValueOf(res);
	}
}
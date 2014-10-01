/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package debugging;

import com.opendoorlogistics.core.formulae.Functions.FmPostcodeUk;
import com.opendoorlogistics.core.formulae.Functions.FmConst;
import com.opendoorlogistics.core.gis.postcodes.UKPostcodes.UKPostcodeLevel;

public class TestPCFunctions {

	public static void main(String[] args) {
		for(String pc: new String[]{"gl51 8bg" , "9 dorset avenue, gl51 8bg, uk",
				"30 Thornton Street, Hertford, Hertfordshire SG14 1QH, UK"}){
			for(UKPostcodeLevel level:UKPostcodeLevel.values()){
				FmPostcodeUk fnc = new FmPostcodeUk(level,new FmConst(pc));
				System.out.println(level + "(" + pc + ") -> " + fnc.execute(null));							
			}
		}
	}

}

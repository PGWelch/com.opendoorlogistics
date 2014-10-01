/*******************************************************************************
 * Copyright (c) 2014 Open Door Logistics (www.opendoorlogistics.com)
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser Public License v3
 * which accompanies this distribution, and is available at http://www.gnu.org/licenses/lgpl.txt
 ******************************************************************************/
package com.opendoorlogistics.components.geocode;

import java.util.Arrays;

import com.opendoorlogistics.core.utils.strings.Strings;

/**
 * ISO countries list with codes
 * @author Phil
 *
 */
final public class Countries {
	
	public static class Country{
		private String name;
		private String twoDigitCode="";
		private String threeDigitCode="";
		private String number="";
		
		private Country(){}
		
		public Country(String name){
			this.name = name;
		}
		
		public String getName() {
			return name;
		}
		public String getTwoDigitCode() {
			return twoDigitCode;
		}
		public String getThreeDigitCode() {
			return threeDigitCode;
		}
		public String getNumber() {
			return number;
		}
		
		@Override
		public String toString(){
			return name;
		}
	}
	
	private static final String[][] countries = new String[][]{
		new String[]{"Afghanistan", "AF", "AFG", "004"},
		new String[]{"Åland Islands", "AX", "ALA", "248"},
		new String[]{"Albania", "AL", "ALB", "008"},
		new String[]{"Algeria", "DZ", "DZA", "012"},
		new String[]{"American Samoa", "AS", "ASM", "016"},
		new String[]{"Andorra", "AD", "AND", "020"},
		new String[]{"Angola", "AO", "AGO", "024"},
		new String[]{"Anguilla", "AI", "AIA", "660"},
		new String[]{"Antarctica", "AQ", "ATA", "010"},
		new String[]{"Antigua and Barbuda", "AG", "ATG", "028"},
		new String[]{"Argentina", "AR", "ARG", "032"},
		new String[]{"Armenia", "AM", "ARM", "051"},
		new String[]{"Aruba", "AW", "ABW", "533"},
		new String[]{"Australia", "AU", "AUS", "036"},
		new String[]{"Austria", "AT", "AUT", "040"},
		new String[]{"Azerbaijan", "AZ", "AZE", "031"},
		new String[]{"Bahamas", "BS", "BHS", "044"},
		new String[]{"Bahrain", "BH", "BHR", "048"},
		new String[]{"Bangladesh", "BD", "BGD", "050"},
		new String[]{"Barbados", "BB", "BRB", "052"},
		new String[]{"Belarus", "BY", "BLR", "112"},
		new String[]{"Belgium", "BE", "BEL", "056"},
		new String[]{"Belize", "BZ", "BLZ", "084"},
		new String[]{"Benin", "BJ", "BEN", "204"},
		new String[]{"Bermuda", "BM", "BMU", "060"},
		new String[]{"Bhutan", "BT", "BTN", "064"},
		new String[]{"Bolivia, Plurinational State of", "BO", "BOL", "068"},
		new String[]{"Bonaire, Sint Eustatius and Saba", "BQ", "BES", "535"},
		new String[]{"Bosnia and Herzegovina", "BA", "BIH", "070"},
		new String[]{"Botswana", "BW", "BWA", "072"},
		new String[]{"Bouvet Island", "BV", "BVT", "074"},
		new String[]{"Brazil", "BR", "BRA", "076"},
		new String[]{"British Indian Ocean Territory", "IO", "IOT", "086"},
		new String[]{"Brunei Darussalam", "BN", "BRN", "096"},
		new String[]{"Bulgaria", "BG", "BGR", "100"},
		new String[]{"Burkina Faso", "BF", "BFA", "854"},
		new String[]{"Burundi", "BI", "BDI", "108"},
		new String[]{"Cambodia", "KH", "KHM", "116"},
		new String[]{"Cameroon", "CM", "CMR", "120"},
		new String[]{"Canada", "CA", "CAN", "124"},
		new String[]{"Cape Verde", "CV", "CPV", "132"},
		new String[]{"Cayman Islands", "KY", "CYM", "136"},
		new String[]{"Central African Republic", "CF", "CAF", "140"},
		new String[]{"Chad", "TD", "TCD", "148"},
		new String[]{"Chile", "CL", "CHL", "152"},
		new String[]{"China", "CN", "CHN", "156"},
		new String[]{"Christmas Island", "CX", "CXR", "162"},
		new String[]{"Cocos (Keeling) Islands", "CC", "CCK", "166"},
		new String[]{"Colombia", "CO", "COL", "170"},
		new String[]{"Comoros", "KM", "COM", "174"},
		new String[]{"Congo", "CG", "COG", "178"},
		new String[]{"Congo, the Democratic Republic of the", "CD", "COD", "180"},
		new String[]{"Cook Islands", "CK", "COK", "184"},
		new String[]{"Costa Rica", "CR", "CRI", "188"},
		new String[]{"Côte d'Ivoire", "CI", "CIV", "384"},
		new String[]{"Croatia", "HR", "HRV", "191"},
		new String[]{"Cuba", "CU", "CUB", "192"},
		new String[]{"Curaçao", "CW", "CUW", "531"},
		new String[]{"Cyprus", "CY", "CYP", "196"},
		new String[]{"Czech Republic", "CZ", "CZE", "203"},
		new String[]{"Denmark", "DK", "DNK", "208"},
		new String[]{"Djibouti", "DJ", "DJI", "262"},
		new String[]{"Dominica", "DM", "DMA", "212"},
		new String[]{"Dominican Republic", "DO", "DOM", "214"},
		new String[]{"Ecuador", "EC", "ECU", "218"},
		new String[]{"Egypt", "EG", "EGY", "818"},
		new String[]{"El Salvador", "SV", "SLV", "222"},
		new String[]{"Equatorial Guinea", "GQ", "GNQ", "226"},
		new String[]{"Eritrea", "ER", "ERI", "232"},
		new String[]{"Estonia", "EE", "EST", "233"},
		new String[]{"Ethiopia", "ET", "ETH", "231"},
		new String[]{"Falkland Islands (Malvinas)", "FK", "FLK", "238"},
		new String[]{"Faroe Islands", "FO", "FRO", "234"},
		new String[]{"Fiji", "FJ", "FJI", "242"},
		new String[]{"Finland", "FI", "FIN", "246"},
		new String[]{"France", "FR", "FRA", "250"},
		new String[]{"French Guiana", "GF", "GUF", "254"},
		new String[]{"French Polynesia", "PF", "PYF", "258"},
		new String[]{"French Southern Territories", "TF", "ATF", "260"},
		new String[]{"Gabon", "GA", "GAB", "266"},
		new String[]{"Gambia", "GM", "GMB", "270"},
		new String[]{"Georgia", "GE", "GEO", "268"},
		new String[]{"Germany", "DE", "DEU", "276"},
		new String[]{"Ghana", "GH", "GHA", "288"},
		new String[]{"Gibraltar", "GI", "GIB", "292"},
		new String[]{"Greece", "GR", "GRC", "300"},
		new String[]{"Greenland", "GL", "GRL", "304"},
		new String[]{"Grenada", "GD", "GRD", "308"},
		new String[]{"Guadeloupe", "GP", "GLP", "312"},
		new String[]{"Guam", "GU", "GUM", "316"},
		new String[]{"Guatemala", "GT", "GTM", "320"},
		new String[]{"Guernsey", "GG", "GGY", "831"},
		new String[]{"Guinea", "GN", "GIN", "324"},
		new String[]{"Guinea-Bissau", "GW", "GNB", "624"},
		new String[]{"Guyana", "GY", "GUY", "328"},
		new String[]{"Haiti", "HT", "HTI", "332"},
		new String[]{"Heard Island and McDonald Islands", "HM", "HMD", "334"},
		new String[]{"Holy See (Vatican City State)", "VA", "VAT", "336"},
		new String[]{"Honduras", "HN", "HND", "340"},
		new String[]{"Hong Kong", "HK", "HKG", "344"},
		new String[]{"Hungary", "HU", "HUN", "348"},
		new String[]{"Iceland", "IS", "ISL", "352"},
		new String[]{"India", "IN", "IND", "356"},
		new String[]{"Indonesia", "ID", "IDN", "360"},
		new String[]{"Iran, Islamic Republic of", "IR", "IRN", "364"},
		new String[]{"Iraq", "IQ", "IRQ", "368"},
		new String[]{"Ireland", "IE", "IRL", "372"},
		new String[]{"Isle of Man", "IM", "IMN", "833"},
		new String[]{"Israel", "IL", "ISR", "376"},
		new String[]{"Italy", "IT", "ITA", "380"},
		new String[]{"Jamaica", "JM", "JAM", "388"},
		new String[]{"Japan", "JP", "JPN", "392"},
		new String[]{"Jersey", "JE", "JEY", "832"},
		new String[]{"Jordan", "JO", "JOR", "400"},
		new String[]{"Kazakhstan", "KZ", "KAZ", "398"},
		new String[]{"Kenya", "KE", "KEN", "404"},
		new String[]{"Kiribati", "KI", "KIR", "296"},
		new String[]{"Korea, Democratic People's Republic of", "KP", "PRK", "408"},
		new String[]{"Korea, Republic of", "KR", "KOR", "410"},
		new String[]{"Kuwait", "KW", "KWT", "414"},
		new String[]{"Kyrgyzstan", "KG", "KGZ", "417"},
		new String[]{"Lao People's Democratic Republic", "LA", "LAO", "418"},
		new String[]{"Latvia", "LV", "LVA", "428"},
		new String[]{"Lebanon", "LB", "LBN", "422"},
		new String[]{"Lesotho", "LS", "LSO", "426"},
		new String[]{"Liberia", "LR", "LBR", "430"},
		new String[]{"Libya", "LY", "LBY", "434"},
		new String[]{"Liechtenstein", "LI", "LIE", "438"},
		new String[]{"Lithuania", "LT", "LTU", "440"},
		new String[]{"Luxembourg", "LU", "LUX", "442"},
		new String[]{"Macao", "MO", "MAC", "446"},
		new String[]{"Macedonia, The Former Yugoslav Republic of", "MK", "MKD", "807"},
		new String[]{"Madagascar", "MG", "MDG", "450"},
		new String[]{"Malawi", "MW", "MWI", "454"},
		new String[]{"Malaysia", "MY", "MYS", "458"},
		new String[]{"Maldives", "MV", "MDV", "462"},
		new String[]{"Mali", "ML", "MLI", "466"},
		new String[]{"Malta", "MT", "MLT", "470"},
		new String[]{"Marshall Islands", "MH", "MHL", "584"},
		new String[]{"Martinique", "MQ", "MTQ", "474"},
		new String[]{"Mauritania", "MR", "MRT", "478"},
		new String[]{"Mauritius", "MU", "MUS", "480"},
		new String[]{"Mayotte", "YT", "MYT", "175"},
		new String[]{"Mexico", "MX", "MEX", "484"},
		new String[]{"Micronesia, Federated States of", "FM", "FSM", "583"},
		new String[]{"Moldova, Republic of", "MD", "MDA", "498"},
		new String[]{"Monaco", "MC", "MCO", "492"},
		new String[]{"Mongolia", "MN", "MNG", "496"},
		new String[]{"Montenegro", "ME", "MNE", "499"},
		new String[]{"Montserrat", "MS", "MSR", "500"},
		new String[]{"Morocco", "MA", "MAR", "504"},
		new String[]{"Mozambique", "MZ", "MOZ", "508"},
		new String[]{"Myanmar", "MM", "MMR", "104"},
		new String[]{"Namibia", "NA", "NAM", "516"},
		new String[]{"Nauru", "NR", "NRU", "520"},
		new String[]{"Nepal", "NP", "NPL", "524"},
		new String[]{"Netherlands", "NL", "NLD", "528"},
		new String[]{"New Caledonia", "NC", "NCL", "540"},
		new String[]{"New Zealand", "NZ", "NZL", "554"},
		new String[]{"Nicaragua", "NI", "NIC", "558"},
		new String[]{"Niger", "NE", "NER", "562"},
		new String[]{"Nigeria", "NG", "NGA", "566"},
		new String[]{"Niue", "NU", "NIU", "570"},
		new String[]{"Norfolk Island", "NF", "NFK", "574"},
		new String[]{"Northern Mariana Islands", "MP", "MNP", "580"},
		new String[]{"Norway", "NO", "NOR", "578"},
		new String[]{"Oman", "OM", "OMN", "512"},
		new String[]{"Pakistan", "PK", "PAK", "586"},
		new String[]{"Palau", "PW", "PLW", "585"},
		new String[]{"Palestine, State of", "PS", "PSE", "275"},
		new String[]{"Panama", "PA", "PAN", "591"},
		new String[]{"Papua New Guinea", "PG", "PNG", "598"},
		new String[]{"Paraguay", "PY", "PRY", "600"},
		new String[]{"Peru", "PE", "PER", "604"},
		new String[]{"Philippines", "PH", "PHL", "608"},
		new String[]{"Pitcairn", "PN", "PCN", "612"},
		new String[]{"Poland", "PL", "POL", "616"},
		new String[]{"Portugal", "PT", "PRT", "620"},
		new String[]{"Puerto Rico", "PR", "PRI", "630"},
		new String[]{"Qatar", "QA", "QAT", "634"},
		new String[]{"Réunion", "RE", "REU", "638"},
		new String[]{"Romania", "RO", "ROU", "642"},
		new String[]{"Russian Federation", "RU", "RUS", "643"},
		new String[]{"Rwanda", "RW", "RWA", "646"},
		new String[]{"Saint Barthélemy", "BL", "BLM", "652"},
		new String[]{"Saint Helena, Ascension and Tristan da Cunha", "SH", "SHN", "654"},
		new String[]{"Saint Kitts and Nevis", "KN", "KNA", "659"},
		new String[]{"Saint Lucia", "LC", "LCA", "662"},
		new String[]{"Saint Martin (French part)", "MF", "MAF", "663"},
		new String[]{"Saint Pierre and Miquelon", "PM", "SPM", "666"},
		new String[]{"Saint Vincent and the Grenadines", "VC", "VCT", "670"},
		new String[]{"Samoa", "WS", "WSM", "882"},
		new String[]{"San Marino", "SM", "SMR", "674"},
		new String[]{"Sao Tome and Principe", "ST", "STP", "678"},
		new String[]{"Saudi Arabia", "SA", "SAU", "682"},
		new String[]{"Senegal", "SN", "SEN", "686"},
		new String[]{"Serbia", "RS", "SRB", "688"},
		new String[]{"Seychelles", "SC", "SYC", "690"},
		new String[]{"Sierra Leone", "SL", "SLE", "694"},
		new String[]{"Singapore", "SG", "SGP", "702"},
		new String[]{"Sint Maarten (Dutch part)", "SX", "SXM", "534"},
		new String[]{"Slovakia", "SK", "SVK", "703"},
		new String[]{"Slovenia", "SI", "SVN", "705"},
		new String[]{"Solomon Islands", "SB", "SLB", "090"},
		new String[]{"Somalia", "SO", "SOM", "706"},
		new String[]{"South Africa", "ZA", "ZAF", "710"},
		new String[]{"South Georgia and the South Sandwich Islands", "GS", "SGS", "239"},
		new String[]{"South Sudan", "SS", "SSD", "728"},
		new String[]{"Spain", "ES", "ESP", "724"},
		new String[]{"Sri Lanka", "LK", "LKA", "144"},
		new String[]{"Sudan", "SD", "SDN", "729"},
		new String[]{"Suriname", "SR", "SUR", "740"},
		new String[]{"Svalbard and Jan Mayen", "SJ", "SJM", "744"},
		new String[]{"Swaziland", "SZ", "SWZ", "748"},
		new String[]{"Sweden", "SE", "SWE", "752"},
		new String[]{"Switzerland", "CH", "CHE", "756"},
		new String[]{"Syrian Arab Republic", "SY", "SYR", "760"},
		new String[]{"Taiwan, Province of China", "TW", "TWN", "158"},
		new String[]{"Tajikistan", "TJ", "TJK", "762"},
		new String[]{"Tanzania, United Republic of", "TZ", "TZA", "834"},
		new String[]{"Thailand", "TH", "THA", "764"},
		new String[]{"Timor-Leste", "TL", "TLS", "626"},
		new String[]{"Togo", "TG", "TGO", "768"},
		new String[]{"Tokelau", "TK", "TKL", "772"},
		new String[]{"Tonga", "TO", "TON", "776"},
		new String[]{"Trinidad and Tobago", "TT", "TTO", "780"},
		new String[]{"Tunisia", "TN", "TUN", "788"},
		new String[]{"Turkey", "TR", "TUR", "792"},
		new String[]{"Turkmenistan", "TM", "TKM", "795"},
		new String[]{"Turks and Caicos Islands", "TC", "TCA", "796"},
		new String[]{"Tuvalu", "TV", "TUV", "798"},
		new String[]{"Uganda", "UG", "UGA", "800"},
		new String[]{"Ukraine", "UA", "UKR", "804"},
		new String[]{"United Arab Emirates", "AE", "ARE", "784"},
		new String[]{"United Kingdom", "GB", "GBR", "826"},
		new String[]{"United States", "US", "USA", "840"},
		new String[]{"United States Minor Outlying Islands", "UM", "UMI", "581"},
		new String[]{"Uruguay", "UY", "URY", "858"},
		new String[]{"Uzbekistan", "UZ", "UZB", "860"},
		new String[]{"Vanuatu", "VU", "VUT", "548"},
		new String[]{"Venezuela, Bolivarian Republic of", "VE", "VEN", "862"},
		new String[]{"Viet Nam", "VN", "VNM", "704"},
		new String[]{"Virgin Islands, British", "VG", "VGB", "092"},
		new String[]{"Virgin Islands, U.S.", "VI", "VIR", "850"},
		new String[]{"Wallis and Futuna", "WF", "WLF", "876"},
		new String[]{"Western Sahara", "EH", "ESH", "732"},
		new String[]{"Yemen", "YE", "YEM", "887"},
		new String[]{"Zambia", "ZM", "ZMB", "894"},
		new String[]{"Zimbabwe", "ZW", "ZWE", "716"}
	};
	
	private static final Country []countryRecs;
	
	static{
		countryRecs = new Country[countries.length];
		int i =0;
		for(String[] arr : countries){
			Country country = new Country();
			country.name = arr[0];
			country.twoDigitCode = arr[1];
			country.threeDigitCode = arr[2];
			country.number = arr[3];
			countryRecs[i++] = country;
		}
	}
	
	public static Country findBy2DigitCode(String code){
		code = Strings.std(code);
		for(Country country : countryRecs){
			if(Strings.std(country.twoDigitCode).equals(code)){
				return country;
			}
		}
		return null;
	}
	
	public static Iterable<Country> countries(){
		return Arrays.asList(countryRecs);
	}
}

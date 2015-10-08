package com.opendoorlogistics.core.utils.strings;

/**
 * Search for the enum value using a standardised string lookup
 * @author Phil
 *
 */
public class EnumStdLookup<T extends Enum<?>> {
	private final StandardisedStringTreeMap<T> enums = new StandardisedStringTreeMap<T>(false);
	
	@SuppressWarnings("unchecked")
	public EnumStdLookup(Class<T> enumClass){
		for(Enum<?> val : enumClass.getEnumConstants()){
			enums.put(val.name(), (T)val);
		}
	}
	
	public T get(String s){
		return enums.get(s);
	}
}

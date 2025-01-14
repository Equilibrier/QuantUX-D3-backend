package com.qux.util;

public class DB {
	
	private static String prefix;
	
	public static String getTable(Class<?> cls){
		if(prefix==null){
			return cls.getSimpleName().toLowerCase();
		} else {
			return DB.prefix + "_"+ cls.getSimpleName().toLowerCase();
		}
	}
	
	public static void setPrefix(String prefix){
		DB.prefix = prefix;
	}
	
	public static String getAppTable() {
		return DB.prefix + "_" + "app";
	}
}

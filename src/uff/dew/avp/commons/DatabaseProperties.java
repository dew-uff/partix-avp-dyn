package uff.dew.avp.commons;

import java.io.Serializable;
import java.util.Properties;

public class DatabaseProperties implements Serializable {

	private Properties properties = new Properties();
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public void addProperties(String key, String value) {
		if(properties.containsKey(key))
			System.out.println("Table already in catalog");
		properties.put(key,value);
	}

	public boolean remove(String arg0) {
		if(properties.containsKey(arg0)) {
			properties.remove(arg0);
			return true;
		}
			return false;		
	}

	public boolean contains(String arg0) {
		return properties.contains(arg0);
	}

	public boolean containsKey(String arg0) {
		return properties.containsKey(arg0);
	}

	public Object get(String arg0) {
		return properties.get(arg0);
	}
	
}

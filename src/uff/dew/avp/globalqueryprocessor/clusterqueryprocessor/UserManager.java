package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor;

import java.util.HashMap;

public class UserManager {
	private HashMap<String,String> users = new HashMap<String,String>();

	public void put(String user, String password) {
		users.put(user,password);
	}
	
	public void verify(String user, String password) {
		String pwd = users.get(user);
		if(!(pwd != null && pwd.equals(password))) 
			System.out.println("User not exist or password is incorrect!");
	}

}

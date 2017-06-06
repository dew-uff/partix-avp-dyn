package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface ConnectionManager extends Remote {
	public static String OBJECT_NAME = "ClusterQueryProcessor";
	public static int DEFAULT_PORT = 8050;
	public int createConnection(String user, String password) throws RemoteException;
			
}

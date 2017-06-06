package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor;

import java.rmi.RemoteException;

import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.ClusterQueryProcessorEngine;
import uff.dew.avp.loadbalancer.LprfLoadBalancer;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.QueryScheduler;


/**
 * @author Bernardo
 */
public class ServerConnectionImpl {
	/**
	 * 
	 */
	private static final long serialVersionUID = 3258408439326781744L;
	public static final String ADD_VP = "ADD VP";
	public static final String DROP_VP = "DROP VP";
	public static final String GET_VP_LIST = "GET VP LIST";
	public static final String GET_NODE_LIST = "GET NODE LIST";
	public static final String DROP_NODE = "DROP NODE";
	public static final String ADD_NODE = "ADD NODE";
	
	private Logger logger = Logger.getLogger(ServerConnectionImpl.class);
	
	private ConnectionManagerImpl connectionManager;
	private QueryScheduler queryScheduler;
	private ClusterQueryProcessorEngine clusterQueryProcessorEngine;
	private LprfLoadBalancer loadBalancer;
	private boolean autoCommit = false;

    /**
     * 
     */
    public ServerConnectionImpl(ConnectionManagerImpl connectionManager, ClusterQueryProcessorEngine clusterQueryProcessorEngine, QueryScheduler queryScheduler, LprfLoadBalancer loadBalancer) throws RemoteException {
		this.connectionManager = connectionManager;
    	this.clusterQueryProcessorEngine = clusterQueryProcessorEngine;
    	this.queryScheduler = queryScheduler;
		this.loadBalancer = loadBalancer;		
		logger.info(Messages.getString("serverconnection.newconnection"));
    }
	
//	public XQueryResult executeQuery(String query) throws RemoteException, XQException, SQLException {
//		if(query.startsWith(GET_VP_LIST))
//			return connectionManager.listVirtualPartitionedTable();
//		else if(query.startsWith(GET_NODE_LIST))
//			return connectionManager.getNodesList();
//			
////		if(query.toUpperCase().trim().equals("SELECT DUMP"))
////			return queryScheduler.dump();
//		XQueryResult rs = null;	
//		
//		SelectQueryManager qm = new SelectQueryManager(query,connectionManager.getMetaData(),queryScheduler.getNextQueryNumber(),clusterQueryProcessorEngine,loadBalancer); 		
//		rs = queryScheduler.executeQuery(qm);				
//	
//		return rs;
//	}
	
	public void close() throws RemoteException {
		if(connectionManager != null)
			connectionManager.notifyClosedConnection(this);			
		connectionManager = null;		
		queryScheduler = null;
		clusterQueryProcessorEngine = null;
		loadBalancer = null;
		//UnicastRemoteObject.unexportObject(this,true);		
	}

	public boolean getAutoCommit() throws RemoteException {
		return autoCommit;
	}
}

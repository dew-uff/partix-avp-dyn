package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Random;

import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.ClusterQueryProcessorEngine;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.ConnectionManager;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.QueryScheduler;
import uff.dew.avp.loadbalancer.LprfLoadBalancer;
import uff.dew.avp.commons.DatabaseProperties;
import uff.dew.avp.commons.MyRMIRegistry;
import uff.dew.avp.commons.Configurator;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.commons.Logger;

/**
 * @author Bernardo
 */
public class ConnectionManagerImpl implements ConnectionManager {
	private static final long serialVersionUID = 4051324561100124982L;
	private Logger logger = Logger.getLogger(ConnectionManagerImpl.class);
	private ClusterQueryProcessorEngine clusterQueryProcessorEngine;
	private QueryScheduler queryScheduler;
	private LprfLoadBalancer loadBalancer;
	private int port;	
	private HashMap<Integer,ServerConnectionImpl> opennedConnections = new HashMap<Integer,ServerConnectionImpl>();
	private UserManager userManager;
	private DatabaseProperties databaseProperties;
	private DatabaseProperties sortProperties;
	private Random keyGen = new Random(System.currentTimeMillis());
	
    public ConnectionManagerImpl(int port, String configFileName) throws RemoteException {
    	try {
    		this.clusterQueryProcessorEngine = new ClusterQueryProcessorEngine();
    		Configurator configurator = new Configurator(configFileName,this);
	    	logger.info(Messages.getString("connectionManagerImpl.init"));
	    	loadBalancer = new LprfLoadBalancer(0);	    	
	     	configurator.config();
	    	userManager = configurator.getUserManager();
	    	databaseProperties = configurator.getDatabaseProperties();
            sortProperties = configurator.getSortProperties();
	    	
	    	logger.info(Messages.getString("connectionManagerImpl.step1"));
    		//RMISocketFactory
			this.port = port;
    		queryScheduler = new QueryScheduler(this);
    		logger.info(Messages.getString("connectionManagerImpl.step2"));
			logger.info(Messages.getString("connectionManagerImpl.step3"));

			logger.info(Messages.getString("connectionManagerImpl.step4"));
    		register();
    		logger.info(Messages.getString("connectionManagerImpl.partixvpReady"));	
        } catch (Exception e) {
			e.printStackTrace();
        } 
    }
    /* (non-Javadoc)
     * @see org.pargres.cqp.ConnectionManager#createConnection()
     */    
    public int createConnection(String user, String password) throws RemoteException {
    	userManager.verify(user,password);
    	ServerConnectionImpl serverConnection = new ServerConnectionImpl(this,clusterQueryProcessorEngine,queryScheduler,loadBalancer);
    	Integer key = new Integer(keyGen.nextInt());    	
    	opennedConnections.put(key,serverConnection);    	    	
    	return key;
    }
    
    public void notifyClosedConnection(ServerConnectionImpl serverConnection) {
    	opennedConnections.remove(serverConnection);
    }

    private void register() {
        try {
			//A comunicação via RMI é quase tão rápida quanto via socket
			//http://martin.nobilitas.com/java/thruput.html			
	    	MyRMIRegistry.bind(port,getRmiAddress(),this);
			logger.info(Messages.getString("connectionManagerImpl.register",new Object[]{port,getRmiAddress()}));
        } catch (Exception e) {
			e.printStackTrace();
        } 
	}
	
	private String getRmiAddress() {
		return "rmi://localhost:"+port+"/"+ConnectionManager.OBJECT_NAME;
	}
	
    private void unregister() {
        try {
			MyRMIRegistry.unbind(port,getRmiAddress(),this);
        } catch (Exception e) {
			e.printStackTrace();
        }
    }	
	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	protected void finalize() throws Throwable {
		logger.debug(Messages.getString("connectionManagerImpl.finalize"));
	}
	
	public void destroy() {
		queryScheduler.shutdown();
		try {
			clusterQueryProcessorEngine.shutdown();
			clusterQueryProcessorEngine = null;			
		} catch (Throwable e) {
			e.printStackTrace();
		}
		Object[] array = opennedConnections.values().toArray();
		for(int i = 0; i < array.length; i++) {
			try {
				((ServerConnectionImpl)array[i]).close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		unregister();
	}
	
    public static void main(String[] args) {
        int portNumber;
        String configFileName;

        if (args.length < 2) {
            System.out.println("usage: java ConnectionManagerImpl " + "query_processor_port_number ConfigFileName");
            return;
        }

        portNumber = Integer.parseInt(args[0]);
        configFileName = args[1].trim();

        try {
        	new ConnectionManagerImpl(portNumber,configFileName);
        } catch (Exception e) {
            System.err.println(e);
            e.printStackTrace();
        }
    }

	public int getNodesList() throws RemoteException {
		return clusterQueryProcessorEngine.getNodesList();
	}
	
	public void addNode(String host, int port) throws RemoteException {
		clusterQueryProcessorEngine.addNode(host,port);
		loadBalancer.addNode();
	}
	public void dropNode(int nodeId) throws RemoteException {
		clusterQueryProcessorEngine.dropNode(nodeId);		
		loadBalancer.dropNode(nodeId);
	}	
}

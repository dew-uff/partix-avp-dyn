package uff.dew.avp.localqueryprocessor.nodequeryprocessor;

/**
 * Componente  a ser executado em cada nó de processamento, servindo de interface entre o processamento global (coordenador) e o processamento local (host).
 * Tem como entrada (método main) as informações de conexão ao DBMSX local. Em seu construtor registra o objeto remoto (RMI bind) a ser utilizado na comunicação
 * entre o nó local e coordenador e recebe (métodos newGlobalQueryTask e newLocalQueryTask) a subconsulta gerada pelo Processador de Consultas Global (coordenador)
 * e encaminhada pela classe globalqueryprocessor.globalquerytask.GlobalQueryTaskEngine, além de outros parâmetros relacionados ao particionamento virtual.
 * 
 */

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.connection.DBConnectionPoolEngine;
import uff.dew.avp.globalqueryprocessor.globalquerytask.GlobalQueryTask;
import uff.dew.avp.commons.MyRMIRegistry;
import uff.dew.avp.commons.SystemResourceStatistics;
import uff.dew.avp.commons.SystemResourcesMonitor;
import uff.dew.avp.commons.Utilities;
import uff.dew.avp.localqueryprocessor.localquerytask.LocalQueryTask;
import uff.dew.avp.localqueryprocessor.localquerytask.LocalQueryTaskEngine;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.Range;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.RangeStatistics;
import uff.dew.svp.db.DatabaseException;

public class NodeQueryProcessorEngine implements NodeQueryProcessor {

	private boolean started = false;
	private Logger logger = Logger.getLogger(NodeQueryProcessorEngine.class);
	private String objectName;
	private DBConnectionPoolEngine localDbPool;
	private SystemResourcesMonitor monitor;
	private int nodeId;
	private int port;

	/** adapted by Luiz Matos 
	 * @throws DatabaseException */
	public NodeQueryProcessorEngine(String hostName, int port, String databaseName, String dbLogin, String dbPassword, int dbmsX)
			throws RemoteException, DatabaseException {
		super();
		this.port = port;
		this.localDbPool = new DBConnectionPoolEngine(hostName, port, dbLogin, dbPassword, databaseName, dbmsX, 1);
		this.monitor = null;
		this.started = true;
		//setNodeId(); //PARA TESTE LOCAL
		objectName = "rmi://" + hostName + ":" + port + "/NodeQueryProcessor";
		try {
			MyRMIRegistry.bind(port, objectName, this);
			MyRMIRegistry.lookup(hostName,port,objectName);

		} catch (Exception e) {
			logger.error("NodeQueryProcessorEngine Exception: "	+ e.getMessage());
			e.printStackTrace();
		}
		logger.info(Messages.getString("nodeQueryProcessorEngine.running",new Object[]{port,objectName}));		
		//System.out.println("this.getNodeId(): " + this.getNodeId());
		
		//grava IP:porta no arquivo CQP.conf que sera utilizado pelo CQP
		Utilities.setNQPFileConf(AVPConst.NQP_CONF_FILE_NAME, hostName+":"+port);
		
	}

	protected void finalize() throws Throwable {
		turnSystemMonitorOff();
		((DBConnectionPoolEngine) localDbPool).shutdown();
		System.gc();
		System.exit(0);
	}

	public static void main(String[] args) {
		int portNumber, dbmsX;
		String hostName, databaseName, dbLogin, dbPwd;
		//boolean quotedDateIntervals;

		if (args.length != 6) {
			System.out.println("usage: java NodeQueryProcessorEngine <hostName_or_IP> <port_number> <databaseName> <dbLogin> <dbPassword><dbmsX_used>");
			System.out.println("e.g.: java NodeQueryProcessorEngine localhost 3050 basex SYSTEM MANAGER 2 - being <dbmsX_used> = 2 = BaseX");
			return;
		}
		hostName = args[0];
		portNumber = (new Integer(args[1])).intValue();
		databaseName = args[2];
		dbLogin = args[3];
		dbPwd = args[4];
		dbmsX = (new Integer(args[5])).intValue();

		try {
			new NodeQueryProcessorEngine(hostName, portNumber, databaseName, dbLogin, dbPwd, dbmsX);
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
		}
	}

	public LocalQueryTask newLocalQueryTask(int id, GlobalQueryTask globalTask,	String query, int numIntervalsLocalTask, int queryExecutionStrategy,
			int numlqts, boolean performDynamicLoadBalancing, boolean onlyCollectionStrategy, boolean getStatistics, Range range, RangeStatistics statistics, String tempCollectionName, int idQuery) throws RemoteException {
		
		LocalQueryTask localTask = new LocalQueryTaskEngine(id, globalTask,	getDBConnectionPool(), query, queryExecutionStrategy, numlqts,
				performDynamicLoadBalancing, onlyCollectionStrategy, getStatistics, range, statistics, tempCollectionName, idQuery);

		return localTask;
	}

	//TESTAR COM SYNCHRONIZED
	public synchronized void shutdown() throws RemoteException, NotBoundException, MalformedURLException {
		logger.debug(Messages.getString("nodeQueryProcessorEngine.unbinding"));
		MyRMIRegistry.unbind(port,objectName,this);
		try {			
			finalize();
		} catch (Throwable t) {
			t.printStackTrace();
			logger.error(t);
		}
	}

	public DBConnectionPoolEngine getDBConnectionPool() throws RemoteException {
		return localDbPool;
	}

	public void turnSystemMonitorOn() throws IOException, RemoteException {
		if (monitor == null) {
			monitor = new SystemResourcesMonitor();
			monitor.start();
		}
	}

	public void turnSystemMonitorOff() throws RemoteException {
		if (monitor != null) {
			monitor.finish();
			monitor = null;
		}
	}

	public void resetSystemMonitorCounters() throws RemoteException {
		if (monitor != null)
			monitor.resetCounters();
	}

	public SystemResourceStatistics getSystemResourceStatistics()
			throws RemoteException {
		if (monitor != null)
			return monitor.getStatistics();
		else
			throw new RuntimeException("NodeQueryProcessorEngine Exception: monitor was off but the number of misses was demanded");
	}

	public boolean isStarted() {
		return started;
	}
	
	private void setNodeId() {
		this.nodeId = 1;
	}

	public int getNodeId() throws RemoteException {
		//return this.nodeId; //PARA TESTE LOCAL
		return this.hashCode();
	}
}

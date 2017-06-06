package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor;

/**
 * 
 * @author lima
 * @author lzomatos
 * 
 */

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;

import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.commons.MyRMIRegistry;
import uff.dew.avp.commons.Node;
import uff.dew.avp.commons.SystemResourceStatistics;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.querymanager.UpdateLogger;
import uff.dew.avp.localqueryprocessor.nodequeryprocessor.NodeQueryProcessor;
import uff.dew.avp.localqueryprocessor.nodequeryprocessor.NqpAllocator;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.Range;


public class ClusterQueryProcessorEngine /* implements ClusterQueryProcessor */{

	private static final long serialVersionUID = 1L;
	private Logger logger = Logger.getLogger(ClusterQueryProcessorEngine.class);
	private final int STATE_NOT_STARTED = 0;
	private final int STATE_STARTED = 1;
	private int state; // indicates if the object has already been STARTED or NOT.
	private ArrayList<Node> nodes = new ArrayList<Node>();
	private String objectName;
	private UpdateLogger updateLogger = new UpdateLogger();
	private NqpAllocator nqpAllocator = new NqpAllocator();

	public boolean isStarted() {
		return state == STATE_STARTED;
	}

	/** Creates a new instance of ClusterQueryProcessorEngine */
	public ClusterQueryProcessorEngine() throws RemoteException {
		// this.port = portNumber;
		// shutdownRequested = false;
		// this.objectName = "//localhost:" + portNumber
		// + "/ClusterQueryProcessor";
		state = STATE_NOT_STARTED;

		try {
			// MyRMIRegistry.bind(portNumber, objectName, this);
			//this.start(configFileName);
			state = STATE_STARTED;
		} catch (Exception e) {
			logger.error("ClusterQueryProcessorEngine Exception: "	+ e.getMessage());
			e.printStackTrace();
		}
	}

	public String getObjectName() throws RemoteException {
		return objectName;
	}

	public void addNode(String nodeName, int portNumber) throws RemoteException {
		try {
			NodeQueryProcessor nqp = null;
			final int MAX_NQP_BINDING_ATTEMPTS = 5;
			final int SLEEP_TIME = 500; // milliseconds
			int nqp_binding_attempts = 1;
			boolean nqpBound = false;

			while (!nqpBound) {
				try {
					logger.debug(Messages.getString("clusterQueryProcessorEngine.addingNode",new Object[]{nodeName,portNumber,nqp_binding_attempts}));
					nqp = (NodeQueryProcessor) MyRMIRegistry.lookup(nodeName,
							portNumber, "//" + nodeName + ":" + portNumber
							+ "/NodeQueryProcessor");
					nqpBound = true;
					logger.info(Messages.getString("clusterQueryProcessorEngine.nodeConnected",new Object[]{nodeName,portNumber}));

				} catch (NotBoundException e) {
					if (nqp_binding_attempts == MAX_NQP_BINDING_ATTEMPTS)
						throw new NotBoundException("Binding to //" + nodeName
								+ ":" + portNumber
								+ "/NodeQueryProcessor not possible after "
								+ MAX_NQP_BINDING_ATTEMPTS + " attempts.");
					else {
						Thread.sleep(SLEEP_TIME);
						nqp_binding_attempts++;
					}
				}
			}
			NodeQueryProcessor[] nqpArray = new NodeQueryProcessor[1];
			nqpArray[0] = nqp;
			nodes.add(new Node(nodeName + ":" + portNumber, nqpArray));
			updateLogger.logAddNode(nodeName + ":" + portNumber);
			nqpAllocator.addNode(nqp);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	@SuppressWarnings("finally")
	private String executeQuery(String query, Range range, int numNQPs,
			int[] partitionSizes, boolean getStatistics,
			boolean localResultComposition, 
			int queryExecutionStrategy, boolean performDynamicLoadBalancing,
			boolean getSystemResourceStatistics) {
		String result = null;
		try {
			int allocatedNQPs;

			if (state != STATE_STARTED)
				throw new IllegalStateException("ClusterQueryProcessor not Started!");

			if (numNQPs > getClusterSize())
				throw new IllegalArgumentException(numNQPs
						+ " NodeQueryProcessors were requested but only "
						+ getClusterSize() + " are available");

			if (range.getNumVPs() < numNQPs)
				throw new IllegalArgumentException(
						"As "
								+ numNQPs
								+ " NodeQueryProcessors will be used, there must be at least "
								+ numNQPs + " intervals");

			ArrayList<NodeQueryProcessor> nqpsUsed = new ArrayList<NodeQueryProcessor>();

			int leastLoadedNode = -1;
			long minLoad = Long.MAX_VALUE;
			allocatedNQPs = 0;

			for (int i = 0; (i < nodes.size()) && (allocatedNQPs < numNQPs); i++) {
				for (int j = 0; (j < nodes.get(i).getNumNQPs())	&& (allocatedNQPs < numNQPs); j++) {
					nqpsUsed.add(nodes.get(i).getNQP(j));
					allocatedNQPs++;

					if(nodes.get(i).getLoad() < minLoad) {
						leastLoadedNode = i;
						minLoad = nodes.get(i).getLoad();
					}

				}
			}
			logger.debug(Messages.getString("clusterQueryProcessorEngine.creatingGlobaQueryTask"));

			Node node = nodes.get(leastLoadedNode);
			NodeQueryProcessor nqp = node.getNQP(0);

			node.incLoad();

			//Cria uma instância GQT para processar a consulta
//			GlobalQueryTask globalTask = new GlobalQueryTaskEngine(nqp, 
//					nqpsUsed, query, range, getStatistics,
//					localResultComposition, queryExecutionStrategy,
//					performDynamicLoadBalancing, partitionSizes,
//					getSystemResourceStatistics, qi);

			//globalTask.start();

			node.decLoad();



		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			return result;
		}
		//IllegalStateException, RemoteException,
		//InterruptedException, IllegalArgumentException, Exception			
	}

	public synchronized void shutdown() throws RemoteException,
	NotBoundException, MalformedURLException {
		logger.info(Messages.getString("clusterQueryProcessorEngine.shutdown"));
		if (state == STATE_STARTED) {
			logger.info(Messages.getString("clusterQueryProcessorEngine.disconnecting"));
			int nodeSize = nodes.size();
			for (int i = 0; i < nodeSize; i++)
				dropNode(0);
		}
		//System.out.println("Unbinding...");

		//MyRMIRegistry.unbind(port, objectName, this);

		logger.info(Messages.getString("clusterQueryProcessorEngine.exit"));
		//shutdownRequested = true;
		notifyAll();
	}

//	public boolean quotedDateIntervals() throws RemoteException {
//		return nodes.get(0).getNQP(0).quotedDateIntervals();
//	}

	public ArrayList<NodeQueryProcessor> getNodeQueryProcessorList() {
		ArrayList<NodeQueryProcessor> list = new ArrayList<NodeQueryProcessor>();
		for (Node node : nodes)
			list.add(node.getNQP(0));
		return list;
	}

	public SystemResourceStatistics[] getGlobalSystemResourceStatistics()
			throws RemoteException {
		SystemResourceStatistics[] statistics = new SystemResourceStatistics[getClusterSize()];

		for (int nodenum = 0; nodenum < nodes.size(); nodenum++) {
			if (nodes.get(nodenum).getNumNQPs() > 0) {
				statistics[nodenum] = nodes.get(nodenum).getNQP(0)
						.getSystemResourceStatistics();
			}
		}
		return statistics;
	}

	public synchronized void setClusterSize(int size) throws RemoteException {
		throw new RuntimeException(
				"ClusterQueryProcessor Exception: function setClusterSize() is not implemented!");
	}

	public int getClusterSize() throws RemoteException {
		return nodes.size();
	}

	public NodeQueryProcessor getNQP(int i) throws RemoteException {
		return nodes.get(i).getNQP(0);
	}

	public void dropNodeByNodeId(int nodeId) throws RemoteException {
		// Generics were not used to possible modification in collection
		for (int i = 0; i < nodes.size(); i++) {
			Node node = nodes.get(0);
			if (node.getNQP(0).getNodeId() == nodeId) {
				nodes.remove(node);
				updateLogger.logDropNode(node.getAddress());
				return;
			}
		}
	}

	public void dropNode(int nodeId) throws RemoteException {
		//TODO: Refazer isso		
		Node node = nodes.remove(nodeId);
		updateLogger.logDropNode(node.getAddress());
		try {
			// node.getNQP(0).shutdown();
		} catch (Exception e) {
			logger.error(e);
			e.printStackTrace();
		}
	}

	public int getNodesList() throws RemoteException {

		int i = 0;
		for (Node node : nodes) {        	
			String item = i + ":" + node.getAddress().toString();
			if(i != nodes.size() - 1)
				item += ";";

			i++;

		}
		return i;
	}

	public String executeQueryWithAVP(String query, Range range, int numNQPs,
			int[] partitionSizes, boolean getStatistics,
			boolean localResultComposition,
			boolean performDynamicLoadBalancing,
			boolean getSystemResourceStatistics) {
		// TODO Auto-generated method stub
		return null;
	}

	public static long getSerialversionuid() {
		return serialVersionUID;
	}
}

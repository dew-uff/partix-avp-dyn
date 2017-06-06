package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.xquery.XQException;

import uff.dew.avp.commons.SystemResourceStatistics;
import uff.dew.avp.localqueryprocessor.nodequeryprocessor.NodeQueryProcessor;
import uff.dew.avp.globalqueryprocessor.globalquerytask.GlobalQueryTask;
import uff.dew.svp.FinalResultComposer;

public interface ClusterQueryProcessor extends Remote {
	public String getObjectName() throws RemoteException;

	public void start(String configFileName, boolean onlyCollectionStrategy) throws InterruptedException,
	IllegalArgumentException, FileNotFoundException, IOException,
	IndexOutOfBoundsException, NotBoundException,
	MalformedURLException, RemoteException, XQException;

	public void executeQueryOnCluster(int idQuery, String xquery, String vpStrategy, int numNQPs, boolean performDynamicLoadBalancing)
			throws IllegalStateException, RemoteException, InterruptedException, IllegalArgumentException, Exception;

	public SystemResourceStatistics[] getGlobalSystemResourceStatistics() throws RemoteException;

	public void shutdown() throws RemoteException, NotBoundException, MalformedURLException;

	public void setClusterSize(int size) throws RemoteException;

	public int getClusterSize() throws RemoteException;
	
	public NodeQueryProcessor getNQP(int i) throws RemoteException;

	public void addNode(String host, int port) throws RemoteException;

	public void dropNode(int nodeId) throws RemoteException;

	public String getNodesList() throws RemoteException;
	
	public GlobalQueryTask newGlobalQueryTask(ClusterQueryProcessor cqp, int cardinality, ArrayList<NodeQueryProcessor> allocatedNQP,
			List<String> sbq, int vpStrategy, boolean performDynamicLoadBalancing, boolean onlyCollectionStrategy, String tempCollectionName, int idQuery) throws RemoteException, IOException;

	public PrintWriter getSaida() throws RemoteException;
	public long getGenerateSubQueriesTime() throws RemoteException;
	public long getPrepareConsolidationTime() throws RemoteException;
	public String getResultsTimeFileName() throws RemoteException;
	public FinalResultComposer getComposer() throws RemoteException;
}

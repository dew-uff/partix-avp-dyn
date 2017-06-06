package uff.dew.avp.localqueryprocessor.nodequeryprocessor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import uff.dew.avp.globalqueryprocessor.globalquerytask.GlobalQueryTask;
import uff.dew.avp.commons.SystemResourceStatistics;
import uff.dew.avp.localqueryprocessor.localquerytask.LocalQueryTask;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.Range;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.RangeStatistics;

public interface NodeQueryProcessor extends Remote {

	public LocalQueryTask newLocalQueryTask(int id, GlobalQueryTask globalTask,	String query, int numIntervalsLocalTask, int queryExecutionStrategy,
			int numlqts, boolean performDynamicLoadBalancing, boolean onlyCollectionStrategy, boolean getStatistics, Range range, RangeStatistics statistics, String tempCollectionName, int idQuery) throws RemoteException;

	public void turnSystemMonitorOn() throws IOException, RemoteException;

	public void turnSystemMonitorOff() throws RemoteException;

	public void resetSystemMonitorCounters() throws RemoteException;

	public SystemResourceStatistics getSystemResourceStatistics() throws RemoteException;

	public void shutdown() throws RemoteException, NotBoundException, MalformedURLException;

	//public int executeUpdate(String query) throws RemoteException, XQException;

	public int getNodeId() throws RemoteException;

}

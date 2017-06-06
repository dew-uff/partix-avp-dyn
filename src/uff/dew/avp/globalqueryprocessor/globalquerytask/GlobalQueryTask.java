package uff.dew.avp.globalqueryprocessor.globalquerytask;

/**
 * 
 * @author lima
 * @author lzomatos
 * 
 */

import java.rmi.Remote;
import java.rmi.RemoteException;

import uff.dew.avp.commons.LocalQueryTaskStatistics;
import uff.dew.avp.commons.SystemResourceStatistics;

import uff.dew.avp.localqueryprocessor.localquerytask.LocalQueryTask;

public interface GlobalQueryTask extends Remote {
    static public final int QE_STRATEGY_FGVP = 0;
    static public final int QE_STRATEGY_AVP = 1;
    static public final int QE_STRATEGY_SVP = 2;
    static public final int QE_STRATEGY_CENTRALIZED = 3;
    
    public void start() throws RemoteException, InterruptedException, Exception;

    public void localIntervalFinished(int localTaskId) throws RemoteException;

    public void localQueryTaskFinished(int localTaskId, LocalQueryTaskStatistics lqtstatistics, Exception exception) throws RemoteException;
    
    public void localQueryTaskFinished(int localTaskId, LocalQueryTaskStatistics statistics,
            SystemResourceStatistics resourceStatistics, Exception exception) throws RemoteException;

    public void getLQTReferences(int[] id, LocalQueryTask[] reference) throws RemoteException;

    public LocalQueryTask getLQTReference(int id) throws RemoteException;
}

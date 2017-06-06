package uff.dew.avp.localqueryprocessor.localquerytask;

/**
 * 
 * @author lima
 */

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.Range;

public interface LocalQueryTask extends Remote {
    public int getId() throws RemoteException;

    // To be called by Global Query Task
   // public void start() throws RemoteException;

    public void start() throws RemoteException;
    
    public void finish() throws RemoteException;

    // To be called by other Local Query Tasks to put messages in the queue
    public void intervalFinished(int idLQT, int intervalBeginning, int intervalEnd) throws RemoteException;

    public void refuseHelp(int idSender, int offerNumber) throws RemoteException;

    /*
     * offerHelp() - used by a neighbor to offer help or to propagate help
     * offers from other nodes. idSender - identifier of the lqt who is offering
     * or propagating the offer; idHelper - identifier of the lqt who wants to
     * help; senderPosition - position of the last sender related the the lqt
     * which is receiving; offerNumber - sequential number of the offer
     * (determined by helper);
     */
    public void offerHelp(int idSender, int idHelper, int senderPosition, int offerNumber) throws RemoteException;

    // Methods that implement work division between LQTs
    // acceptHelp() - called by who is accepting help
    // deprecated public boolean acceptHelp( int idlqtHelped, LocalQueryTask
    // lqtHelped ) throws RemoteException;
    // acceptHelp() - called by who is accepting help. Puts an acceptance
    // message in the helper queue
    public void acceptHelp(int idSender, int offerNumber) throws RemoteException;

    // getPartOfRange() - called by who is giving help
    public Range getPartOfRange() throws RemoteException;

    // To be called by Query Executor
    public void rangeProcessed() throws RemoteException, InterruptedException;

    public ArrayList<String> getCompletePartialResultFileNames() throws RemoteException;
    
	public void setCompletePartialResultFileNames(String completePartialResultFileName) throws RemoteException;
	
	public long getTimeComposePartialResultFiles() throws RemoteException;
	
	public int getQueryExecutionStrategy() throws RemoteException;
	
	public void setTimeComposePartialResultFiles(long completePartialResultFileName) throws RemoteException;

}

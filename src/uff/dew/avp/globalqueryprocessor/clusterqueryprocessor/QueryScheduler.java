package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.ConnectionManagerImpl;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.querymanager.AbstractQueryManager;

/**
 * @author Bernardo
 */
public class QueryScheduler implements Runnable {
	private Logger logger = Logger.getLogger(QueryScheduler.class);
	private AtomicInteger queryCounter = new AtomicInteger(0);
	private ConcurrentLinkedQueue<AbstractQueryManager> queue = new ConcurrentLinkedQueue<AbstractQueryManager>();
	private AtomicBoolean executingUpdate = new AtomicBoolean();
	private Set<AbstractQueryManager> executingQueries = new HashSet<AbstractQueryManager>();
	private boolean requestShutdown = false;
	private ConnectionManagerImpl connectionManager;
	private LongTransactionQueue longTransactionQueue = new LongTransactionQueue();
	private Thread thread;

	public QueryScheduler(ConnectionManagerImpl connectionManager) {
		this.connectionManager = connectionManager;		
		executingUpdate.set(false);		
		thread = new Thread(this);
		thread.start();
	}

	public void shutdown() {
		requestShutdown = true;

		try {
			while(thread.isAlive()) {			
				synchronized (this) {
					notifyAll();
				}		
				thread.join(200);				
			}
		} catch (InterruptedException e) {
			System.err.println(e.getMessage());
			e.printStackTrace();
		}
	}

	private int pendingQueries() {
		return executingQueries.size();
	}

	//** CONCURRENT METHODS **/

	public int getNextQueryNumber() {
		synchronized (queryCounter) {
			int i = queryCounter.intValue();
			queryCounter.set(i+1);
			return i;
		}
	}	

	public void put(AbstractQueryManager s) {		
		s.setScheduller(this);				

		longTransactionQueue.wait(s);
		synchronized (this) {
			queue.add(s);
			logger.debug(Messages.getString("queryScheduler.added",s.getQueryNumber()));
			notifyAll();	
		}		
	}

	public synchronized void remove(AbstractQueryManager s) {		
		executingQueries.remove(s);
		logger.debug(Messages.getString("queryScheduler.remove",s.getQueryNumber()));

		notifyAll();
	}
	
	public void run() {
		while(!requestShutdown) {
			try {
				logger.debug(Messages.getString("queryScheduler.running"));
				synchronized (this) {					
					while(queue.size() != 0) 
						//executeNextInQueue();

					logger.debug(Messages.getString("queryScheduler.waitingForMore"));
					wait();
				}
			} catch (Exception e) { 
				logger.error(e);
				e.printStackTrace();
			}
		}
	}

//	public String executeQuery(AbstractQueryManager s) throws RemoteException {
//		//Isto é thread-safe?
//		put(s);
//		String rs = null;
//
//		rs = ((SelectQueryManager)s).executeQuery();
//		logger.debug(Messages.getString("queryScheduler.queryExecuted",s.getQueryNumber()));			
//
//		remove(s);
//		return rs;		
//	}

	public void beginTransaction() {
		longTransactionQueue.block();		
	}

	public void endTransaction() {
		longTransactionQueue.unblock();		
	}
}

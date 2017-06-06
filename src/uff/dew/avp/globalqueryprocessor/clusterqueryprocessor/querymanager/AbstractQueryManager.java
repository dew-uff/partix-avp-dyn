package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.querymanager;

import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.ClusterQueryProcessorEngine;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.Schedullable;

/**
 * @author Bernardo
 */
public abstract class AbstractQueryManager extends Schedullable {
	private Logger logger = Logger.getLogger(AbstractQueryManager.class);

	private boolean ready = false;

	protected ClusterQueryProcessorEngine clusterQueryProcessorEngine;
	
	protected String sql;
	
	protected int clusterSize = -1;

	public AbstractQueryManager(String sql, int queryNumber,ClusterQueryProcessorEngine clusterQueryProcessorEngine) {
		this.sql = sql;
		try {
			clusterSize = clusterQueryProcessorEngine.getClusterSize();
		} catch (Exception e) {
			logger.error(e);
		}
		if (clusterSize == 0)
			System.out.println("No NQP left");
		
		this.queryNumber = queryNumber;
		this.clusterQueryProcessorEngine = clusterQueryProcessorEngine;
	}

	protected void schedullerWait() {
		while (!ready) {
			try {
				synchronized (this) {
					logger.debug(Messages.getString("querymanager.waiting",	queryNumber));
					wait();
				}
			} catch (Exception e) {
				e.printStackTrace();
				logger.error(e);
			}
		}
	}

	/**
	 * @return
	 */
/*	public abstract int executeUpdate() throws SQLException, RemoteException;

	public abstract ResultSet executeInterQuery() throws SQLException, RemoteException;
	
	public abstract ResultSet executeIntraQuery() throws SQLException, RemoteException;
	*/

	public void go() {
		ready = true;
		logger.debug(Messages.getString("querymanager.readyToGo", queryNumber));
		synchronized (this) {
			notifyAll();
		}
	}

	public String getSql() {
		return sql;
	}

}

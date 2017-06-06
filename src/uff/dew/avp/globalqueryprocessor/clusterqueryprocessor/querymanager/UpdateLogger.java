package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.querymanager;

import uff.dew.avp.commons.Logger;

public class UpdateLogger {
	private Logger logger = Logger.getLogger(UpdateLogger.class);

	public void logAddNode(String nodeName) {
		logger.info("Node Query Processor on "+nodeName+" was added");
	}
	public void logDropNode(String nodeName) {
		logger.info("Node Query Processor on "+nodeName+" was dropped");
	}
	
	public void logUpdate(String sql) {
		sql = sql.replaceAll("\n"," ");
		sql = sql.trim();
		logger.info(sql);
	}

}

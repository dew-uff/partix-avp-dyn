package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.querymanager;

import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;

import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.ClusterQueryProcessorEngine;
import uff.dew.avp.loadbalancer.LprfLoadBalancer;

/**
 * @author Bernardo
 */
public class SelectQueryManager extends AbstractQueryManager {
	private Logger logger = Logger.getLogger(SelectQueryManager.class);

	private LprfLoadBalancer loadBalancer;

	public SelectQueryManager(String query, int queryNumber, ClusterQueryProcessorEngine clusterQueryProcessorEngine,
			LprfLoadBalancer loadBalancer) {
		super(query, queryNumber,clusterQueryProcessorEngine);
		this.loadBalancer = loadBalancer;
		
		logger.debug(Messages.getString("serverconnection.queryCreate",	new String[] { Integer.toString(queryNumber) }));
	}

//	public String executeQuery() throws RemoteException {
//		schedullerWait();
//		String resultSet = null;
//			resultSet = executeIntraQuery();
//		return resultSet;
//	}

//	private String executeIntraQuery() throws RemoteException {
//		String resultSet = null;
//
//		int nodeIndex = loadBalancer.next();
//		loadBalancer.notifyStartIntraQuery(nodeIndex);
//		try {
//			//resultSet = executeIntraQueryDummy();
//			//if(resultSet == null) {
//				globalqueryprocessor.clusterqueryprocessor.queryplanner.QueryAvpDetail queryAvpDetail = queryInfo.getQueryAvpDetail();
//				resultSet = clusterQueryProcessorEngine.executeQueryWithAVP(queryAvpDetail.getQuery(),
//								queryAvpDetail.getRange(), queryAvpDetail
//										.getNumNQPs(), queryAvpDetail
//										.getPartitionSizes(), queryAvpDetail
//										.isGetStatistics(), queryAvpDetail
//										.isLocalResultComposition(),
//								queryAvpDetail.isPerformDynamicLoadBalancing(),
//								queryAvpDetail.isGetSystemResourceStatistics());
//		//	}
//		} catch (Exception e) {
//			logger.error(e);
//			e.printStackTrace();
//			loadBalancer.notifyFinishIntraQuery(nodeIndex);
//		}
//
//		loadBalancer.notifyFinishIntraQuery(nodeIndex);
//		return resultSet;
//	}

}

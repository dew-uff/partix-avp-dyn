package uff.dew.test;

import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.MyRMIRegistry;
import uff.dew.avp.commons.Utilities;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.ClusterQueryProcessor;

/*
 * Esta classe deve ser executada em qualquer máquina (pref. fora do cluster), lendo determinada consulta em disco e submetendo-a ao cluster (CQP).
 * Entradas: endereço+porta do CQP, a quantidade de NQP, estratégia de particionamento, id da consulta e a execução (ou não) de load balance
 * 
 */
public class TestVirtualPartitioning {

	private static Logger logger = Logger.getLogger(TestVirtualPartitioning.class);
	private String cqpAddress;
	private int cqpPort;
	private int numNQPs;
	private String vpStrategy;
	private int idQuery;
	private boolean performDynamicLoadBalancing;
	private String originalQuery = null;

	private FileWriter writer;
	private PrintWriter saida;

	/** Creates a new instance of TestVirtualPartitioning */
	public TestVirtualPartitioning(String cqpAddress, int cqpPort, int numNQPs, String vpStrategy, int idQuery, boolean performDynamicLoadBalancing)
			throws IllegalStateException, IllegalArgumentException, InterruptedException, Exception {
		setCqpAddress(cqpAddress);
		setCqpPort(cqpPort);
		setNumNQPs(numNQPs);
		setVpStrategy(vpStrategy);
		setIdQuery(idQuery);
		setPerformDynamicLoadBalancing(performDynamicLoadBalancing);
		this.run();
	}

	/**
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		if( args.length != 6 ) {
			System.out.println("usage: java TestVirtualPartitioning node_address port numNQPs vpStrategy idQuery performDynamicLoadBalancing" );
			return;
		}
		try {
			String cqpAddress = args[0].trim();
			int cqpPort = Integer.parseInt( args[1] );
			int numNQPs = Integer.parseInt( args[2] );
			String vpStrategy = args[3];
			int idQuery = Integer.parseInt( args[4] );
			boolean performDynamicLoadBalancing = Boolean.parseBoolean(args[5]);
			new TestVirtualPartitioning(cqpAddress, cqpPort, numNQPs, vpStrategy, idQuery, performDynamicLoadBalancing);
		} catch (Exception e) {
			logger.error(e);
		}
	}

	public void run() throws IllegalStateException, IllegalArgumentException, InterruptedException, Exception {
		ClusterQueryProcessor cqp;

		try {
			System.out.println("Starting .....");
			System.out.println("Coordinator (CQP) remote address: " + "rmi://"+ cqpAddress + ":" + cqpPort + "/ClusterQueryProcessor");

			//obtendo referência remota do CQP implementado pela classe CQP_Scheduler_Engine.java
			cqp = (ClusterQueryProcessor) MyRMIRegistry.lookup(cqpAddress,cqpPort,"rmi://"+ cqpAddress + ":" + cqpPort + "/ClusterQueryProcessor");
			System.out.println("Cluster size: " + cqp.getClusterSize() + " nodes");

			//lê a consulta em disco e carrega seu conteúdo
			setOriginalQuery(Utilities.getFileContent(AVPConst.QUERIES_FILE_PATH, "q" + getIdQuery() + ".xq"));
			//System.out.println("Original Query: " + getOriginalQuery());
			System.out.println("VP strategy: " + getVpStrategy());

			//execução da subconsulta de acordo com a estratégia de particionamento virtual informada
			long startTime = System.nanoTime();
			cqp.executeQueryOnCluster(getIdQuery(), getOriginalQuery(), getVpStrategy(), getNumNQPs(), isPerformDynamicLoadBalancing());
			long elapsedTime = System.nanoTime() - startTime;

			System.out.println("General elapsed time = " + elapsedTime/1000000 + "ms");
			System.out.println( "Finished!" );
			System.out.println( "------------------------------------------------------------" );

			writer = new FileWriter(AVPConst.CQP_CONF_DIR_NAME + cqp.getResultsTimeFileName(), true);
			saida = new PrintWriter(writer);
			//saida.print("All-Processing-Time-Execution-Client: ");
			//saida.println(elapsedTime/1000000 + " ms");
			saida.println(elapsedTime/1000000);
			saida.close();
			writer.close();

			//cqp.getSaida().print("All-Processing-Time-Execution-Client: ");
			//cqp.getSaida().println(elapsedTime/1000000 + " ms");

			//MyRMIRegistry.unbind(getCqpPort(), "rmi://"+ cqpAddress + ":" + cqpPort + "/ClusterQueryProcessor", cqp);

			//cqp.shutdown();
			System.gc();
			System.exit(0);

		} catch (Exception e) {
			System.err.println("TestVirtualPartitioning exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public String getCqpAddress() {
		return cqpAddress;
	}

	public void setCqpAddress(String cqpAddress) {
		this.cqpAddress = cqpAddress;
	}

	public int getCqpPort() {
		return cqpPort;
	}

	public void setCqpPort(int cqpPort) {
		this.cqpPort = cqpPort;
	}

	public int getNumNQPs() {
		return this.numNQPs;
	}

	public void setNumNQPs(int numNQPs) {
		this.numNQPs = numNQPs;
	}

	public String getVpStrategy() {
		return vpStrategy;
	}

	public void setVpStrategy(String vpStrategy) {
		this.vpStrategy = vpStrategy;
	}

	public int getIdQuery() {
		return idQuery;
	}

	public void setIdQuery(int idQuery) {
		this.idQuery = idQuery;
	}

	public boolean isPerformDynamicLoadBalancing() {
		return performDynamicLoadBalancing;
	}

	public void setPerformDynamicLoadBalancing(boolean performDynamicLoadBalancing) {
		this.performDynamicLoadBalancing = performDynamicLoadBalancing;
	}

	public static Logger getLogger() {
		return logger;
	}

	public static void setLogger(Logger logger) {
		TestVirtualPartitioning.logger = logger;
	}

	public String getOriginalQuery() {
		return originalQuery;
	}

	public void setOriginalQuery(String originalQuery) {
		this.originalQuery = originalQuery;
	}

}


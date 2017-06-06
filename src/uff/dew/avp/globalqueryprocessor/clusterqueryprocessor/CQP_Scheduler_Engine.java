package uff.dew.avp.globalqueryprocessor.clusterqueryprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.xml.xquery.XQException;

import uff.dew.svp.ExecutionContext;
import uff.dew.svp.FinalResultComposer;
import uff.dew.svp.Partitioner;
import uff.dew.svp.db.DatabaseException;
import uff.dew.svp.exceptions.PartitioningException;
import uff.dew.svp.catalog.Catalog;
import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.MyRMIRegistry;
import uff.dew.avp.commons.Node;
import uff.dew.avp.commons.SystemResourceStatistics;
import uff.dew.avp.commons.Utilities;
import uff.dew.avp.globalqueryprocessor.globalquerytask.GlobalQueryTask;
import uff.dew.avp.globalqueryprocessor.globalquerytask.GlobalQueryTaskEngine;
import uff.dew.avp.localqueryprocessor.nodequeryprocessor.NqpAllocator;
import uff.dew.avp.localqueryprocessor.nodequeryprocessor.NodeQueryProcessor;


public class CQP_Scheduler_Engine extends UnicastRemoteObject implements ClusterQueryProcessor {
	private static final long serialVersionUID = 3258408434998194487L;
	private final boolean debug = false;
	private final int STATE_NOT_STARTED = 0;
	private final int STATE_STARTED = 1;
	private int a_state; // indicates if the object has already been STARTED or NOT.
	private Node[] a_nodes;
	private int a_TotalNumberOfNQPs;
	private static boolean a_shutdown_requested;
	private String a_objectName;
	// for multi-query load balancing
	private NodeQueryProcessor[] a_nqp; // array of nqps
	private NqpAllocator a_nqpAllocator;
	private int a_currentClusterSize; // to make tests easier
	private List<String> sbq; //armazena as subconsultas geradas pelo algoritmo do SVP
	private GlobalQueryTask globalTask; //cria instância do GQT
	private static String cqpAddress;
	private BufferedReader configReader;
	private Catalog catalog;
	private int cardinality = 0;
	private String vpStrategy;
	private String tempCollectionName;

	private String resultsTimeFileName;
	private FileWriter writer;
	private PrintWriter saida;

	//Utilizado para consolidacao
	private static boolean onlyCollectionStrategy;
	private File fs = null;
	private String filename = AVPConst.FINAL_RESULT_DIR + "finalResult.xml"; //receberá os resultados parciais se o forceTempCollectionMode (FinalResulstComposer) for false (padrão)
	private OutputStream resultWriter;
	private FinalResultComposer composer;
	
	private long generateSubQueriesTime = 0;
	private long prepareConsolidationTime = 0;
	
	/** Creates a new instance of CQP_Scheduler_Engine */
	public CQP_Scheduler_Engine(String objectName) throws IOException {
		a_shutdown_requested = false;
		a_nodes = null;
		a_objectName = objectName;
		a_state = STATE_NOT_STARTED;
	}

	public static void main(String[] args) {
		if (args.length < 3) {
			System.out.println("usage: java CQP_Scheduler_Engine CQP_address CQP_port_number onlyCollectionStrategy");
			return;
		}
		cqpAddress = args[0];
		int cqpPort = Integer.parseInt(args[1]);
		onlyCollectionStrategy = Boolean.parseBoolean(args[2].trim());

		//grava IP:porta no arquivo CQP.conf que sera utilizado pelo CQP
		Utilities.setCQPFileConf(AVPConst.CQP_CONF_FILE_NAME, cqpAddress+":"+cqpPort);
		
		String configFileName = AVPConst.NQP_CONF_FILE_NAME;
		try {
			LocateRegistry.createRegistry(cqpPort);
			ClusterQueryProcessor qProc = new CQP_Scheduler_Engine("rmi://" + cqpAddress + ":" + cqpPort + "/ClusterQueryProcessor");
			qProc.start(configFileName, onlyCollectionStrategy);
			Naming.rebind(qProc.getObjectName(), qProc);
			synchronized (qProc) {
				while (!a_shutdown_requested) {
					qProc.wait();
				}
				qProc.notifyAll();
			}

			System.gc();
			System.exit(0);
		} catch (Exception e) {
			System.err.println("CQP_Scheduler_Engine Exception: " + e.getMessage());
			e.printStackTrace();
		}
	}

	//Inicializa o CQP
	public synchronized void start(String configFileName, boolean onlyCollectionStrategy)
			throws InterruptedException, IllegalArgumentException,
			FileNotFoundException, IOException, IndexOutOfBoundsException,
			NotBoundException, MalformedURLException, RemoteException, XQException {
		if (a_state == STATE_STARTED) {
			System.out.println(getClass().getName() + " - start(): Already Started!");
		} else {
			LinkedList<Node> nodeList = new LinkedList<Node>(); // list to store
			// Node objects
			String line;
			int lineCount = 1;
			a_TotalNumberOfNQPs = 0;
			configReader = new BufferedReader(new FileReader(configFileName));
			while ((line = configReader.readLine()) != null) {
				line = line.trim();
				if (line.length() > 0) { // non-empty line
					if (line.charAt(0) != '#') {
						char fieldSeparator = ':';
						int separatorIndex;
						String nodeName;
						Node node;
						LinkedList<NodeQueryProcessor> nqpList; // list to store
						// NQPs from one
						// node
						NodeQueryProcessor[] nqpArray;
						nodeName = null;
						node = null;
						nqpList = null;
						nqpArray = null;
						// non-comment line
						// get nodeName
						separatorIndex = line.indexOf(fieldSeparator);
						if ((separatorIndex <= 0)) {
							// no node address
							throw new IllegalArgumentException("Line " + lineCount + ": node address not informed");
						} else if (separatorIndex == line.length() - 1) {
							// no port number
							throw new IllegalArgumentException("Line " + lineCount + ": no port number");
						} else {
							nodeName = line.substring(0, separatorIndex).trim();
							// getting port numbers and connecting to NQPs
						}
						while ((separatorIndex != -1) && (separatorIndex < line.length() - 1)) {
							int nextSeparatorIndex;
							String portNumber;
							NodeQueryProcessor nqp;
							final int MAX_NQP_BINDING_ATTEMPTS = 5;
							final int SLEEP_TIME = 500; // milliseconds
							int nqp_binding_attempts;
							boolean nqpBound;
							nextSeparatorIndex = line.indexOf(fieldSeparator, separatorIndex + 1);
							if (nextSeparatorIndex == -1) {
								// last line field
								portNumber = line.substring(separatorIndex + 1).trim();
							} else {
								portNumber = line.substring(separatorIndex + 1,	nextSeparatorIndex).trim();
							}
							if (portNumber.length() == 0) {
								throw new IllegalArgumentException("Line " + lineCount + ": empty port number");
							}
							if (nqpList == null) {
								nqpList = new LinkedList<NodeQueryProcessor>();
							}
							nqp_binding_attempts = 1;
							nqpBound = false;
							while (!nqpBound) {
								try {
									System.out.println("Connecting to " + "rmi://" + nodeName + ":" + portNumber + "/NodeQueryProcessor. Attempt number: " + nqp_binding_attempts);
									nqp = (NodeQueryProcessor) MyRMIRegistry.lookup(nodeName, Integer.parseInt(portNumber), "rmi://" + nodeName + ":" + portNumber + "/NodeQueryProcessor");
									nqpList.add(nqp);
									nqpBound = true;
									System.out.println("Connected to " + "rmi://" + nodeName + ":" + portNumber + "/NodeQueryProcessor");
									System.out.println("Node Id: " + nqp.getNodeId());
									System.out.println("");
								} catch (NotBoundException e) {
									if (nqp_binding_attempts == MAX_NQP_BINDING_ATTEMPTS) {
										throw new NotBoundException("Binding to rmi://"	+ nodeName + ":" + portNumber + "/NodeQueryProcessor not possible after " + MAX_NQP_BINDING_ATTEMPTS + " attempts.");
									} else {
										Thread.sleep(SLEEP_TIME);
										nqp_binding_attempts++;
									}
								}
							} //fim do while interno
							separatorIndex = nextSeparatorIndex;
						} //fim do while externo
						nqpArray = new NodeQueryProcessor[nqpList.size()];
						a_TotalNumberOfNQPs += nqpList.size();
						for (int i = 0; i < nqpArray.length; i++) {
							nqpArray[i] = (NodeQueryProcessor) nqpList.removeFirst();
						}
						node = new Node(nodeName, nqpArray);
						nodeList.add(node);
					} //fim do if (line.charAt(0) != '#')
				} // fim do if (line.length() > 0) { // non-empty line
				lineCount++;
			}
			a_nodes = new Node[nodeList.size()];
			a_nqp = new NodeQueryProcessor[a_TotalNumberOfNQPs];
			for (int i = 0, idxnqp = 0; i < a_nodes.length; i++) {
				a_nodes[i] = (Node) nodeList.removeFirst();
				for (int j = 0; j < a_nodes[i].getNumNQPs(); j++, idxnqp++) {
					a_nqp[idxnqp] = a_nodes[i].getNQP(j);
				}
			}
			a_nqpAllocator = new NqpAllocator(a_nqp, 0);
			a_currentClusterSize = a_nqp.length;
			a_state = STATE_STARTED;
			//Obtém referência para manipular o catálogo criado previamente
			catalog = Catalog.get();
			configReader.close();
			
			System.out.println(getClass().getName() + " - start(): Started!");
		}
		notifyAll();
	}

	//Método chamado pela aplicação cliente
	public void executeQueryOnCluster(int idQuery, String xquery, String vpStrategy, int numNQPs, boolean performDynamicLoadBalancing)
			throws IllegalStateException, RemoteException, InterruptedException, IllegalArgumentException, Exception {
		//System.out.println("Coordinator received a query ...");

		this.vpStrategy = vpStrategy;
		
		//Usado no experimento para gravar resultados em disco
		resultsTimeFileName = Utilities.createTimeResultFileNameCQP( idQuery, vpStrategy, numNQPs);
		writer = new FileWriter(AVPConst.CQP_CONF_DIR_NAME+resultsTimeFileName);
		saida = new PrintWriter(writer);
		//saida.println("Cluster size: " + numNQPs);
		//saida.println("VP strategy: " + vpStrategy);
		//saida.println("Query id: " + idQuery);
		//saida.println("Cluster-size;VP-strategy;Query-id");
		//saida.println(numNQPs + ";" + vpStrategy + ";" + idQuery);
		
		//Começa a medir o tempo de execucao total no CQP
		long startTime = System.nanoTime();
		
		//saida.print("Time-Parser-Generating-SubQueries: ");
		sbq = generateSubQueries(xquery); //obtém as subconsultas com base no algoritmo do SVP
		
		//saida.print("Time-Preparing-Consolidation: ");
		prepareConsolidation(onlyCollectionStrategy); //Cria/limpa colecao temporaria para armazenar resultados parciais
		
		long startTime2 = System.nanoTime();
		
		switch(vpStrategy) {
		case "SVP":
			executeQuery(sbq, GlobalQueryTask.QE_STRATEGY_SVP, getCardinality(), numNQPs, performDynamicLoadBalancing, onlyCollectionStrategy, tempCollectionName, idQuery);
			break;
		case "AVP":
			executeQuery(sbq, GlobalQueryTask.QE_STRATEGY_AVP, getCardinality(), numNQPs, performDynamicLoadBalancing, onlyCollectionStrategy, tempCollectionName, idQuery);
			break;
		default:
			System.out.println("Invalid VP strategy: " + vpStrategy);
			break;
		}

		long elapsedTime = System.nanoTime() - startTime;
		long elapsedTime2 = System.nanoTime() - startTime2;
		
		//saida.print("All-Processing-Time-Execution-CQP: ");
		//saida.print(elapsedTime/1000000 + " ms");
		saida.print(elapsedTime2/1000000 + ";");//tempo total de processamento medido pelo CQP - tirando o tempo do parser e da geracao das subconsultas
		
		saida.print(elapsedTime/1000000 + ";");//tempo total de processamento medido pelo CQP - incluindo o tempo do parser e da geracao das subconsultas
		
		//saida.println();
		saida.close();
		writer.close();

		//System.out.println(a_nqp.length);
//		for(NodeQueryProcessor NQP : a_nqp) {
//			System.out.println(NQP.getNodeId());
//			NQP.shutdown();
//		}
//		this.shutdown();
	}

	private void prepareConsolidation(boolean onlyCollectionStrategy) throws XQException {
		//Prepara ambiente para consolidacao
		try {
			//Nome unico para ter colecoes diferentes para cada execucao
			tempCollectionName = AVPConst.TEMP_COLLECTION_NAME+"_"+resultsTimeFileName.substring(resultsTimeFileName.indexOf(vpStrategy), resultsTimeFileName.indexOf(".txt"));
			
			resultWriter = new FileOutputStream(fs = new File(filename));
			composer = new FinalResultComposer(resultWriter); //cria instância passando arquivo p/ incluir resultado final

			composer.setDatabaseInfo(AVPConst.DB_CONF_LOCALHOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME, 
					AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE); // só usado se for incluir em coleção temporária

			long startTime = System.nanoTime();

			//Verifica a estrategia de consolidacao e se a colecao temporaria nao existir a cria agora
			if(onlyCollectionStrategy) {
				composer.setForceOnlyTempCollectionExecutionMode(true); 
				composer.setExecutionContext(ExecutionContext.restoreFromStream(new FileInputStream(AVPConst.SUBQUERIES_FILE_PATH+AVPConst.SUBQUERIES_FILE_NAME + "_0.txt")));
				//composer.loadPartial(AVPConst.TEMP_COLLECTION_NAME); //Utilizado no caso do nome da colecao temporaria ser fixo
				composer.loadPartial(tempCollectionName);//Utilizado no caso de ser uma colecao temporaria diferente para cada execucao
				composer.setTempCollectionName(tempCollectionName);//idem anterior
			}
			
			long elapsedTime = System.nanoTime() - startTime;
			
			//saida.println(elapsedTime/1000000 + " ms");
			prepareConsolidationTime = elapsedTime/1000000;
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (DatabaseException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

	//Gera subconsultas com base no algoritmo do SVP
	private List<String> generateSubQueries(String xquery) {
		//System.out.println("Generating subqueries ...");

		String subqueriesPath = AVPConst.SUBQUERIES_FILE_PATH + AVPConst.SUBQUERIES_FILE_NAME;
		String catalogPath = AVPConst.CATALOG_FILE_PATH+AVPConst.TPCH_CATALOG_FILE_NAME;
		String contextPath = AVPConst.CONTEXT_FILE_PATH+AVPConst.CONTEXT_FILE_NAME;

		Partitioner partitioner = null;
		List<String> subqueries = null;
		File fs = null;
		FileWriter writer = null;
		BufferedWriter buffer = null;

		try {
			FileInputStream catalogStream = new FileInputStream(catalogPath);
			partitioner = new Partitioner(catalogStream);

			long startTime = System.nanoTime();
			subqueries = partitioner.executePartitioning(xquery, getClusterSize()); //geração de subqueries + parser
			long elapsedTime = System.nanoTime() - startTime;
			
			//saida.println(elapsedTime/1000000 + " ms");
			//System.out.println("Time elapsed to generate subqueries = " + elapsedTime/1000000 + "ms");

			generateSubQueriesTime = elapsedTime/1000000;
			
			//escreve subconsultas em output/subquery_?.txt
			for (int i = 0; i < subqueries.size(); i++) {
				writer = new FileWriter(subqueriesPath + "_" + i + ".txt");
				buffer = new BufferedWriter(writer);
				buffer.write(subqueries.get(i));
				buffer.close();
				//System.out.println(subqueries.get(i));
				//Salva os contextos para serem utilizados depois na composição do resultado final
				OutputStream contextWriter = new FileOutputStream(fs = new File(contextPath + "_" + i + ".txt"));
				partitioner.getExecutionContext().save(contextWriter);
				contextWriter.close();
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (PartitioningException e) {
			e.printStackTrace();
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} 

		return subqueries;
	}

	private int getCardinality() {
		int cardinality;
		//obtém a cardinalidade do atributo de fragmentação, sendo que primeiramente deve obter xpath da subconsulta 0
		String fragment_0 = Utilities.getFileContent(AVPConst.SUBQUERIES_FILE_PATH, AVPConst.SUBQUERIES_FILE_NAME + "_0.txt");
		String xpath = fragment_0.substring(fragment_0.indexOf("')/")+3, fragment_0.indexOf("["));
		String document = fragment_0.substring(fragment_0.indexOf("doc(")+5, fragment_0.indexOf("')/"));
		if (document.lastIndexOf(".xml") != -1)
			cardinality = catalog.getCardinality(xpath, document, null);
		else
			cardinality = catalog.getCardinality(xpath, document+".xml", null);

		return cardinality;
	}

	private void executeQuery(List<String> sbqs, int vpStrategy, int cardinality, int numNQPs, boolean performDynamicLoadBalancing, boolean onlyCollectionStrategy, String tempCollectionName, int idQuery)
			throws IllegalStateException, RemoteException, InterruptedException, IllegalArgumentException, Exception {    	

		//System.out.println("Starting query execution ...");

		int[] allocatedNQP_id;
		int id_nqp_gqt; // NQP where GQT will run

		if (a_state != STATE_STARTED)
			throw new IllegalStateException("ClusterQueryProcessor not Started!");
		if (numNQPs > a_TotalNumberOfNQPs)
			throw new IllegalArgumentException(numNQPs + " NodeQueryProcessors were requested but only " + a_TotalNumberOfNQPs + " are available");
		if (sbqs.size() < numNQPs)
			throw new IllegalArgumentException("As " + numNQPs + " NodeQueryProcessors will be used, there must be at least " + numNQPs + " intervals or subqueries");

		allocatedNQP_id = new int[numNQPs];
		a_nqpAllocator.allocateNQPsForQueryProcessing(allocatedNQP_id);	// Obtém o id do NQP que irá rodar o GQT

		// get NQP objects references
		ArrayList<NodeQueryProcessor> allocatedNQPs = new ArrayList<NodeQueryProcessor>(numNQPs);
		for (int i = 0; i < allocatedNQP_id.length; i++) {
			allocatedNQPs.add(a_nqp[allocatedNQP_id[i]]);
		}

		//cria instância do GQT
		globalTask = newGlobalQueryTask(this, cardinality, allocatedNQPs, sbqs, vpStrategy, performDynamicLoadBalancing, onlyCollectionStrategy, tempCollectionName, idQuery);

		//checar
		globalTask.start();

		if (debug) {
			System.out.print("Allocated nodes : {" + id_nqp_gqt + "} ");
			for (int i = 0; i < allocatedNQP_id.length; i++) {
				System.out.print(allocatedNQP_id[i] + " ");
			}
			System.out.println();
		}
		//a_nqpAllocator.disposeNQPs(allocatedNQP_id);

		resultWriter.flush();
		resultWriter.close();
	}

	//cria uma GQT para um NQP específico (1o. parâmetro), passando os NQPs alocados e as subconsultas
	public GlobalQueryTask newGlobalQueryTask(ClusterQueryProcessor cqp, int cardinality, ArrayList<NodeQueryProcessor> nqps, List<String> sbqs,
			int vpStrategy, boolean performDynamicLoadBalancing, boolean onlyCollectionStrategy, String tempCollectionName, int idQuery) throws RemoteException, IOException {

		//System.out.println("Creating GlobalQueryTask object ...");
		GlobalQueryTask globalTask = new GlobalQueryTaskEngine(cqp, cardinality, nqps, sbqs, vpStrategy, performDynamicLoadBalancing, onlyCollectionStrategy, tempCollectionName, idQuery);

		return globalTask;
	}

	public synchronized void shutdown() throws RemoteException,	NotBoundException, MalformedURLException {
		System.out.println("Shuting down...");
		if (a_state == STATE_STARTED) {
			System.out.println("Disconnecting from NodeQueryProcessors...");
			for (int i = 0; i < a_nodes.length; i++) {
				System.out.println("Node " + a_nodes[i].getAddress());
				a_nodes[i].getNQP(i).shutdown();
				a_nqp[i].shutdown();
				a_nodes[i] = null;
				a_nqp[i] = null;
			}
			a_nodes = null;
			a_nqp = null;
		}
		
		System.out.println("Unbinding...");
		Naming.unbind(a_objectName);
		//deleta o arquivo CQP.conf que foi utilizado pelo CQP
		System.out.println("Cleaning conf and temp files ...");
		Utilities.deleteCQPConf(AVPConst.CQP_CONF_DIR_NAME);
		//System.out.println("Cleaning temp collection ...");
		
		System.out.println("Exit.");
		a_shutdown_requested = true;
		notifyAll();
	}

	public SystemResourceStatistics[] getGlobalSystemResourceStatistics() throws RemoteException {
		SystemResourceStatistics[] statistics = new SystemResourceStatistics[a_TotalNumberOfNQPs];

		for (int nodenum = 0; nodenum < a_nodes.length; nodenum++) {
			if (a_nodes[nodenum].getNumNQPs() > 0) {
				statistics[nodenum] = a_nodes[nodenum].getNQP(0).getSystemResourceStatistics();
			}
		}
		return statistics;
	}

	public synchronized void setClusterSize(int size) throws RemoteException {
		if (size > a_nqp.length)
			throw new IllegalArgumentException("CQP_Scheduler_Engine Exception: Cannot change size to " + size + " because there are only " + a_nqp.length	+ " NQPs.");

		NodeQueryProcessor[] nqp = new NodeQueryProcessor[size];
		for (int i = 0; i < size; i++)
			nqp[i] = a_nqp[i];
		a_currentClusterSize = size;
		a_nqpAllocator = new NqpAllocator(nqp, 0);
		notifyAll();
	}

	public int getClusterSize() throws RemoteException {
		return a_currentClusterSize;
	}

	public NodeQueryProcessor getNQP(int i) throws RemoteException {
		return a_nqp[i];
	}

	public void addNode(String host, int port) throws RemoteException {
		// TODO Auto-generated method stub

	}

	public void dropNode(int nodeId) throws RemoteException {
		// TODO Auto-generated method stub

	}

	public String getNodesList() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	public String getObjectName() throws RemoteException {
		return a_objectName;
	}
	
	public PrintWriter getSaida() throws RemoteException  {
		return saida;
	}

	public long getGenerateSubQueriesTime() throws RemoteException {
		return generateSubQueriesTime;
	}
	
	public long getPrepareConsolidationTime() throws RemoteException {
		return prepareConsolidationTime;
	}
	
	public String getResultsTimeFileName() throws RemoteException {
		return resultsTimeFileName;
	}
	
	public FinalResultComposer getComposer() throws RemoteException {
		return composer;
	}
}

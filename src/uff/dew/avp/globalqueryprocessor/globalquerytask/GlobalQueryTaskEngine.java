package uff.dew.avp.globalqueryprocessor.globalquerytask;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.commons.LocalQueryTaskStatistics;
import uff.dew.avp.commons.SystemResourceStatistics;
import uff.dew.avp.commons.Utilities;
import uff.dew.avp.localqueryprocessor.localquerytask.LocalQueryTask;
import uff.dew.avp.localqueryprocessor.nodequeryprocessor.NodeQueryProcessor;
import uff.dew.avp.globalqueryprocessor.clusterqueryprocessor.ClusterQueryProcessor;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.Range;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.RangeStatistics;
import uff.dew.svp.ExecutionContext;
import uff.dew.svp.FinalResultComposer;

public class GlobalQueryTaskEngine extends UnicastRemoteObject implements GlobalQueryTask {

	private static final long serialVersionUID = 3257288045500904500L;
	private Logger logger = Logger.getLogger(GlobalQueryTaskEngine.class);
	private ArrayList<LocalQueryTaskStatistics> localTaskStatistics;
	private ArrayList<SystemResourceStatistics> systemResourceStatistics;
	private int numLocalTasks;
	private int numLocalQueryTasksFinished = 0;
	private int numIntervalsFinished;
	private ClusterQueryProcessor cqp;
	private int cardinality; //guarda a cardinalidade do atributo de fragmentação
	private ArrayList<NodeQueryProcessor> nqps = new ArrayList<NodeQueryProcessor>();
	private List<String> sbqs = null;
	private boolean getStatistics;
	private boolean getSystemResourceStatistics;
	private int queryExecutionStrategy;
	private ArrayList<LocalQueryTask> lqts;
	// For dynamic load balancing
	private boolean performDynLoadBal;
	private boolean onlyCollectionStrategy;
	// for predefined partition sizes
	private int[] predefinedPartitionSizes; // used to simulate skew
	private Exception exception;
	private static String resultsTimeFile;
	private static long globalComposeTime = 0;

	private FinalResultComposer composer;
	private String tempCollectionName;
	private int idQuery;

	private int numNQPs;

	public GlobalQueryTaskEngine(ClusterQueryProcessor cqp, int cardinality, ArrayList<NodeQueryProcessor> nqps, List<String> sbqs,
			int vpStrategy, boolean performDynamicLoadBalancing, boolean onlyCollectionStrategy, String tempCollectionName, int idQuery) throws IOException {
		this.cqp = cqp;
		this.cardinality = cardinality;
		this.nqps = nqps;
		this.numNQPs = nqps.size();
		this.sbqs = sbqs;
		this.queryExecutionStrategy = vpStrategy;
		this.performDynLoadBal = performDynamicLoadBalancing;
		this.onlyCollectionStrategy = onlyCollectionStrategy;
		this.tempCollectionName = tempCollectionName;
		this.idQuery = idQuery;
	}

	//Este método é chamado por uma instância nas classes ClusterQueryProcessorEngine e CQP_Scheduler_Engine
	public void start() throws RemoteException, InterruptedException, Exception {
		int numIntervalsLocalTask; // number of intervals in a local query task
		int countLocalTasks;
		int initialInterval, endInterval; // Where each interval begins and ends
		int size; // interval's size

		// Start Local Query Tasks
		logger.debug(Messages.getString("globalQueryTaskEngine.createLQTs"));		
		this.numLocalTasks = this.nqps.size();
		numIntervalsLocalTask = this.nqps.size() / numLocalTasks;
		this.numIntervalsFinished = 0;
		size = cardinality / this.nqps.size(); //obtém o tamanho de cada intervalo de posições a ser encaminhado p/ os nós


		// Creating Local Query Task objects
		this.lqts = new ArrayList<LocalQueryTask>(numLocalTasks);

		for (countLocalTasks = 0; countLocalTasks < numLocalTasks; countLocalTasks++) {
			LocalQueryTask newLqt;
			Range r;
			RangeStatistics statistics;

			initialInterval = Integer.parseInt(Utilities.getIntervalBeginning(sbqs.get(countLocalTasks)));
			endInterval = Integer.parseInt(Utilities.getIntervalEnding(sbqs.get(countLocalTasks)));

			// define um conjunto de atributos relacionados ao fragmento (e.g., firstValue, lastValue, vpSize etc.)
			r = new Range(1, initialInterval, endInterval, this.nqps.size());

			// define um conjunto de valores utilizados no ajuste do tamanho do fragmento
			statistics = new RangeStatistics(1,	r.getOriginalLastValue() - r.getFirstValue() + 1, (float) (1.0 / numNQPs));
					
		            
			logger.debug(Messages.getString("globalQueryTaskEngine.createLQT",new Object[]{countLocalTasks,initialInterval,endInterval}));

			newLqt = this.nqps.get(countLocalTasks).newLocalQueryTask(countLocalTasks, this, sbqs.get(countLocalTasks), numIntervalsLocalTask,
					this.queryExecutionStrategy, numLocalTasks,	this.performDynLoadBal, this.onlyCollectionStrategy, true, r, statistics, tempCollectionName, this.idQuery);

			this.lqts.add(newLqt);

		} //fim do laço for

		// Starting Local Query Tasks threads
		for (countLocalTasks = 0; countLocalTasks < numLocalTasks; countLocalTasks++)
			lqts.get(countLocalTasks).start();

		// Wait for intervals finishing
		logger.debug(Messages.getString("globalQueryTaskEngine.waitingForIntervals"));

		synchronized (this) {
			while (this.numIntervalsFinished < numLocalTasks && exception == null) {
				wait(100);
			}
		}

		if(exception != null)
			throw exception;

		// All vps were processed. Finish LQTs.
		//this.numLocalQueryTasksFinished = 0;

		for(LocalQueryTask lqt : lqts)  {
			lqt.finish();
		}

		// Wait for lqts finishing
		logger.debug(Messages.getString("globalQueryTaskEngine.waitingForLQTs"));

		synchronized (this) {
			while (this.numLocalQueryTasksFinished < numLocalTasks && exception == null) {
				wait(100);
			}
		}
		if(exception != null)
			throw exception;

		// Local Query Tasks Finished.
		logger.debug(Messages.getString("globalQueryTaskEngine.getFinalResult"));
		logger.debug(Messages.getString("globalQueryTaskEngine.finished"));
	}

	public void getLQTReferences(int[] id, LocalQueryTask[] reference)
			throws RemoteException {
		if (id.length != reference.length)
			throw new IllegalArgumentException("GlobalQueryTaskEngine.getLQTReferences(): 'id' and 'reference' arrays must have the same size!");
		for (int i = 0; i < id.length; i++)
			reference[i] = getLQTReference(id[i]);
	}

	public LocalQueryTask getLQTReference(int id) throws RemoteException {
		if ((id >= 0) && id < (this.lqts.size()))
			return this.lqts.get(id);
		else
			return null;
	}

	//	public synchronized void localQueryTaskFinished(int localTaskId, Exception exception) throws RemoteException {
	//		System.out.println("GQT localQueryTaskFinished");
	//		this.numLocalQueryTasksFinished++;
	//		this.exception = exception;
	//
	//		//INCLUIR AQUI CHAMADA PARA METODO QUE GRAVA RESULTADO PARCIAL COMO COLECAO - NO CASO DO AVP - CHECAR
	//
	//		notifyAll();
	//	}

	//TESTE
	public synchronized void localQueryTaskFinished(int localTaskId, LocalQueryTaskStatistics lqtstatistics, Exception exception) throws RemoteException {
		//System.out.println("GQT localQueryTaskFinished");
		this.numLocalQueryTasksFinished++;
		this.exception = exception;
		if(localTaskStatistics != null)
			localTaskStatistics.set(localTaskId, lqtstatistics);

		if (!hasLQTprocessing()) { //checa se é o último LQT
			//composer.setForceTempCollectionExecutionMode(false); // irá salvar em uma coleção temporária no SGBDX, se não fizer isso analisará a consulta e definirá se salva em arquivo (agregando todos os resultados) ou em uma colegação temporária do SGBDX (executando consulta final)
			finalConsolidation(this.cqp.getComposer(), lqts);
		}
		
		notifyAll();
	}

	public synchronized void localQueryTaskFinished(int localTaskId, LocalQueryTaskStatistics statistics,
			SystemResourceStatistics resourceStatistics, Exception exception) throws RemoteException {
		this.numLocalQueryTasksFinished++;
		this.exception = exception;
		if (this.localTaskStatistics != null) {
			this.localTaskStatistics.set(localTaskId,statistics);

		}
		if (this.systemResourceStatistics != null) {
			this.systemResourceStatistics.set(localTaskId,resourceStatistics);
		}
		
		notifyAll();
	}

	// TESTAR COM SYNCRONIZED
	//	public synchronized void localIntervalFinished(int localTaskId) throws RemoteException {
	//		System.out.println("GQT localIntervalFinished");
	//		logger.debug(Messages.getString("globalQueryTaskEngine.intervalFinished",localTaskId));
	//		this.numIntervalsFinished++;
	//
	//		File fs = null;
	//		String filename = "result.xml"; //receberá os resultados parciais se o forceTempCollectionMode (FinalResulstComposer) for false (padrão)
	//		OutputStream resultWriter;
	//		try {
	//			resultWriter = new FileOutputStream(fs = new File(filename));
	//
	//			FinalResultComposer composer = new FinalResultComposer(resultWriter);
	//
	//			composer.setDatabaseInfo(AVPConst.DB_CONF_HOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME, 
	//					AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE);
	//
	//			// the constructFinalQuery (below) was originally executed using the same context
	//			// previously used to retrieve a partial result (at the coordinator node). 
	//			// That means that some singletons objects were populated when compiling the final 
	//			// result. Now we don't have that information, so we need to restore it.
	//			File file = new File("output/context.txt");
	//			InputStream is = new FileInputStream(file);
	//			composer.setExecutionContext(ExecutionContext.restoreFromStream(is));
	//			is.close();
	//
	//			//composer.setForceTempCollectionExecutionMode(true); // irá salvar em uma coleção temporária no SGBDX, se não fizer isso analisará a consulta e definirá se salva em arquivo (agregando todos os resultados) ou em uma colegação temporária do SGBDX (executando consulta final)
	//			
	//			if (hasLQTprocessing()) { //checa se ainda tem LQT processando, se tem faz consolidação dos resultados parciais desse LQT
	//				finalConsolidation(composer, getLQTReference(localTaskId), true);
	//			
	//			} else { // se for o último LQT ativo, faz consolidação dos resultados parciais e finaliza com consulta na coleção ou gravando o último resultado parcial no arquivo de resultados
	//				
	//				//Carrega os documentos XML com os resultados parciais em uma coleção temporária
	//				finalConsolidation(composer, getLQTReference(localTaskId), false);
	//
	//			}
	//
	//			resultWriter.flush();
	//			resultWriter.close();
	//		
	//		} catch (FileNotFoundException e) {
	//			e.printStackTrace();
	//		}
	//		catch (IOException e) {
	//			e.printStackTrace();
	//		} catch (DatabaseException e) {
	//			e.printStackTrace();
	//		}
	//		
	//		notifyAll();
	//		
	//	}

	//TESTE
	public synchronized void localIntervalFinished(int localTaskId) throws RemoteException {
		//System.out.println("GQT localIntervalFinished - localTaskId is " + localTaskId);
		this.numIntervalsFinished++;
		logger.debug(Messages.getString("globalQueryTaskEngine.intervalFinished",localTaskId));
		notifyAll();

	}

	private void finalConsolidation(FinalResultComposer composer, ArrayList<LocalQueryTask> lqts) {
		String contextPath = AVPConst.CONTEXT_FILE_PATH+AVPConst.CONTEXT_FILE_NAME;
		List<String> partials = new ArrayList<String>();
		File file = null;
		InputStream is = null;

		String auxTime;

		try {

			long startTime = System.nanoTime();

			if (!onlyCollectionStrategy) {
				//Insere os arquivos com os resultados parciais de todos os LQTs na lista "partials"
				for (int i = 0; i < lqts.size(); i++) {
					// the constructFinalQuery (below) was originally executed using the same context
					// previously used to retrieve a partial result (at the coordinator node). 
					// That means that some singletons objects were populated when compiling the final 
					// result. Now we don't have that information, so we need to restore it.
					File contextFile = new File(contextPath + "_" + i + ".txt");
					InputStream isFile = new FileInputStream(contextFile);
					composer.setExecutionContext(ExecutionContext.restoreFromStream(isFile));

					//System.out.println("composer = " + composer.toString());
					isFile.close();

					for (int j = 0; j < lqts.get(i).getCompletePartialResultFileNames().size(); j++) {
						partials.add(lqts.get(i).getCompletePartialResultFileNames().get(j).toString());
						//System.out.println("partial = " + lqts.get(i).getCompletePartialResultFileNames().get(j).toString());
						String filename = partials.get(i);
						//System.out.println("No GQT: " + filename);
						file = new File(filename);
						is = new FileInputStream(file);
						//System.out.println("is = " + is.toString());
						composer.loadPartial(is);
						is.close();
					}
				}
			} else {

				composer.setExecutionContext(ExecutionContext.restoreFromStream(new FileInputStream(AVPConst.SUBQUERIES_FILE_PATH+AVPConst.SUBQUERIES_FILE_NAME + "_0.txt")));

			}

			auxTime = composer.combinePartialResults();//executa consulta final na colecao temporaria ou apenas concatena resultados

			long elapsedTime = System.nanoTime() - startTime;

			//composer.cleanup();//apaga colecao/arquivos temporarios
			//cqp.getSaida().println("Time-Parser-Generating-SubQueries;Time-Preparing-Consolidation;Time-Exec-Final-Query;Time-Write-Final-Result;Time-Final-Consolidation-GQT;All-Processing-Time-Execution-CQP-without-Parser-SBQGenerate;All-Processing-Time-Execution-CQP-with-Parser-SBQGenerate;All-Processing-Time-Execution-Client");
			cqp.getSaida().print(cqp.getGenerateSubQueriesTime() + ";" + cqp.getPrepareConsolidationTime() + ";" + auxTime.substring(0, auxTime.indexOf(";")) + ";" + auxTime.substring(auxTime.indexOf(";")+1) + ";" + elapsedTime/1000000 + ";");					
			//			cqp.getSaida().print("Time-Exec-Final-Query: ");
			//			cqp.getSaida().println(auxTime.substring(0, auxTime.indexOf(";")) + " ms");
			//			
			//			cqp.getSaida().print("Time-Write-Final-Result: ");
			//			cqp.getSaida().println(auxTime.substring(auxTime.indexOf(";")+1) + " ms");
			//			
			//			cqp.getSaida().print("Time-Final-Consolidation-GQT: ");
			//			cqp.getSaida().println(elapsedTime/1000000 + " ms");

			//System.out.println("Time elapsed to final consolidation = " + elapsedTime/1000000 + "ms");
			lqts.get(0).setTimeComposePartialResultFiles(elapsedTime / 1000000);

			// need to sort list
			//		List<String> partials = new ArrayList<String>();
			//		for (int i = 0; i< completePartialResultFileNames.size(); i++) {
			//			//System.out.println(completePartialResultFileNames.get(i).toString());
			//			partials.add(completePartialResultFileNames.get(i).toString());
			//		}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean hasLQTprocessing() {
		if (this.numLocalTasks > this.numLocalQueryTasksFinished)
			return true;
		else
			return false;
	}

	public static String getResultsTimeFile() {
		return GlobalQueryTaskEngine.resultsTimeFile;
	}

	public static void setResultsTimeFile(String resultsTimeFile) {
		GlobalQueryTaskEngine.resultsTimeFile = resultsTimeFile;
	}
}

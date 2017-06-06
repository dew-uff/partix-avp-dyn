package uff.dew.avp.localqueryprocessor.queryexecutor;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.rmi.RemoteException;

import uff.dew.avp.commons.Utilities;
import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.LocalQueryTaskStatistics;
import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Messages;
import uff.dew.avp.connection.DBConnectionPoolEngine;
import uff.dew.avp.localqueryprocessor.localquerytask.LocalQueryTask;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.PartitionSize;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.PartitionTuner;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.PartitionTunerMT_NonUniform;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.PartitionTunerMeanTime;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.PartitionTunerPure;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.Range;
import uff.dew.avp.localqueryprocessor.dynamicrangegenerator.RangeStatistics;
import uff.dew.svp.SubQueryExecutionException;
import uff.dew.svp.SubQueryExecutor;
import uff.dew.svp.db.DatabaseException;


public class QueryExecutorAvp extends QueryExecutor {

	private Logger logger = Logger.getLogger(QueryExecutorAvp.class);
	private PartitionTuner partitionTuner;
	private PartitionSize currentPartitionSize;
	private int nextRangeValue;
	private Preview preview;
	private int lqtId;
	private int idQuery;
	
	private OutputStream out = null;
	private String filename = null;
	private FileOutputStream filepath = null;
	private File fs = null;

	private FileOutputStream myFile;
	private PrintStream myStream;
	

	public QueryExecutorAvp(LocalQueryTask lqt, DBConnectionPoolEngine dbpool, String query, Range range,
			RangeStatistics statistics, LocalQueryTaskStatistics lqtStatistics, boolean onlyCollectionStrategy, String tempCollectionName, int idQuery) throws RemoteException {
		super(lqt, dbpool, query, range, lqtStatistics, onlyCollectionStrategy, tempCollectionName, idQuery);
		
		this.lqtId = lqt.getId();
		this.idQuery = idQuery;
		//this.partitionTuner = new PartitionTunerMT_NonUniform(this.range.getStatistics());
		//partitionTuner = new PartitionTunerMT_NonUniform(range.getVPSize(), statistics);
		//partitionTuner = new PartitionTunerMeanTime(statistics);
		partitionTuner = new PartitionTunerPure(statistics, range.getOriginalLastValue()-range.getFirstValue());

		currentPartitionSize = null;
		if (this.lqtStatistics != null)
			this.lqtStatistics.setPartitionTuner(this.partitionTuner);

		preview = new Preview(range.getFirstValue(),range.getOriginalLastValue());
		//System.out.println("QueryExecutorAvp constructor ...");
	}

	protected boolean getQueryLimits(String query, int[] limits) {
		switch (state) {
		case ST_STARTING_RANGE: {
			limits[0] = range.getFirstValue();
			//limits[0] = Integer.parseInt(SubQuery.getIntervalBeginning(query));
			state = ST_PROCESSING_RANGE;
			currentPartitionSize = partitionTuner.getPartitionSize();
			break;
		}
		case ST_PROCESSING_RANGE: {
			limits[0] = nextRangeValue;
			if (partitionTuner.stillTuning() && currentPartitionSize.getNumPerformedExecutions() >= currentPartitionSize.getNumExpectedExecutions()) {
				// Number of expected executions was reached.
				// Send feedback to partition tuner.
				partitionTuner.setSizeResults(currentPartitionSize);
				// Ask a new partition size.
				currentPartitionSize = partitionTuner.getPartitionSize();
			}
			break;
		}
		default: {
			throw new IllegalThreadStateException("LocalQueryTaskEngine_AVP Exception: getQueryLimits() should not be called while in state " + state + "!");
		}
		}
		
		//Luiz Matos, para permitir que os calculos dos novos tamanhos iniciais sejam feitos com base nos novos subintervalos
		partitionTuner.setMaxNumberOfKeys(range.getCurrentLastValue()-range.getFirstValue()+1);
		
		limits[1] = range.getNextValue(currentPartitionSize.numberOfKeys());
		//limits[1] = Integer.parseInt(SubQuery.getIntervalEnding(query));
		nextRangeValue = limits[1];
		
		if (limits[0] == limits[1]) {
			state = ST_RANGE_PROCESSED;

			/*
			 * if ( a_partitionTuner.stillTuning() ) { if(
			 * a_currentPartitionSize.getNumPerformedExecutions() > 0 ) { //
			 * Executions were performed. // Send feedback to partition tuner.
			 * a_partitionTuner.setSizeResults( a_currentPartitionSize ); // Ask
			 * a new partition size. a_currentPartitionSize =
			 * a_partitionTuner.getPartitionSize(); } }
			 */
			try {
				myFile= new FileOutputStream(AVPConst.PARTIAL_RESULTS_FILE_PATH+Utilities.createPTunerStatisticsFileName(this.lqtId, "AVP", this.idQuery, limits[0]));

				myStream = new PrintStream(myFile);
				partitionTuner.printTuningStatistics(myStream);
				
				myStream.flush();
				myStream = null;
				myFile.close();
				
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			partitionTuner.reset();
			return false;
		} else if (limits[0] < limits[1]) {
			partitionTuner.setLastNumberOfKeys(limits[1]-limits[0]);//utilizado no AVP-DYN para guardar o último intervalo utilizado antes da oferta de ajuda
			return true;
		}
		else
			throw new IllegalThreadStateException("LocalQueryTaskEngine_AVP Exception: lower limit superior to upper!");
	}

	protected void executeSubQuery(String query, int[] limit)
			throws RemoteException, SubQueryExecutionException, DatabaseException {
		//System.out.println("Entrei no método executeSubQuery() da classe QueryExecutorAvp");
		long startTime; long elapsedTime;
		boolean hasResults = false;

		//Insere subintervalos no lugar do ? na subquery
		while(query.indexOf("?") > -1) {
			query = query.replaceFirst("\\?",limit[0]+"");
			query = query.replaceFirst("\\?",limit[1]+"");
		}  
		//System.out.println("Query transformada: " + query);
		int aux = limit[1]-limit[0];
		//System.out.println("range: " + limit[0] + " - " + limit[1] + " ("+aux+" positions)");

		//FINALMENTE A EXECUÇÃO DA CONSULTA

		try {
			//Execução da subconsulta usando svp_lib
			SubQueryExecutor sqe = new SubQueryExecutor(query);

			sqe.setDatabaseInfo(AVPConst.DB_CONF_LOCALHOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME,
					AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE);

			if (onlyCollectionStrategy) { //somente se for gravar resultados parciais direto na colecao temporaria

				startTime = System.nanoTime();
				hasResults = sqe.executeQuery(onlyCollectionStrategy, this); //Passa true se quer gravar direto na colecao, sem passar pelo sistema de arquivos
				elapsedTime = System.nanoTime() - startTime;

			} else { //se for gravar os resultados parciais no sistema de arquivos
				filename = AVPConst.PARTIAL_RESULTS_FILE_PATH + "/partial_" + limit[0] + ".xml";

				out = new FileOutputStream(fs = new File(filename));

				// execute query, saving result to a partial file in local fs
				startTime = System.nanoTime();
				hasResults = sqe.executeQuery(out);
				elapsedTime = System.nanoTime() - startTime;

				this.setCompleteFileName(filename);
				//System.out.println("Time elapsed to process the subquery = " + this.getTimeProcessingSubqueries() + "ms");

				out.flush();
				out.close();
				out = null;

				// if it doesn't have results, delete the partial file
				if (!hasResults) {
					fs.delete();
				}
			}
			//currentPartitionSize.setExecTime(elapsedTime / 1000000); //aqui é registrado o desempenho (tempo) do proc. do fragmento - Tempo Médio
			//Considerando SOMENTE o tempo de execucao da subconsulta
			//Usar "elapsedTime" se for para considerar tempo de exec. subq. + construcao partialResult + inserir na colecao temp.
//			System.out.println("elapsedTime (nano) = " + this.getTimeExecSubquery()*1000000);
//			System.out.println("elapsedTime (ms) = " + this.getTimeExecSubquery());
			currentPartitionSize.setExecNotMeanTime(this.getTimeExecSubquery()); //aqui é registrado o desempenho (tempo) do proc. do fragmento já em ms - Tempo Absoluto 
			
			this.setTimeProcessingSubqueries(elapsedTime / 1000000); // em ms - tempo total (exec. subq. + construcao partialResult + isnerir na colecao temp.)
			preview.setRange(nextRangeValue); //obtém novo intervalo
			
			logger.debug(Messages.getString("queryExecutorAvp.elapsedTime2",new Object[]{this.getTimeExecSubquery()}));//this.getTimeExecSubquery() = somente o tempo de exec. da subq.
			
			if (lqtStatistics != null)
				//lqtStatistics.queryFinished(elapsedTime); //conversao de nanoTime para milliseconds eh feita na classe LocalQueryTaskStatistics 
				  lqtStatistics.queryFinished(this.getTimeExecSubquery()); 
				
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class Preview {
		private long init;
		private int rangeBegin;
		private int rangeEnd;
		private int actual;

		public Preview(int rangeBegin, int rangeEnd) {
			this.rangeBegin = rangeBegin;
			this.rangeEnd = rangeEnd;
			actual = rangeBegin;
			//init = System.currentTimeMillis();
			init = System.nanoTime();
		}

		public void setRange(int range) {
			actual = range;
		}

		public String toString() {
//			System.out.println("Entrei no metodo toString() da classe QueryExecutorAVP ...");
//			System.out.println("tr = rangeEnd-rangeBegin+1 = " + rangeEnd + "-" + rangeBegin + "+ 1");
			float tr = (rangeEnd-rangeBegin+1);
			//float at = System.nanoTime()-init;
			//System.out.println("at = System.nanoTime()-init = " + System.nanoTime() + "-" + init);
			float at = System.nanoTime()-init;
			
		//	System.out.println("ar = actual-rangeBegin+1 = " + actual + "-" + rangeBegin + "+ 1");
			float ar = actual-rangeBegin+1;

		//	System.out.println("total = (tr*at)/ar = " + tr + "*" + at + "/" + ar);
			long total = (long)((tr * at)/ar);

//			System.out.println("estimated (nano) = at / total = " + at + "/" + total);
//			System.out.println("estimated (ms) = (at / total)/1000000 = (" + at + "/" + total + ")/1000000");
			long estimated = (long)(at/total)/1000000;
		//	System.out.println("estimated (ms) = " + estimated);
			
			//return "Estimated time: "+at+"/"+total+"ms";
			return "Estimated time: "+estimated+"ms";
		}
	}//fim class Preview

}

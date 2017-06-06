package uff.dew.avp.localqueryprocessor.queryexecutor;

/**
 * 
 * @author lima
 * @lzomatos
 * 
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.rmi.RemoteException;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.LocalQueryTaskStatistics;
import uff.dew.avp.commons.Logger;
import uff.dew.avp.commons.Utilities;
import uff.dew.avp.connection.DBConnectionPoolEngine;
import uff.dew.avp.localqueryprocessor.localquerytask.LocalQueryTask;
import uff.dew.svp.SubQueryExecutionException;
import uff.dew.svp.SubQueryExecutor;
import uff.dew.svp.db.DatabaseException;

public class QueryExecutorSvp extends QueryExecutor {

	private Logger logger = Logger.getLogger(QueryExecutorSvp.class);
	private Preview preview;
	private int nextRangeValue;

	private OutputStream out = null;
	private String filename = null;
	private FileOutputStream filepath = null;
	private File fs = null;

	/** Creates a new instance of QueryExecutor_SVP */
	public QueryExecutorSvp(LocalQueryTask lqt, DBConnectionPoolEngine dbpool, String query, LocalQueryTaskStatistics lqtStatistics, boolean onlyCollectionStrategy, String tempCollectionName, int idQuery) throws RemoteException {
		//super(lqt, dbpool, localResultComposition, query, isFinalQuery);
		super(lqt, dbpool, query, lqtStatistics, onlyCollectionStrategy, tempCollectionName, idQuery);
		//System.out.println("QueryExecutor_SVP: " + tempCollectionName);
		//preview = new Preview(Integer.parseInt(SubQuery.getIntervalBeginning(query)), Integer.parseInt(SubQuery.getIntervalEnding(query)));
		preview = new Preview(Integer.parseInt(Utilities.getIntervalBeginning(query)), Integer.parseInt(Utilities.getIntervalEnding(query)));

		//System.out.println("QueryExecutorSvp constructor ...");
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
			init = System.nanoTime();
		}

		public void setRange(int range) {
			actual = range;
		}

		public String toString() {
			float tr = (rangeEnd-rangeBegin+1);
			float at = System.nanoTime()-init;
			float ar = actual-rangeBegin+1;
			long total = (long)((tr * at)/ar)/1000000;
			return "Estimated time: "+at+"/"+total+" ms";
		}
	}

	@Override
	protected boolean getQueryLimits(String query, int[] limits) {
		switch (state) {
		case ST_STARTING_RANGE: {
			//limits[0] = range.getFirstValue();
			// limits[0] = Integer.parseInt(SubQuery.getIntervalBeginning(query));
			limits[0] = Integer.parseInt(Utilities.getIntervalBeginning(query));

			state = ST_PROCESSING_RANGE;
			//currentPartitionSize = partitionTuner.getPartitionSize();
			break;
		}
		case ST_PROCESSING_RANGE: {
			limits[0] = nextRangeValue;
			//limits[0] = Integer.parseInt(SubQuery.getIntervalBeginning(query));
			//            if (partitionTuner.stillTuning()) {
			//                if (currentPartitionSize.getNumPerformedExecutions() >= currentPartitionSize.getNumExpectedExecutions()) {
			//                    // Number of expected executions was reached.
			//                    // Send feedback to partition tuner.
			//                    partitionTuner.setSizeResults(currentPartitionSize);
			//                    // Ask a new partition size.
			//                    currentPartitionSize = partitionTuner.getPartitionSize();
			//                }
			//            }
			break;
		}
//		case ST_WAITING_NEW_RANGE: {
//			break;
//		}
		default: {
			throw new IllegalThreadStateException("LocalQueryTaskEngine_SVP Exception: getQueryLimits() should not be called while in state "  + state + "!");
		}
		}
		//limits[1] = range.getNextValue(currentPartitionSize.numberOfKeys());
		//limits[1] = Integer.parseInt(SubQuery.getIntervalEnding(query));
		limits[1] = Integer.parseInt(Utilities.getIntervalEnding(query));

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
			//partitionTuner.reset();
			return false;
		} else if (limits[0] < limits[1])
			return true;
		else
			throw new IllegalThreadStateException("LocalQueryTaskEngine_SVP Exception: lower limit superior to upper!");
	}

	@Override
	protected void executeSubQuery(String query, int[] limit) throws RemoteException, SubQueryExecutionException, DatabaseException {
		//System.out.println("Entrei no método executeSubQuery() da classe QueryExecutorSvp");
		long startTime; long elapsedTime;
		boolean hasResults = false;

		//FINALMENTE A EXECUÇÃO DA CONSULTA

		try {
			//Execução da subconsulta usando svp_lib
			SubQueryExecutor sqe = new SubQueryExecutor(query);

			sqe.setDatabaseInfo(AVPConst.DB_CONF_LOCALHOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME,
					AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE);

			if (!onlyCollectionStrategy) { //somente se for gravar resultados parciais no sistema de arquivos
				filename = AVPConst.PARTIAL_RESULTS_FILE_PATH + "/partial_" + limit[0] + ".xml";

				out = new FileOutputStream(fs = new File(filename));

				startTime = System.nanoTime();
				// execute query, saving result to a partial file in local fs
				hasResults = sqe.executeQuery(out);
				elapsedTime = System.nanoTime() - startTime;
                this.setCompleteFileName(filename);
				out.flush();
				out.close();
				out = null;
				
				// if it doesn't have results, delete the partial file
				if (!hasResults) {
					fs.delete();
				}
				
			} else { //se for gravar os resultados parciais direto na colecao temporaria
		
				startTime = System.nanoTime();
				hasResults = sqe.executeQuery(onlyCollectionStrategy, this); //Passa true se quer gravar direto na colecao, sem passar pelo sistema de arquivos
				elapsedTime = System.nanoTime() - startTime;
			}

			this.setTimeProcessingSubqueries(elapsedTime / 1000000); // em ms
			//System.out.println("Time elapsed to process the subquery = " + this.getTimeProcessingSubqueries() + "ms");
			
			//currentPartitionSize.setExecTime(elapsedTime / 1000000); //aqui é registrado o desempenho (tempo) do proc. do fragmento
			preview.setRange(nextRangeValue); //obtém novo intervalo
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

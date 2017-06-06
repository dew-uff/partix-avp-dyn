package uff.dew.test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.Utilities;
import uff.dew.svp.SubQueryExecutionException;
import uff.dew.svp.SubQueryExecutor;
import uff.dew.svp.db.DatabaseException;

public class SubQueryExecutorTest  {	

	public static void main(String args[]) throws IOException {

		// fragment file from SVP
		//String fragment = Utilities.getFileContent(AVPConst.SUBQUERIES_FILE_PATH, AVPConst.SUBQUERIES_FILE_NAME + "_0.txt");
		String fragment = Utilities.readFile(AVPConst.SUBQUERIES_FILE_PATH+AVPConst.SUBQUERIES_FILE_NAME + "_0.txt", StandardCharsets.UTF_8);

		boolean onlyCollection = true; //false para gravar antes os resultados parciais no sistema de arquivos - true para os resultados parciais irem direto para a colecao temporaria
		boolean hasResults;
		
		OutputStream out = null;
		String filename = null;
		FileOutputStream filepath = null;
		File fs = null;

		long startTime; long elapsedTime;
		
		try {

			SubQueryExecutor sqe = new SubQueryExecutor(fragment);
System.out.println(fragment);
			sqe.setDatabaseInfo(AVPConst.DB_CONF_LOCALHOST, AVPConst.DB_CONF_PORT, AVPConst.DB_CONF_USERNAME,
					AVPConst.DB_CONF_PASSWORD, AVPConst.DB_CONF_DATABASE, AVPConst.DB_CONF_TYPE);

			if (!onlyCollection) { //somente se for gravar resultados parciais no sistema de arquivos
				boolean zip = AVPConst.COMPRESS_DATA;
				if (zip) {
					filename = AVPConst.PARTIAL_RESULTS_FILE_PATH + "/partial_" + System.currentTimeMillis() + ".zip";
					filepath = new FileOutputStream(fs = new File(filename));

					ZipOutputStream zipout = new ZipOutputStream(new BufferedOutputStream(filepath));
					ZipEntry entry = new ZipEntry("partial_" + System.currentTimeMillis() + ".xml");
					zipout.putNextEntry(entry);
					out = zipout;
					zipout.close();
				}
				else {
					filename = AVPConst.PARTIAL_RESULTS_FILE_PATH + "/partial_" + System.currentTimeMillis() + ".xml";
					out = new FileOutputStream(fs = new File(filename));
				}
		
				startTime = System.nanoTime();
				// execute query, saving result to a partial file in local fs
				hasResults = sqe.executeQuery(true, null); //Passa true se quer gravar direto na colecao, sem passar pelo sistema de arquivos
				elapsedTime = System.nanoTime() - startTime;
				out.flush();
				out.close();
				out = null;

			} else { //se for gravar os resultados parciais direto na colecao temporaria
				startTime = System.nanoTime();
				// execute query, saving result to a partial file in local fs
				hasResults = sqe.executeQuery(onlyCollection, null); //Passa true se quer gravar direto na colecao, sem passar pelo sistema de arquivos
				elapsedTime = System.nanoTime() - startTime;
			}

			System.out.println("Time elapsed: " + elapsedTime/1000000 + " ms");

			// if it doesn't have results, delete the partial file
			if (!hasResults) {
				fs.delete();
			}

		}catch (DatabaseException e) {
			throw new IOException(e);
		}
		catch (SubQueryExecutionException e) {
			throw new IOException(e);
		}
		finally {
			if (out != null) {
				out.close();
			}
		}
	}
}

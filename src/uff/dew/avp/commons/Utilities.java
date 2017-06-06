package uff.dew.avp.commons;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Utilities {

	/** Creates a new instance of Util */
	public Utilities() {
	}

	public static String createTimeResultFileNameCQP( int numQuery, String technique, int numNodes ) {
		return  "time-query" + (numQuery < 10 ? "0" + numQuery : "" + numQuery ) +
				"_" + numNodes + "nodes_" + technique.trim() + "_" + System.currentTimeMillis() + ".txt";
	}

	public static String createTimeResultFileNameQE( int lqtId, String technique, int idQuery ) {
		return  "time-query" + (idQuery < 10 ? "0" + idQuery : "" + idQuery ) +
				"_lqt" + (lqtId < 10 ? "0" + lqtId : "" + lqtId ) +
				"_" + technique.trim() + "_" + System.currentTimeMillis() + ".txt";
	}

	public static String createPTunerStatisticsFileName( int lqtId, String technique, int idQuery, int maxPos ) {
		return  "PTuner-statistics-query" + (idQuery < 10 ? "0" + idQuery : "" + idQuery ) +
				"_lqt" + (lqtId < 10 ? "0" + lqtId : "" + lqtId ) + "_" + maxPos +
				"_" + technique.trim() + "_" + System.currentTimeMillis() + ".txt";
	}
	
	public static String createLBalanceStatisticsFileName( int lqtId, String technique, int idQuery ) {
		return  "LBalance-statistics-query" + (idQuery < 10 ? "0" + idQuery : "" + idQuery ) +
				"_lqt" + (lqtId < 10 ? "0" + lqtId : "" + lqtId ) + 
				"_" + technique.trim() + "_" + System.currentTimeMillis() + ".txt";
	}
	
	public static String createDataResultFileName( int numQuery, String technique, int numNodes ) {
		return  "data-query" + (numQuery < 10 ? "0" + numQuery : "" + numQuery ) +
				"_" + numNodes + "nodes_" + technique.trim() + System.currentTimeMillis() + ".txt";
	}

	public static String getFileContent(String filePath, String fileName) {
		FileReader reader;
		BufferedReader buffer;
		String content = "";

		try {
			reader = new FileReader(filePath+fileName);
			buffer = new BufferedReader(reader);

			while(buffer.ready())
				content += buffer.readLine();

			buffer.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	public static String readFile(String path, Charset encoding) throws IOException 
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return encoding.decode(ByteBuffer.wrap(encoded)).toString();
	}

	public static String setCQPFileConf(String filePathName, String content) {
		FileWriter writer;
		BufferedWriter buffer;

		try {
			writer = new FileWriter(filePathName);
			buffer = new BufferedWriter(writer);
			buffer.write(content+"\n");

			buffer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}
	
	public static String setNQPFileConf(String filePathName, String content) {
		FileWriter writer;
		BufferedWriter buffer;

		try {
			writer = new FileWriter(filePathName, true);
			buffer = new BufferedWriter(writer);
			buffer.write(content+"\n");

			buffer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	public static void deleteCQPConf(String fileDir) {
		File diretorio = new File(fileDir);   
        
        FileFilter ff = new FileFilter() {   
            public boolean accept(File arquivo){   
                return arquivo.getName().endsWith(".conf");   
            }   
        };   
          
        File[] arquivos = diretorio.listFiles(ff);   
    
        if(arquivos != null){   
            for(File arquivo : arquivos){   
               arquivo.delete();    
            }   
        }  
	}
	
	public static void deleteCQPConfTest(String filePathName) {
		File writer;
		writer = new File(filePathName);
		writer.delete();
	}
	
	public static String setFileContent(String filePathName, String content) {
		FileWriter writer;
		BufferedWriter buffer;

		try {
			writer = new FileWriter(filePathName);
			buffer = new BufferedWriter(writer);
			buffer.write(content);

			buffer.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return content;
	}

	public static long getFileLength(String filePathName) {
		File arquivo = new File(filePathName);  
		return arquivo.length();  
	}

	public static String getIntervalBeginning(String xquery){

		int posPositionFunction = xquery.indexOf("[position() ");
		String intervalBeginning = "";

		if ( posPositionFunction != -1 ) { // houve fragmentacao, pois ha cardinalidade 1:N entre os elementos.

			String subXquery = xquery.substring(posPositionFunction, xquery.length());			
			int posEqualsSymbol = subXquery.indexOf("=");
			int finalIntervalSpecification = ( subXquery.indexOf(" and") == -1? subXquery.indexOf("]"): subXquery.indexOf(" and") ); 
			intervalBeginning = subXquery.substring(posEqualsSymbol+2, finalIntervalSpecification); // soma dois para suprimir o caracter = e o espaco em branco
		}

		return intervalBeginning;
	}

	public static String getIntervalEnding(String xquery){

		int posPositionFunction = xquery.lastIndexOf("position() ");
		String intervalEnding = "";

		if ( posPositionFunction != -1 ) { // houve fragmentacao, pois ha cardinalidade 1:N entre os elementos.		
			String subXquery = xquery.substring(posPositionFunction, xquery.length());
			subXquery = subXquery.substring(0, subXquery.indexOf("]")+1);

			// se possui simbolo <, o fragmento tem tamanho maior que 1,caso contrario, é um fragmento unitário.
			int posSymbol = ( subXquery.indexOf("<") != -1? subXquery.indexOf("<"): subXquery.indexOf("=") );			

			int finalIntervalSpecification = subXquery.indexOf("]");
			intervalEnding = subXquery.substring(posSymbol+2, finalIntervalSpecification);
		}		

		return intervalEnding;
	}
}

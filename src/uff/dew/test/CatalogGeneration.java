package uff.dew.test;

import java.io.FileOutputStream;

import uff.dew.svp.catalog.Catalog;
import uff.dew.avp.AVPConst;

public class CatalogGeneration {
	
	public static void main(String args[]){
		
		if (args.length < 1) {
			System.out.println("Usage: CatalogGenerator <resource> [<resource> ...]\n\n"
					+ "<resource> is a XML file or directory containing XML files");
			System.exit(0);
		}

		try {
			System.out.println("Generating catalog in " + AVPConst.CATALOG_FILE_PATH+AVPConst.TPCH_CATALOG_FILE_NAME + " ...");
			Catalog cg = Catalog.get();
			long startTime = System.nanoTime();
			cg.createCatalogFromRawResources(args);
			cg.saveCatalog(new FileOutputStream(AVPConst.CATALOG_FILE_PATH+AVPConst.TPCH_CATALOG_FILE_NAME));
			long elapsedTime = System.nanoTime() - startTime;
			//cg.saveCatalog(System.out);
			System.out.println("Done!");
			System.out.println("Time elapsed: " + elapsedTime/1000000 + "ms");
			
			//cg.setDbMode(true);
			System.out.println(cg.getCardinality("Orders/Order", "orders.xml", null));
		}
		catch(Exception e) {
			System.err.println("Error during Catalog creation: " + e.getMessage());
			System.exit(1);
		}
	}
}

//public static void main(String args[]){
//
//	testConstructFromFile();
//}
//
//public static void testConstructFromFile() {
//
//	String[] resources = {"c:\\Users\\lzomatos\\Downloads\\xmark-standard.xml"};
//
//	Catalog c = Catalog.get();
//	try {
//		c.createCatalogFromRawResources(resources);
//		c.saveCatalog(System.out);
//	} catch (Exception e) {
//		e.printStackTrace();
//		System.out.println("Error during catalog creation from a single XML file.");
//	}
//}
//
//public void testConstructFromDirectory() {
//
//	String[] resources = {"c:\\Users\\lzomatos\\Downloads\\"};
//
//	Catalog c = Catalog.get();
//	try {
//		c.createCatalogFromRawResources(resources);
//		c.saveCatalog(System.out);
//	} catch (Exception e) {
//		e.printStackTrace();
//		System.out.println("Error during catalog creation from a single XML file.");
//	}
////}

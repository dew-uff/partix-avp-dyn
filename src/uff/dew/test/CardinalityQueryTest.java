package uff.dew.test;

import uff.dew.avp.AVPConst;
import uff.dew.avp.commons.Utilities;
import uff.dew.svp.catalog.Catalog;
import uff.dew.svp.db.DatabaseException;

public class CardinalityQueryTest {

	public static void main(String args[]) throws DatabaseException {
		Catalog catalog = Catalog.get();
		
		int cardinality = 0;

		//obtém a cardinalidade do atributo de fragmentação, sendo que primeiramente deve obter xpath da subconsulta 0
		String fragment_0 = Utilities.getFileContent(AVPConst.SUBQUERIES_FILE_PATH, AVPConst.SUBQUERIES_FILE_NAME + "_0.txt");
		String xpath = fragment_0.substring(fragment_0.indexOf("')/")+3, fragment_0.indexOf("["));
		System.out.println("fragment: " + fragment_0);
		System.out.println(xpath + AVPConst.CATALOG_FILE_PATH+AVPConst.TPCH_CATALOG_FILE_NAME);
		System.out.println(catalog.getCardinality(xpath, "orders.xml", null));
		cardinality = catalog.getCardinality("Orders/Order/", null, null);
		System.out.println("Card. do atr. de frag.: " + cardinality);
	}
}

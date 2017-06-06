package uff.dew.svp.strategy;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.xquery.XQException;

/**
 * @author gabriel
 *
 */
public interface CompositionStrategy {
    
    public void loadPartial(InputStream partial) throws IOException;

    //Luiz Matos - o retorno são os tempos de exec da consulta final e da gravacao em disco do resultado final
    public String combinePartials2(String tempCollection) throws IOException;
    
    public void combinePartials() throws IOException;
    
    public void cleanup();

    //Luiz Matos - para o caso em que usamos somente a colecao, sem passar pelos filesystem
	public void loadPartial(String collectionName) throws IOException;

	public boolean existsCollection(String collectionName) throws XQException;

}

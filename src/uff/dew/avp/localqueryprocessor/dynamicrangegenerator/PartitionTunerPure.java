/*
 * PartitionTunerStartSmall.java
 *
 * Created on 6 février 2004, 14:35
 */

package uff.dew.avp.localqueryprocessor.dynamicrangegenerator;

/**
 *
 * @author  lima
 */

import java.io.PrintStream;

 
/**
    Tune starting with only one key. At each iteraction, double. Stops when the estimated time is worse. 
    Only one size is tested at each time
 */

public class PartitionTunerPure extends PartitionTuner {

    /**
	 * 
	 */
	private static final long serialVersionUID = 3691042054025197108L;
	// PartitionTuner States
    private final int INITIAL = 0;
    private final int SEARCHING = 1;
    private final int SET = 2;
    private final int RESTARTING = 3;
    
    private final int NUMBER_OF_EXECUTIONS = 1; // number of executions to test each different size
//    private final int INITIAL_NUMBER_OF_KEYS = 10240; // number of keys to the first size that will be tested

    //Luiz Matos
    // *** AVP-DYN ***
    //utilizado no cálculo do intervalo inicial, sendo:
    // 0.02*cardinalidade = 1% da cardinalidade do subintervalo entregue para cada nó
    // 0.05*cardinalidade = 5%, 0.1*cardinalidade = 10%, 0.2*cardinalidade = 20%
    private final float INITIAL_TAX_OF_KEYS = (float) 0.1;
    private int a_lastNumberOfKeys = 0; //para guardar o ultimo tamanho de intervalo utilizado antes da oferta de ajuda
    private boolean hasOfferHelp=false; //usado para identificar se já executou 1 vez e agora trata-se de oferta de ajuda (alterado no método reset())
    
    private final float INITIAL_SIZE_GROWTH_TAX = (float) 1.0;
    private final float RESTART_SIZE_GROWTH_TAX = INITIAL_SIZE_GROWTH_TAX * (float) 0.2; 
    private final float REDUCTION_IN_SIZE_GROWTH_TAX = (float) 0.75;
    private final float MIN_SIZE_GROWTH_TAX = (float) 0.1;
    private final float SIZE_REDUCTION_WHILE_RESTARTING = (float) 0.05;
    
    private final int MAX_CHANCES_FOR_SET = 3;

    
    private final float MEANTIME_GROWTH_TOLERANCE = (float) 0.25; // to be applied over the "size increasing tax" being used
    private final float MEANTIME_SET_TOLERANCE = (float) 0.1; // to be used while evaluating current size
    
    private RangeStatistics a_rangeStatistics; // to help calculating partition sizes in terms of number of key values and ratio

    private int a_state; // To store tuner state

    private int a_currentNumKeys; // currently used size (in terms of keys)
    private float a_timeCurrentNumKeys; // execution time of the best number of keys
    //private int a_greatestNumKeys; // greatest number of keys already tested
    private float a_sizeGrowthTax; // growth tax currently being used
    private int a_numTimesSetWorse; // number of times the mean exec time was worse than when it was SET
    //private float a_meanExecTimeBeforeRestart; // mean execution time before RESTARTING state
    
    // for max size
    private int a_maxNumberOfKeys; // to establish a limit over the partition size
    private int a_numTimesMaxWouldNotBeRespected; // only statistics
    
    // for re-search
    private int a_numGreatestWasMet; // statistics
    private int a_numReTunings; // statistics
    private int a_numSets; // statistics
    
    // Public  Methods ----------------------------------------------
    
    /** Creates a new instance of PartitionTuner */
    public PartitionTunerPure(RangeStatistics rangeStatistics, int cardinality) {
    	//System.out.println("Criando instância de PartitionTunerPure ...");
        a_state = INITIAL;
        a_rangeStatistics = rangeStatistics;
       
        //a_maxNumberOfKeys = calculateNumberOfKeys( a_rangeStatistics.getMaxSizeRatio() );
        setMaxNumberOfKeys(cardinality);
        a_numTimesMaxWouldNotBeRespected = 0;
        a_numGreatestWasMet = 0;
        a_numReTunings = 0;  

    }
    
    public boolean stillTuning() {
        return true; // always tuning
    }

    // Called by QueryExecutor when finished testing one size
    // When using many QueryExecutors, make this method SYNCHRONIZED
    public void setSizeResults( PartitionSize size ) throws IllegalThreadStateException {
//    	System.out.println("Entrei no metodo setSizeResults ....");
//    	System.out.println("size.numberOfKeys() = " + size.numberOfKeys());
//    	System.out.println("size.getExecutionTime() (float) = " + size.getPureExecutionTime());
//    	System.out.println("(long)size.getExecutionTime() (nano) = " + (long)size.getPureExecutionTime()*1000000);
//    	System.out.println("(long)size.getExecutionTime()  (ms) = " + (long)size.getPureExecutionTime());
//
//    	System.out.println("a_currentNumKeys (before) = " + a_currentNumKeys);
//    	System.out.println("a_timeCurrentNumKeys (before) = " + a_timeCurrentNumKeys);
//    	System.out.println("a_sizeGrowthTax (before) = " + a_sizeGrowthTax);
        switch( a_state ) {
            case INITIAL: {
                throw new IllegalThreadStateException( "Some object is trying to set results before first test" );
            }
            case SEARCHING: {
                if( a_currentNumKeys == 0) {
                    a_currentNumKeys = size.numberOfKeys(); // first set operation
                    a_timeCurrentNumKeys = size.getPureExecutionTime();
                    a_sizeGrowthTax = INITIAL_SIZE_GROWTH_TAX;
//                    if( a_currentNumKeys == a_maxNumberOfKeys ) {
//                        a_state = SET; // Max size. Cannot grow.
//                        a_numTimesSetWorse = 0;
//                    }
                }
                else if( size.getPureExecutionTime() <= (a_timeCurrentNumKeys * (2.0 + (MEANTIME_GROWTH_TOLERANCE * a_sizeGrowthTax) ) ) ) {
                    // Size is better than best. Change best. Continue searching.
                    a_currentNumKeys = size.numberOfKeys();
                    a_timeCurrentNumKeys = size.getPureExecutionTime();
//                    if( a_currentNumKeys == a_maxNumberOfKeys ) {
//                        a_state = SET; // Max size. Cannot grow.
//                        a_numTimesSetWorse = 0;
//                    }
                } else {
                    // Size is worse than best. Reduce growth tax.
                    a_sizeGrowthTax = a_sizeGrowthTax * ((float)1.0 - REDUCTION_IN_SIZE_GROWTH_TAX);
                    if( a_sizeGrowthTax < MIN_SIZE_GROWTH_TAX ) {
                        a_state = SET;
                        a_numTimesSetWorse = 0;
                    }
                }
                break;
            }
            case SET: {                
                if( size.numberOfKeys() != a_currentNumKeys )
                    throw new IllegalArgumentException( "Size was expected to be equal to current. current = " + a_currentNumKeys + ". Size = " + size.numberOfKeys() );
                a_numSets++;
                if( size.getPureExecutionTime() > (a_timeCurrentNumKeys * (1.0 + MEANTIME_SET_TOLERANCE) ) ) {
                    a_numTimesSetWorse++;
                    //if( a_numTimesSetWorse == 1 )
                    //    a_meanExecTimeBeforeRestart = a_meanTimeCurrentNumKeys;
                    if (a_numTimesSetWorse > MAX_CHANCES_FOR_SET) {
                        a_state = RESTARTING; // Best size is going worse. Re-Start tuning.
                        a_numReTunings++;
                        a_numTimesSetWorse = 0;
                    }
                }
                else {
                    a_numTimesSetWorse = 0;
                    if( size.getPureExecutionTime() < a_timeCurrentNumKeys )
                        a_timeCurrentNumKeys = size.getPureExecutionTime();
                }
                break;
            }
            case RESTARTING: {
                a_currentNumKeys = size.numberOfKeys(); // first set operation
                a_timeCurrentNumKeys = size.getPureExecutionTime();
                if( a_currentNumKeys == a_maxNumberOfKeys ) { 
//                    a_state = SET; // Max size. Cannot grow.
//                    a_numTimesSetWorse = 0;
                }   
                else {
                    a_state = SEARCHING;
                    a_sizeGrowthTax = RESTART_SIZE_GROWTH_TAX;
                }
                break;
            }
            default: 
                throw new IllegalThreadStateException( "PartitionTuner at an unknown state: " + a_state );
        }
        
//        System.out.println("a_currentNumKeys (after) = " + a_currentNumKeys);
//    	System.out.println("a_timeCurrentNumKeys (after) = " + a_timeCurrentNumKeys);
//    	System.out.println("a_sizeGrowthTax (after) = " + a_sizeGrowthTax);
    }
    
    // Called by QueryExecutor when demanding a new partition size to use
    // When using many QueryExecutors, make this method SYNCHRONIZED
    public PartitionSize getPartitionSize() throws IllegalThreadStateException {
    	//System.out.println("Entrei no metodo getPartitionSize ...");
    	
        PartitionSize size = null;
        switch( a_state ) {
            case INITIAL: {
                a_currentNumKeys = 0;
                a_timeCurrentNumKeys = 0;
             
                //System.out.println("!!!!!!! getMaxNumberOfKeys()*INITIAL_TAX_OF_KEYS = " + getMaxNumberOfKeys() + "*" + INITIAL_TAX_OF_KEYS);
                
                //a_greatestNumKeys = 0;
                if (hasOfferHelp == false) //na primeira execução
                	size = new PartitionSize( calculateRatio( (getMaxNumberOfKeys()*INITIAL_TAX_OF_KEYS )), (int) (getMaxNumberOfKeys()*INITIAL_TAX_OF_KEYS), NUMBER_OF_EXECUTIONS );
                else //na segunda execução em diante - quando usa offer-help
                	size = new PartitionSize( calculateRatio( (getLastNumberOfKeys() )), (int) (getLastNumberOfKeys()), NUMBER_OF_EXECUTIONS );
                //System.out.println("hasOfferHelp - " + hasOfferHelp + "  !!! size = " + size);
                a_state = SEARCHING;
                break;
            }
            case SET: {
                size = new PartitionSize( calculateRatio( a_currentNumKeys ), a_currentNumKeys, 1 );
                break;
            }
            case SEARCHING: {
                int numkeys;
                numkeys = (int)Math.ceil( (double)a_currentNumKeys * (1.0 + a_sizeGrowthTax) );
                
//                if( numkeys > a_maxNumberOfKeys ) {
//                    numkeys = a_maxNumberOfKeys;
//                    a_numTimesMaxWouldNotBeRespected++;                            
//                }
                
                size = new PartitionSize( calculateRatio( numkeys ), numkeys, NUMBER_OF_EXECUTIONS );
                break;
            }
            case RESTARTING: {
                int numkeys;
                numkeys = (int)Math.ceil( (double)a_currentNumKeys / (1.0 + SIZE_REDUCTION_WHILE_RESTARTING ) );
                if( numkeys < 1 )
                    numkeys = 1;
                size = new PartitionSize( calculateRatio( numkeys ), numkeys, NUMBER_OF_EXECUTIONS );
                break;                
            }
            default: 
                throw new IllegalThreadStateException( "PartitionTuner at an unknown state: " + a_state );
        }

        /*
        if( size.numberOfKeys() > a_greatestNumKeys )
            a_greatestNumKeys = size.numberOfKeys();
         */
        //System.out.println("size = " + size);
        return size;
    }
    
    public void printTuningStatistics( PrintStream out ) {
        out.println( "Upper limit for the number of keys: " + a_maxNumberOfKeys );
        out.println( "Number of Re-Searchs do to greatest size being met: " + a_numGreatestWasMet );
        out.println( "Number of Re-Tuning operations: " + a_numReTunings );
        out.println( "Number of SET states: " + a_numSets );
        out.println( "The limit would not be respected " + a_numTimesMaxWouldNotBeRespected + " times.");
        
    }
       
    // Private Methods ----------------------------------------------
    
    private float calculateRatio( float numberOfKeys ) {
    	//System.out.println("Entrei no metodo calculateRatio ...");
    	//System.out.println("numberOfKeys = " + numberOfKeys);
    	//System.out.println("a_rangeStatistics.getMeanNumTuplesPerValue() = " + a_rangeStatistics.getMeanNumTuplesPerValue());
    	//System.out.println("a_rangeStatistics.getTotalNumberOfTuples() = " + a_rangeStatistics.getTotalNumberOfTuples());
//    	
    	//System.out.println("calculateRatio = " + (float) ((((float)numberOfKeys) * a_rangeStatistics.getMeanNumTuplesPerValue() ) / a_rangeStatistics.getTotalNumberOfTuples() ));
        return (float) ((((float)numberOfKeys) * a_rangeStatistics.getMeanNumTuplesPerValue() ) / a_rangeStatistics.getTotalNumberOfTuples() );
    } 
    
    private int calculateNumberOfKeys( float ratio ) {
    	//System.out.println("Entrei no metodo calculateNumberOfKeys ...");
    	//System.out.println("ratio = " + ratio);
    	//System.out.println("a_rangeStatistics.getTotalNumberOfTuples() = " + a_rangeStatistics.getTotalNumberOfTuples());
    	//System.out.println("a_rangeStatistics.getMeanNumTuplesPerValue() = " + a_rangeStatistics.getMeanNumTuplesPerValue());
    	//System.out.println("calculateNumberOfKeys = " + (int) ((ratio * a_rangeStatistics.getTotalNumberOfTuples() ) / a_rangeStatistics.getMeanNumTuplesPerValue()));
        return (int) ( (ratio * a_rangeStatistics.getTotalNumberOfTuples() ) / a_rangeStatistics.getMeanNumTuplesPerValue());
    }
    
    public void reset() {
        a_state = INITIAL;
        hasOfferHelp = true; // setado para true após 1a. execução, já que agora trata-se de offer-help
    }
    
    public int getMaxNumberOfKeys() {
    	return this.a_maxNumberOfKeys;
    }
    
  //utilizado no AVP-DYN para guardar o último intervalo utilizado antes da oferta de ajuda
    public void setLastNumberOfKeys(int lastNumberOfKeys) {
    	this.a_lastNumberOfKeys = lastNumberOfKeys;
    }
    
    public int getLastNumberOfKeys() {
    	return this.a_lastNumberOfKeys;
    }
    
    public void setMaxNumberOfKeys(long newTotalTableTuples) {
//    	a_rangeStatistics.setTotalNumberOfTuples(newTotalTableTuples);
    	if (hasOfferHelp == false)
    		this.a_maxNumberOfKeys = calculateNumberOfKeys( 1 );
    	else
    		this.a_maxNumberOfKeys = calculateNumberOfKeys( a_rangeStatistics.getMaxSizeRatio() );
//    	System.out.println("newTotalTableTuples = " + newTotalTableTuples);
//    	this.a_maxNumberOfKeys = calculateNumberOfKeys( a_rangeStatistics.getTotalNumberOfTuples() );
    	//System.out.println("newTotalTableTuples = " + newTotalTableTuples);
    }
}

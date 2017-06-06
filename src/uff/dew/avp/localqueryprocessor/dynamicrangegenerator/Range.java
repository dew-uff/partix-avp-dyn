package uff.dew.avp.localqueryprocessor.dynamicrangegenerator;

import java.io.Serializable;

/**
 * 
 * @author lima
 */
//Essa classe controla as posições e parâmetros utilizados na consulta original, ainda na etapa do SVP
public class Range implements Serializable {

	private static final long serialVersionUID = 4051322344863578417L;

	private int idOwner;
	private int firstValue; 
	// last value initially supplied. It can change due to dynamic load balancing
	private int originalLastValue;
	// current last value. Can be different from the one initially supplied due
	// to dynamic load balancing
	private int currentLastValue;
	// last processed value + 1
	private int currentValue;
	// query arguments that must receive the first value
	private int[] argumentFirstValue;
	// query arguments that must receive the last value
	private int[] argumentLastValue;
	// Number of virtual partitions into which this interval should be divided
	private int numVPs;
	// Virtual partition size. Calculated according to a_numVPs
	private int vpSize;
	// Used for partition size modulation. Not to be changed by Range.
	private RangeStatistics statistics;

	private String field;
	private String tableName;
	private long rangeInit;
	private long rangeEnd;
	private long cardinality;
	private String rangeMin;
	private String rangeMax;

	/** Creates a new instance of Range */
	public Range(int idOwner, int firstValue, int lastValue, int[] argumentFirstValue, int[] argumentLastValue, int numVPs, RangeStatistics statistics) {
		if (firstValue > lastValue) {
			throw new IllegalThreadStateException("Invalid range ("+ firstValue + "," + lastValue + ").");
		} else {
			this.idOwner = idOwner;
			this.firstValue = firstValue;
			originalLastValue = lastValue;
			currentLastValue = originalLastValue;
			currentValue = this.firstValue;
			this.argumentFirstValue = argumentFirstValue;
			this.argumentLastValue = argumentLastValue;
			this.numVPs = numVPs;
			vpSize = (originalLastValue - this.firstValue) / this.numVPs;
			this.statistics = statistics;
			return;
		}
		//System.out.println("Estou no construtor da classe Range ...");
	}

	public Range(int idOwner, int firstValue, int lastValue, int[] argumentFirstValue, int[] argumentLastValue, int numVPs) {
		this(idOwner, firstValue, lastValue, argumentFirstValue, argumentLastValue, numVPs, null);
	}
	
	//Usado no caso de receber o intervalo do fragmento de um único nó
	public Range(int idOwner, int firstValue, int lastValue, int numVPs) {
		if (firstValue > lastValue) {
			throw new IllegalThreadStateException("Invalid range ("+ firstValue + "," + lastValue + ").");
		} else {
			this.idOwner = idOwner;
			this.firstValue = firstValue;
			originalLastValue = lastValue;
			currentLastValue = originalLastValue;
			currentValue = this.firstValue;
			this.numVPs = numVPs;
			vpSize = (originalLastValue - this.firstValue) / this.numVPs;
			return;
		}
		
	}

	public Range(String field, String tableName, long rangeInit, long rangeEnd, long cardinality) {
		this.field = field;
		this.tableName = tableName;
		this.rangeInit = rangeInit;
		this.rangeEnd = rangeEnd;
		this.cardinality = cardinality;
	}

	public int getFirstValue() {
		return firstValue;
	}

	public int getOriginalLastValue() {
		return originalLastValue;
	}

	public synchronized int getCurrentLastValue() {
		return currentLastValue;
	}

	public int getNumberArgumentsFirstValue() {
		return argumentFirstValue.length;
	}

	public int getArgumentFirstValue(int argNumber) {
		return argumentFirstValue[argNumber];
	}

	public int[] getAllArgumentsFirstValue() {
		return argumentFirstValue;
	}

	public int getNumberArgumentsLastValue() {
		return argumentLastValue.length;
	}

	public int getArgumentLastValue(int argNumber) {
		return argumentLastValue[argNumber];
	}

	public int[] getAllArgumentsLastValue() {
		return argumentLastValue;
	}

	public int getNumVPs() {
		return numVPs;
	}

	public int getVPSize() {
		return vpSize;
	}

	public RangeStatistics getStatistics() {
		return statistics;
	}

	public synchronized int getNextValue(int intervalSize) {
		if (currentValue < currentLastValue) {
			currentValue += intervalSize;
			if (currentValue > currentLastValue)
				currentValue = currentLastValue;
		} else if (currentValue > currentLastValue) {
			// a_currentValue > a_currentLastValue
			throw new IllegalThreadStateException("LocalQueryInterval Exception: Current value (" + currentValue + ") is greater then current last value (" + currentLastValue + "). It can cause errors on final result");
		}
		// if( a_currentValue == a_currentLastValue ) --> Interval limit
		// reached. Cannot continue growing. Return the same value.
		notifyAll();
		return currentValue;
	}

	public synchronized void reset(int idOwner, int firstValue, int lastValue) {
		this.idOwner = idOwner;
		this.firstValue = firstValue;
		this.originalLastValue = lastValue;
		this.currentLastValue = this.originalLastValue;
		this.currentValue = this.firstValue;
		this.vpSize = (this.originalLastValue - this.firstValue) / this.numVPs;
		notifyAll();
	}

	//Método utilizado quando um nó irá receber ajuda de outro
	public synchronized Range cutTail(float fraction) {
		// Cut a fraction of the not processed interval starting from its end
		Range tail;

		if (currentValue == currentLastValue) {
			// there is nothing else to be done!
			tail = null;
		} else {
			int newIntervalFirstValue, newIntervalLastValue;

			newIntervalFirstValue = currentLastValue - (int) Math.ceil((currentLastValue - currentValue) * fraction);
			if (newIntervalFirstValue == currentLastValue)
				tail = null;
			else {
				if (newIntervalFirstValue < currentValue) {
					// all non-processed interval was caught
					newIntervalFirstValue = currentValue;
				}
				newIntervalLastValue = currentLastValue;
				currentLastValue = newIntervalFirstValue;
				//tail = new Range(idOwner, newIntervalFirstValue, newIntervalLastValue, argumentFirstValue, argumentLastValue, numVPs, statistics);
				tail = new Range(1, newIntervalFirstValue, newIntervalLastValue, this.numVPs); // checar, LUIZ MATOS
				if (currentValue == currentLastValue)
					if (firstValue == currentLastValue) {
						// This situation can occurr when a node received an
						// interval and, before starting processing it,
						// received help. The interval was so small that it has
						// been entirely given to another node.
						System.out.println("I think this interval has not been processed here: First Value = "
								+ firstValue
								+ "; Current Value = "
								+ currentValue
								+ "; Current Last Value = "
								+ currentLastValue
								+ "; Original Last Value = "
								+ originalLastValue);
					}
			}
		}
		notifyAll();
		return tail;
	}

	public synchronized float getUnprocessedFraction() {
		// return the fraction of the interval that still needs to be processed
		float fraction = (float) (currentLastValue - currentValue)
				/ (float) (currentLastValue - firstValue);
		notifyAll();
		return fraction;
	}

	public int getIdOwner() {
		return idOwner;
	}

	public void setFirstValue(int firstValue) {
		this.firstValue = firstValue;
	}

	public void setOriginalLastValue(int originalLastValue) {
		this.originalLastValue = originalLastValue;
	}

	public String getField() {
		return field;
	}
	public long getRangeEnd() {
		return rangeEnd;
	}
	public long getRangeInit() {
		return rangeInit;
	}
	public String getTableName() {
		return tableName;
	}

	public long getCardinality() {
		return cardinality;
	}

	public void setRangeEnd(long rangeEnd) {
		this.rangeEnd = rangeEnd;
	}

	public void setRangeInit(long rangeInit) {
		this.rangeInit = rangeInit;
	}

	public String getRangeMax() {
		return rangeMax;
	}

	public void setRangeMax(String rangeMax) {
		this.rangeMax = rangeMax;
	}

	public String getRangeMin() {
		return rangeMin;
	}

	public void setRangeMin(String rangeMin) {
		this.rangeMin = rangeMin;
	}

}

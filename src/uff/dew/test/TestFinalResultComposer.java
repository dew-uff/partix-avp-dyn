package uff.dew.test;

public class TestFinalResultComposer {
	private static FinalResultComposerTest frc;
	
	public static void main(String args[]) {
		frc = new FinalResultComposerTest();
		//frc.testExecuteFinalCompositionRegular();
		//frc.testExecuteFinalCompositionAggregation();
		//frc.testExecuteFinalCompositionRegularForceTempCollectionMode();
		frc.testExecuteFinalOnlyTempCollectionMode();
	}
}

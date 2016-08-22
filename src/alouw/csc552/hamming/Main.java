package alouw.csc552.hamming;

import java.util.concurrent.TimeUnit;

import ajeffrey.teaching.debug.Debug;

public class Main {

	public static final boolean DEBUG = false;
	public static final long MAX_SOLUTION_DURATION = 10;
	public static final TimeUnit MAX_SOLUTION_DURATION_UOM = TimeUnit.MINUTES;
	public static final int NUM_HAMMING_NUMBERS = 60;
	
	public static void main(String[] args) {
		
		if (DEBUG) Debug.out.addPrintStream (System.err);
		
		final long startTime = System.currentTimeMillis();
		
		// create & configure a network to produce an ordered sequence of Hamming Numbers lte some threshold value 	
		// time to generate solution cannot exceed arguments
		HammingNetwork network = HammingNetwork.INSTANCE;
		network.configure(NUM_HAMMING_NUMBERS, MAX_SOLUTION_DURATION, MAX_SOLUTION_DURATION_UOM);
		
		// generate the numbers & block until complete
		network.start();
		
		final long endTime = System.currentTimeMillis();
		
		System.out.println("");
		System.out.println("Duration: " + (endTime - startTime)/60.0 + " Seconds");
	}
}
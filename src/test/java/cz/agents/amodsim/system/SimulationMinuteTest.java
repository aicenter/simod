/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.system;

import org.junit.Test;

/**
 *
 * @author fido
 */
public class SimulationMinuteTest {
	
	private static final int ONE_MINUTE_IN_MILIS = 60000;
	
	// we expect trips to be no longer then 40 minutes
	private static final int TRIP_MAX_DURATION = 2400000;
	
	private static final int START_TIME_MILIS = 25200000;
	
	@Test
	public void run(){
		FullTest.runFullTest(ONE_MINUTE_IN_MILIS, START_TIME_MILIS, TRIP_MAX_DURATION);
	}
}

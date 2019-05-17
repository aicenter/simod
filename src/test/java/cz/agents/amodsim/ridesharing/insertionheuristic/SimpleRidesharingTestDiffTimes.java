/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.insertionheuristic;


import cz.agents.amodsim.ridesharing.insertionheuristic.common.InsertionHeuristicTestEnvironment;
import cz.agents.amodsim.ridesharing.scenarios.SimpleRidesharingDiffTimes;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import org.junit.Test;

/**
 *
 * @author David Fiedler
 */
public class SimpleRidesharingTestDiffTimes {
	
	@Test
    public void run() throws Throwable{
		new SimpleRidesharingDiffTimes().run(new InsertionHeuristicTestEnvironment());
    }
	
	public static void main(String[] args) {
        VisualTests.runVisualTest(SimpleRidesharingTestDiffTimes.class);
    }
}

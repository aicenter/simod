/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga;

import cz.agents.amodsim.ridesharing.scenarios.Weight1;
import cz.agents.amodsim.ridesharing.vga.common.VGASystemTestScenario;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import org.junit.Test;

/**
 *
 * @author David Fiedler
 */
public class WeightTestWeight1 {
	
	@Test
	public void run() throws Throwable{
		new Weight1().run(new VGASystemTestScenario());
	}
	
	public static void main(String[] args) {
		VisualTests.runVisualTest(WeightTestWeight1.class);
	}
}

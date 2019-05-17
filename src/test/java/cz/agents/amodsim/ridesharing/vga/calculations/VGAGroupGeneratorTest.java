/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.agents.amodsim.ridesharing.vga.calculations;

import com.google.inject.Injector;
import cz.agents.amodsim.ridesharing.vga.common.VGASystemTestScenario;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GroupGenerator;
import ninja.fido.config.Configuration;
import org.junit.BeforeClass;

/**
 *
 * @author F.I.D.O.
 */
public class VGAGroupGeneratorTest {
	
	private GroupGenerator groupGenerator;
	
	@BeforeClass
	public void prepare(){
		// bootstrap Guice
		VGASystemTestScenario scenario = new VGASystemTestScenario();
		Injector injector = scenario.getInjector();
		
		AgentpolisConfig agentpolisConfig = new AgentpolisConfig();
		AmodsimConfig amodsimConfig = new AmodsimConfig();
		Configuration.load(agentpolisConfig, amodsimConfig, "agentpolis");
		
		StandardPlanCostProvider planCostComputation = new StandardPlanCostProvider(amodsimConfig);
		
//		groupGenerator = new VGAGroupGenerator(planCostComputation, amodsimConfig);
	}
	
	
	public void testOptimalPlanGenretion(){
		
	}
}

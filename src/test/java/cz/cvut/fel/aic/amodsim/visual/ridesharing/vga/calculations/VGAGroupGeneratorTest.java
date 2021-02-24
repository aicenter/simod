/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.calculations;

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.StandardPlanCostProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GroupGenerator;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.common.VGASystemTestEnvironment;
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
		VGASystemTestEnvironment scenario = new VGASystemTestEnvironment();
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

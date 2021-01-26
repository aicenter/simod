/* 
 * Copyright (C) 2019 Czech Technical University in Prague.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic;

import cz.cvut.fel.aic.amodsim.ridesharing.insertionheuristic.common.InsertionHeuristicTestEnvironment;
import cz.cvut.fel.aic.amodsim.ridesharing.scenarios.Weight1Scenario;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import org.junit.Test;

/**
 *
 * @author David Fiedler
 */
public class WeightTestWeight1 {
	
	@Test
	public void run() throws Throwable{
		new Weight1Scenario().run(new InsertionHeuristicTestEnvironment());
	}
	
	public static void main(String[] args) {
		VisualTests.runVisualTest(WeightTestWeight1.class);
	}
}

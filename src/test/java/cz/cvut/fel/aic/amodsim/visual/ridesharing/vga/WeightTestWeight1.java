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
package cz.cvut.fel.aic.amodsim.visual.ridesharing.vga;

import cz.cvut.fel.aic.agentpolis.VisualTests;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.scenarios.Weight1Scenario;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.common.VGASystemTestEnvironment;
import org.junit.Test;

/**
 *
 * @author David Fiedler
 */
public class WeightTestWeight1 {
	
	@Test
	public void run() throws Throwable{
		new Weight1Scenario().run(new VGASystemTestEnvironment());
	}
	
	public static void main(String[] args) {
		VisualTests.runVisualTest(WeightTestWeight1.class);
	}
}

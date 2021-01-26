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
package cz.cvut.fel.aic.amodsim.visual.ridesharing.vga;

import cz.cvut.fel.aic.amodsim.visual.ridesharing.scenarios.CapacityScenario;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.common.VGASystemTestEnvironment;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import org.junit.Test;

/**
 *
 * @author David Fiedler
 */
public class CapacityTest {
	
	@Test
	public void run() throws Throwable{
		new CapacityScenario().run(new VGASystemTestEnvironment());
	}
	
	public static void main(String[] args) {
		VisualTests.runVisualTest(CapacityTest.class);
	}
}
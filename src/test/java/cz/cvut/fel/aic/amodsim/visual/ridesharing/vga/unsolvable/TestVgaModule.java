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
package cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.unsolvable;

import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.vga.common.TestModule;
import java.io.File;

/**
 *
 * @author fido
 */
public class TestVgaModule extends TestModule{
	
	private final AmodsimConfig amodsimConfig;

	public TestVgaModule(AmodsimConfig amodsimConfig, File localConfigFile) {
		super(amodsimConfig, localConfigFile); 
		this.amodsimConfig = amodsimConfig;
                this.amodsimConfig.ridesharing.batchPeriod = 10;
                this.amodsimConfig.ridesharing.weightParameter = 0.6;
                this.amodsimConfig.ridesharing.maximumRelativeDiscomfort = 1.3;                                         
                this.amodsimConfig.ridesharing.maxProlongationInSeconds = 60;
//                this.amodsimConfig.ridesharing.vga.exportGroupData
                        
                this.amodsimConfig.ridesharing.vga.groupGeneratorLogFilepath = new File("").getAbsolutePath();
                roadWidth = 80;
                
                
	}
	
}

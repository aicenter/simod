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
package cz.cvut.fel.aic.amodsim;

import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioInitializer;
import cz.cvut.fel.aic.agentpolis.system.StandardAgentPolisModule;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.visio.MapVisualizerVisioInitializer;
import java.io.File;

/**
 *
 * @author F.I.D.O.
 */
public class MapVisualiserModule  extends MainModule{

	private final AmodsimConfig amodsimConfig;
	
	public MapVisualiserModule(AmodsimConfig amodsimConfig, File localConfigFile) {
		super(amodsimConfig, localConfigFile, false);
		this.amodsimConfig = amodsimConfig;
	}
	
	@Override
	protected void bindVisioInitializer() {
		bind(VisioInitializer.class).to(MapVisualizerVisioInitializer.class);
	}
}

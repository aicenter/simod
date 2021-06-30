/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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
package cz.cvut.fel.aic.simod;


import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.MapInitializer;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.simod.config.SimodConfig;
import cz.cvut.fel.aic.simod.init.StationsInitializer;
import cz.cvut.fel.aic.simod.mapVisualization.MapVisualiserModule;
import cz.cvut.fel.aic.simod.mapVisualization.MapVisualizationCreator;
import java.io.File;
import java.net.MalformedURLException;
import org.slf4j.LoggerFactory;

/**
 * @author David Fiedler
 */
public class MapVisualizer {

	private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(MapVisualizer.class);
	
	public static void main(String[] args) throws MalformedURLException {
		new MapVisualizer().run(args);
	}


	public void run(String[] args) {
		SimodConfig config = new SimodConfig();
		
		File localConfigFile = args.length > 0 ? new File(args[0]) : null;
		
		Injector injector = new AgentPolisInitializer(new MapVisualiserModule(config, localConfigFile)).initialize();
		
		injector.getInstance(AgentpolisConfig.class).visio.showVisio = true;
                                
		MapVisualizationCreator creator = injector.getInstance(MapVisualizationCreator.class);

		// prepare map, entity storages...
		creator.prepareSimulation(injector.getInstance(MapInitializer.class).getMap());
		
		// load stations
		injector.getInstance(StationsInitializer.class).loadStations();


		creator.startSimulation();

	}
}

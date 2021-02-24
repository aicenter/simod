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

import com.google.inject.Injector;
import cz.cvut.fel.aic.agentpolis.VisualTests;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.agentpolis.simulator.creator.SimulationCreator;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicleFactorySpec;
import cz.cvut.fel.aic.amodsim.init.EventInitializer;
import cz.cvut.fel.aic.amodsim.io.TimeTrip;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.GurobiSolver;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.amodsim.visual.ridesharing.EventOrderStorage;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 *
 * @author travnja5
 */
public class VgaSolverFailTest {
    
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(GurobiSolver.class);
        
        @Test
        public void run() throws Throwable{                             
        //environment preparation
                AmodsimConfig config = new AmodsimConfig();		
                File localConfigFile = null;
            
            // Guice configuration
                AgentPolisInitializer agentPolisInitializer 
                                = new AgentPolisInitializer(new TestVgaModule(config, localConfigFile));
                Injector injector = agentPolisInitializer.initialize();
            
            //prepare grid
                Graph<SimulationNode, SimulationEdge> graph 
                                    = Utils.getGridGraph(5, injector.getInstance(Transformer.class), 5);            
                SimulationCreator creator = injector.getInstance(SimulationCreator.class);

            // prepare map
                SimpleMapInitializer mapInitializer = injector.getInstance(SimpleMapInitializer.class);
                mapInitializer.setGraph(graph);
                creator.prepareSimulation(mapInitializer.getMap());
            
            //trips
		List<TimeTrip<SimulationNode>> trips = new LinkedList<>();		                
		trips.add(new TimeTrip<>(0, 1000, graph.getNode(6), graph.getNode(15)));
                trips.add(new TimeTrip<>(1, 1000, graph.getNode(16), graph.getNode(22)));
                trips.add(new TimeTrip<>(2, 1000, graph.getNode(18), graph.getNode(14)));
                trips.add(new TimeTrip<>(3, 1000, graph.getNode(8), graph.getNode(2)));


                injector.getInstance(EventInitializer.class).initialize(trips, new ArrayList<>());
                
            //vehicles position
                List<SimulationNode> vehicalInitPositions = new LinkedList<>();
		vehicalInitPositions.add(graph.getNode(12));
            //creating vehicles
		OnDemandVehicleFactorySpec onDemandVehicleFactory = injector.getInstance(OnDemandVehicleFactorySpec.class);
		OnDemandVehicleStorage onDemandVehicleStorage = injector.getInstance(OnDemandVehicleStorage.class);
		int counter = 0;
		for (SimulationNode vehiclePosition: vehicalInitPositions) {
			String onDemandVehicelId = String.format("%s", counter);
			OnDemandVehicle newVehicle = onDemandVehicleFactory.create(onDemandVehicelId, vehiclePosition);
			onDemandVehicleStorage.addEntity(newVehicle);
			counter++;
		}
                
			EventOrderStorage eventOrderStorage = injector.getInstance(EventOrderStorage.class);

			creator.startSimulation();                                

			assertEquals(6, eventOrderStorage.getOnDemandVehicleEvents().size());

			LOGGER.info("Model saved to {}/model1.lp", config.ridesharing.vga.groupGeneratorLogFilepath);

			File file = new File(config.ridesharing.vga.groupGeneratorLogFilepath+"/model1.lp");
			Assert.assertTrue(file.exists());
			file.delete();                       
        }
        
        public static void main(String[] args) {
                VisualTests.runVisualTest(VgaSolverFailTest.class);
        }
}

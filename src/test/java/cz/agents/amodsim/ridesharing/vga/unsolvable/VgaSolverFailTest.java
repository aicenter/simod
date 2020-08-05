/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.unsolvable;

import com.google.inject.Injector;
import cz.agents.amodsim.ridesharing.EventOrderStorage;
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
		trips.add(new TimeTrip<>(1000, graph.getNode(6), graph.getNode(15)));
                trips.add(new TimeTrip<>(1000, graph.getNode(16), graph.getNode(22)));
                trips.add(new TimeTrip<>(1000, graph.getNode(18), graph.getNode(14)));
                trips.add(new TimeTrip<>(1000, graph.getNode(8), graph.getNode(2)));


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
                
                System.out.println(config.ridesharing.vga.groupGeneratorLogFilepath+"/model1.lp");
                
                
                File file = new File(config.ridesharing.vga.groupGeneratorLogFilepath+"/model1.lp");
                Assert.assertTrue(file.exists());
                file.delete();                       
        }
        
        public static void main(String[] args) {
                VisualTests.runVisualTest(VgaSolverFailTest.class);
        }
}

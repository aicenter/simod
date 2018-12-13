/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package cz.agents.amodsim.ridesharing.vga.calculations;

import com.google.inject.Injector;
import cz.agents.amodsim.ridesharing.vga.common.TestModule;
import cz.agents.amodsim.ridesharing.vga.common.TestModuleNoVisio;
import cz.agents.amodsim.ridesharing.vga.common.VGASystemTestScenario;
import cz.agents.amodsim.ridesharing.vga.mock.TestOptimalPlanVehicle;
import cz.agents.amodsim.ridesharing.vga.mock.TestPlanRequest;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.Utils;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationEdge;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.init.SimpleMapInitializer;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.ArrayOptimalVehiclePlanFinder;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.PlanCostComputation;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.Plan;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGARequest.VGARequestFactory;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanAction;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanDropoff;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehiclePlanPickup;
import cz.cvut.fel.aic.geographtools.Graph;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import ninja.fido.config.Configuration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author F.I.D.O.
 */
public class ArrayOptimalVehiclePlanFinderTest {
	
	private static ArrayOptimalVehiclePlanFinder arrayOptimalVehiclePlanFinder;
	
	private static Injector injector;
	
	private static AmodsimConfig config;
	
	@BeforeClass
	public static void prepare(){
		config = new AmodsimConfig();
        
        File localConfigFile = null;

        // Guice configuration
        AgentPolisInitializer agentPolisInitializer 
				= new AgentPolisInitializer(new TestModuleNoVisio(config, localConfigFile));
        injector = agentPolisInitializer.initialize();
		
		// config changes
		config.amodsim.ridesharing.vga.batchPeriod = 0;
		config.amodsim.ridesharing.vga.maximumRelativeDiscomfort = 2.1;
		
		MathUtils.setTravelTimeProvider(injector.getInstance(TravelTimeProvider.class));
		arrayOptimalVehiclePlanFinder = injector.getInstance(ArrayOptimalVehiclePlanFinder.class);
	}
	
	@Test
	public void testComputation(){
		// set roadgraph - grid 5x4
        Graph<SimulationNode, SimulationEdge> graph 
				= Utils.getGridGraph(5, injector.getInstance(Transformer.class), 4);
		injector.getInstance(SimpleMapInitializer.class).setGraph(graph);
		
		// prepare test requests
		Set<PlanComputationRequest> onBoardRequests = new LinkedHashSet<>();
		TestPlanRequest request0 = new TestPlanRequest(0, config, graph.getNode(16), graph.getNode(14), 3, false);
		TestPlanRequest request1 = new TestPlanRequest(1, config, graph.getNode(17), graph.getNode(3), 1, true);
		TestPlanRequest request2 = new TestPlanRequest(2, config, graph.getNode(5), graph.getNode(10), 4, false);
		TestPlanRequest request4 = new TestPlanRequest(4, config, graph.getNode(11), graph.getNode(0), 8, false);
		onBoardRequests.add(request1);
		
		// prepare actions
		List<VGAVehiclePlanAction> actions = new ArrayList<>();
		actions.add(new VGAVehiclePlanDropoff(request1));
		actions.add(new VGAVehiclePlanPickup(request0));
		actions.add(new VGAVehiclePlanDropoff(request0));
		actions.add(new VGAVehiclePlanPickup(request2));
		actions.add(new VGAVehiclePlanDropoff(request2));
		actions.add(new VGAVehiclePlanPickup(request4));
		actions.add(new VGAVehiclePlanDropoff(request4));
				
				
		// prepare test vehicle
		SimulationNode position = graph.getNode(16);
		TestOptimalPlanVehicle vehicle = new TestOptimalPlanVehicle(onBoardRequests, position, 
				config.amodsim.ridesharing.vehicleCapacity);
		
		Plan plan = arrayOptimalVehiclePlanFinder.getOptimalVehiclePlanForGroup(vehicle, actions, 8, false);
		
		Assert.assertNull(plan);
	}
}
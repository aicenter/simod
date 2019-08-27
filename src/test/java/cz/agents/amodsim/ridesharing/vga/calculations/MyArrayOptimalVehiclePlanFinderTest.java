/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.calculations;

import com.google.inject.Injector;
import cz.agents.amodsim.ridesharing.vga.common.TestModuleNoVisio;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.agents.amodsim.ridesharing.vga.mock.TestOptimalPlanVehicle;
import cz.agents.amodsim.ridesharing.vga.mock.TestPlanRequest;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import cz.cvut.fel.aic.agentpolis.system.AgentPolisInitializer;
import cz.cvut.fel.aic.amodsim.config.AmodsimConfig;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.traveltimecomputation.TravelTimeProvider;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.ArrayOptimalVehiclePlanFinder;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.IOptimalPlanVehicle;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.MathUtils;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehicle;
import java.io.File;
import java.util.LinkedHashSet;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author matal
 */
public class MyArrayOptimalVehiclePlanFinderTest {
    private static ArrayOptimalVehiclePlanFinder arrayOptimalVehiclePlanFinder;
	private static AmodsimConfig config;
	private static Injector injector;
	@BeforeClass
	public static void prepare(){
		config = new AmodsimConfig();
        File localConfigFile = null;
        AgentPolisInitializer agentPolisInitializer 
				= new AgentPolisInitializer(new TestModuleNoVisio(config, localConfigFile));
		injector = agentPolisInitializer.initialize();
        MathUtils.setTravelTimeProvider(injector.getInstance(TravelTimeProvider.class));
		arrayOptimalVehiclePlanFinder = injector.getInstance(ArrayOptimalVehiclePlanFinder.class);
    }
    @Test
    public void test(){
        LinkedHashSet<PlanComputationRequest> onBoardRequests = new LinkedHashSet<>();
        TestOptimalPlanVehicle vehicle = new TestOptimalPlanVehicle(onBoardRequests, new SimulationNode(11676,1443613450057047L, new GPSLocation(50057047,14436134,554512578,45963735), 1), 
				5);
        LinkedHashSet<PlanComputationRequest> group = new LinkedHashSet<>();
        TestPlanRequest request = new TestPlanRequest(2,config,new SimulationNode(10829,1443877050042385L, new GPSLocation(50042385,14438770,554349425,45981382), 1),new SimulationNode(7993,1447614050095541L, new GPSLocation(50095541,14476140,554938497,46253107), 1),1,false);
		group.add(request);
        Assert.assertNull(arrayOptimalVehiclePlanFinder.computeOptimalVehiclePlanForGroup(vehicle, group, 60, false));
    }
}

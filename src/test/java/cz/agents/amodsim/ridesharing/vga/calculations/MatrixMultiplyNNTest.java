/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations.*;
import cz.cvut.fel.aic.amodsim.ridesharing.vga.model.VGAVehicle;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author matal
 */
public class MatrixMultiplyNNTest {
    static Set<GroupData> groupsForNN = new LinkedHashSet<>();
    private static NN nn;
    private static IOptimalPlanVehicle vehicle;
    private static double[] correctData = {0.9504464036700224, 0.11001340633826014, 0.9999999999991172};
    @BeforeClass
    public static void prepare(){
        nn = new MatrixMultiplyNN();
        vehicle = new TestVehicle();
        //43
        Set<PlanComputationRequest> requests = new LinkedHashSet<>();
        requests.add(new TestRequest(new TestSimulationNode(50029406,14494622),186,new TestSimulationNode(50072000,14463575),939));
        requests.add(new TestRequest(new TestSimulationNode(50030801,14499329),228,new TestSimulationNode(50093052,14443520),1379));
        requests.add(new TestRequest(new TestSimulationNode(50026414,14492197),229,new TestSimulationNode(50089638,14432454),1414));        
        GroupData gd = new GroupData(requests);
        groupsForNN.add(gd);
        //47
        requests = new LinkedHashSet<>();
        requests.add(new TestRequest(new TestSimulationNode(50029406,14494622),186,new TestSimulationNode(50072000,14463575),939));
        requests.add(new TestRequest(new TestSimulationNode(50026414,14492197),229,new TestSimulationNode(50075414,14433372),1221));
        requests.add(new TestRequest(new TestSimulationNode(50026994,14499856),231,new TestSimulationNode(50084796,14419926),1470));        
        gd = new GroupData(requests);
        groupsForNN.add(gd);
        //48
        requests = new LinkedHashSet<>();
        requests.add(new TestRequest(new TestSimulationNode(50029406,14494622),186,new TestSimulationNode(50072000,14463575),939));
        requests.add(new TestRequest(new TestSimulationNode(50026414,14492197),229,new TestSimulationNode(50089834,14425895),1453));
        requests.add(new TestRequest(new TestSimulationNode(50026414,14492197),229,new TestSimulationNode(50089638,14432454),1414));        
        gd = new GroupData(requests);
        groupsForNN.add(gd);
    }
    @Test
    public void testNNGroupSize3(){
        
        nn.setProbability(groupsForNN, vehicle, 2);
        int i = 0;
        
        for (GroupData newGroupToCheck : groupsForNN) {
            Assert.assertEquals(correctData[i], newGroupToCheck.getFeasible(), 0.001);
            i++;
        }
    }
}

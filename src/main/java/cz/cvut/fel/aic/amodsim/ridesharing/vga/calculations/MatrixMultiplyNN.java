/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import org.ojalgo.matrix.PrimitiveMatrix;
import cz.cvut.fel.aic.amodsim.ridesharing.model.PlanComputationRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 *
 * @author matal
 */
public class MatrixMultiplyNN implements NN {   

    @Override
    public void setProbability(Set gd, IOptimalPlanVehicle vehicle) {
        PrimitiveMatrix.Factory matrixFactory = PrimitiveMatrix.FACTORY;
        Integer[][] data = new Integer[gd.size()][9];
        int car_lat = vehicle.getPosition().latE6;
        int car_lon = vehicle.getPosition().lonE6;
        int car_cap = vehicle.getCapacity();
        int k = 0;
        for (GroupData newGroupToCheck : (Set<GroupData>) gd) {
            int j = 0;
            data[k][j++] = car_lat;
            data[k][j++] = car_lon;
            data[k][j++] = car_cap;            
            for (PlanComputationRequest rq : newGroupToCheck.getRequests()) {
                data[k][j++] = rq.getFrom().latE6;
                data[k][j++] = rq.getFrom().lonE6;
                data[k][j++] = rq.getMaxPickupTime(); 
                data[k][j++] = rq.getTo().latE6;
                data[k][j++] = rq.getTo().lonE6;
                data[k][j++] = rq.getMaxDropoffTime(); 
            }
            k++;
        }
        PrimitiveMatrix matrixX = matrixFactory.rows(data);
        
        
        Integer[] data2 = new Integer[1024];
        for (int i = 0; i < 1024; i++) {
            data2[i] = i;
        }
        PrimitiveMatrix matrixW1 = matrixFactory.columns(data2);
        PrimitiveMatrix matrixb1 = matrixFactory.columns(data2);
        matrixW1 = matrixX.multiply(matrixW1.add(matrixb1));
        
        for (GroupData newGroupToCheck : (Set<GroupData>) gd) {
            newGroupToCheck.setFeasible(rd.nextFloat());
        }
    }
}

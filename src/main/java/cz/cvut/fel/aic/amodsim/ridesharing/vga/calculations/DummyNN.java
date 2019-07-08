/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import java.util.Random;
import java.util.Set;

/**
 *
 * @author matal
 */
public class DummyNN implements NN{
 
    @Override
    public void setProbability(Set gd, IOptimalPlanVehicle vehicle) {
        Random rd = new Random();
        for (GroupData newGroupToCheck : (Set<GroupData>) gd) {
            newGroupToCheck.setFeasible(rd.nextFloat());
        }
    }
}

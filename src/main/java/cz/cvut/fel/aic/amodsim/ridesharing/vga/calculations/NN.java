/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import java.util.Set;

/**
 *
 * @author matal
 */
public interface NN<V extends IOptimalPlanVehicle> {
    public void setProbability(Set<GroupData> gd, V vehicle);
}

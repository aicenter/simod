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
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import java.util.List;
import java.util.Random;

/**
 *
 * @author matal
 */
public class DummyNN implements NN{
 
    @Override
    public void setProbability(List gd, IOptimalPlanVehicle vehicle, int groupSize) {
        double ar[] = {0.4, 0.8, 0.8};
        int i = 0;
        for (GroupData newGroupToCheck : (List<GroupData>) gd) {
            newGroupToCheck.setFeasible(ar[i%3]);
            i++;
        }
    }
    @Override
    public void setProbability(List gd, int groupSize) {
        this.setProbability(gd, null, groupSize);
    }
}

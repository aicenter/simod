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
package cz.cvut.fel.aic.amodsim.storage;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import java.util.Collection;


/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class OnDemandVehicleStorage extends EntityStorage<OnDemandVehicle>{
	
	@Inject
	public OnDemandVehicleStorage() {
		super();
	}
    
    
    public void clearStorage(){
        if (isEmpty()){
            return;
        }
        Collection <OnDemandVehicle>  vehicles = getEntities();
        for (OnDemandVehicle v : vehicles){
            removeEntity(v);
        }
    }
	
}

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
package cz.cvut.fel.aic.amodsim.init;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.NearestElementUtils;
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.amodsim.storage.OnDemandvehicleStationStorage;
import java.util.List;

/**
 *
 * @author fido
 */
@Singleton
public class EntityInitializer {
	private final OnDemandvehicleStationStorage onDemandVehicleStationStorage;
	
	private final NearestElementUtils nearestElementUtils;

	
	
	@Inject
	public EntityInitializer(OnDemandvehicleStationStorage onDemandVehicleStationStorage, 
			NearestElementUtils nearestElementUtils) {
		this.onDemandVehicleStationStorage = onDemandVehicleStationStorage;
		this.nearestElementUtils = nearestElementUtils;
	}

	public void initialize(List<OnDemandVehicleStation> onDemandVehicleStations) {
//		for (OnDemandVehicleStation onDemandVehicleStation : onDemandVehicleStations) {
//			onDemandVehicleStation.setNearestNode(nearestElementUtils.getNearestElement(
//					onDemandVehicleStation.getGpsLocation(), EGraphType.HIGHWAY));
//			onDemandVehicleStationStorage.addEntity(onDemandVehicleStation);
//		}
	}
	

	
	
	
}

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
import cz.cvut.fel.aic.amodsim.entity.OnDemandVehicleStation;
import cz.cvut.fel.aic.agentpolis.simmodel.environment.EntityStorage;
import cz.cvut.fel.aic.geographtools.GPSLocation;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtil;
import cz.cvut.fel.aic.geographtools.util.NearestElementUtilPair;
import cz.cvut.fel.aic.geographtools.util.Transformer;
import java.util.ArrayList;
import java.util.List;
import org.locationtech.jts.geom.Coordinate;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author fido
 */
@Singleton
public class OnDemandvehicleStationStorage extends EntityStorage<OnDemandVehicleStation>{
	
	private NearestElementUtil<OnDemandVehicleStation> nearestElementUtil;
	
	private final Transformer transformer;
	
	private int numberOfDemandsNotServedFromNearestStation;
	
	public int getNumberOfDemandsNotServedFromNearestStation() {
		return numberOfDemandsNotServedFromNearestStation;
	}
	
	@Inject
	public OnDemandvehicleStationStorage(Transformer transformer) {
		super();
		this.transformer = transformer;
		numberOfDemandsNotServedFromNearestStation = 0;
	}
	
	public OnDemandVehicleStation getNearestStation(GPSLocation position){
		if(nearestElementUtil == null){
			nearestElementUtil = getNearestElementUtilForStations();
		}
		
		OnDemandVehicleStation nearestStation = nearestElementUtil.getNearestElement(position);
		return nearestStation;
	}
	
	public OnDemandVehicleStation getNearestReadyStation(GPSLocation position){
		if(nearestElementUtil == null){
			nearestElementUtil = getNearestElementUtilForStations();
		}
		
		OnDemandVehicleStation[] onDemandVehicleStationsSorted 
				= (OnDemandVehicleStation[]) nearestElementUtil.getKNearestElements(position, 1);
		
		OnDemandVehicleStation nearestStation = null;
		int i = 0;
		while(i < onDemandVehicleStationsSorted.length){
			if(!onDemandVehicleStationsSorted[i].isEmpty()){
				if(i > 0){
					numberOfDemandsNotServedFromNearestStation++;
				}
				nearestStation = onDemandVehicleStationsSorted[i];
				break;
			}
			i++;
		}
		
		return nearestStation;
	}
	
	private NearestElementUtil<OnDemandVehicleStation> getNearestElementUtilForStations() {
		List<NearestElementUtilPair<Coordinate,OnDemandVehicleStation>> pairs = new ArrayList<>();
		
		OnDemandvehicleStationStorage.EntityIterator iterator = new EntityIterator();
		
		OnDemandVehicleStation station;
		while ((station = iterator.getNextEntity()) != null) {
			GPSLocation location = station.getPosition();
			
			pairs.add(new NearestElementUtilPair<>(new Coordinate(
					location.getLongitude(), location.getLatitude(), location.elevation), station));
		}
		
		return new NearestElementUtil<>(pairs, transformer, new OnDemandVehicleStationArrayConstructor());
	}
	
	
	
	
	private static class OnDemandVehicleStationArrayConstructor 
			implements NearestElementUtil.SerializableIntFunction<OnDemandVehicleStation[]>{

		@Override
		public OnDemandVehicleStation[] apply(int value) {
			return new OnDemandVehicleStation[value];
		}

	}
	
}

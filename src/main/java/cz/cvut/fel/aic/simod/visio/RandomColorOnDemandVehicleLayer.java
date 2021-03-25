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
package cz.cvut.fel.aic.simod.visio;


import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.AgentPolisEntity;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.PhysicalTransportVehicle;
import cz.cvut.fel.aic.simod.storage.PhysicalTransportVehicleStorage;
import java.awt.Color;
import java.util.HashMap;
import java.util.Random;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author fido
 */
public class RandomColorOnDemandVehicleLayer extends OnDemandVehicleLayer{
	
	private final Random random;
	
	private final HashMap<AgentPolisEntity,Color> agentColors;
	
	
	
	public RandomColorOnDemandVehicleLayer(PhysicalTransportVehicleStorage physicalTransportVehicleStorage, AgentpolisConfig agentpolisConfig) {
		super(physicalTransportVehicleStorage, agentpolisConfig);
		this.random = new Random();
		agentColors = new HashMap<>();
	}

	@Override
	protected Color getEntityDrawColor(PhysicalTransportVehicle agent) {
		if(agentColors.containsKey(agent)){
			return agentColors.get(agent);
		}
		else{
			Color color = getRandomColor();
			agentColors.put(agent, color);
			return color;
		}
	}
	
	
	
	
	
	private Color getRandomColor(){
		float r = random.nextFloat();
		float g = random.nextFloat();
		float b = random.nextFloat();
		
		return new Color(r, g, b);
	}
	
}

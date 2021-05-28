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
package cz.cvut.fel.aic.simod;

import cz.cvut.fel.aic.agentpolis.simmodel.entity.EntityType;

/**
 *
 * @author david
 */
public enum DemandSimulationEntityType implements EntityType{

	TEST_TYPE("testType", 0),
	DEMAND("demand", 1),
	PARCEL("parcel", 2),
	ON_DEMAND_VEHICLE("on demand vehicle", 3),
	VEHICLE("vehicle", 4),
	ON_DEMAND_VEHICLE_STATION("on demand vehicle station", 5);
	
	private final String entityType;
	private final int id;
	

	private DemandSimulationEntityType(String entityType, int id) {
		this.entityType = entityType;
		this.id = id;
	}	

	@Override
	public String getDescriptionEntityType() {
		return entityType;
	}

	public int getId() {
		return id;
	}
	
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.testsim;

import cz.agents.agentpolis.simmodel.entity.EntityType;

/**
 *
 * @author david
 */
public enum DemandSimulationEntityType implements EntityType{

	TEST_TYPE("testType"),
	DEMAND("demand");
	
	private final String entityType;
	

	private DemandSimulationEntityType(String entityType) {
		this.entityType = entityType;
	}	

	@Override
	public String getDescriptionEntityType() {
		return entityType;
	}
	
}

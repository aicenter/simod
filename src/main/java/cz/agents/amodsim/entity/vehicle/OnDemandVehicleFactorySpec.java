/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.entity.vehicle;

import cz.agents.agentpolis.simmodel.environment.model.citymodel.transportnetwork.elements.SimulationNode;
import cz.agents.basestructures.Node;

/**
 *
 * @author fido
 */
public interface OnDemandVehicleFactorySpec {
    public OnDemandVehicle create(String vehicleId, SimulationNode startPosition);
}

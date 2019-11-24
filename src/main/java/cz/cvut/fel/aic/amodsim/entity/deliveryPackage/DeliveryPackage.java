/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.entity.deliveryPackage;

import cz.cvut.fel.aic.agentpolis.simmodel.environment.transportnetwork.elements.SimulationNode;
import org.apache.commons.lang3.tuple.Triple;

/**
 *
 * @author kubis
 */
public class DeliveryPackage {

        private final DeliveryPackageDimensions dimensions;

        private final int weight;

        private final SimulationNode destination;

        public DeliveryPackage(DeliveryPackageDimensions dimensions, int weight, SimulationNode destination) {
                this.destination = destination;
                this.weight = weight;
                this.dimensions = dimensions;
        }

}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.agents.amodsim.event;

import cz.agents.alite.common.event.EventType;

/**
 *
 * @author fido
 */
public enum OnDemandVehicleStationEvent implements EventType{
    TRIP,
    REBALANCING
}

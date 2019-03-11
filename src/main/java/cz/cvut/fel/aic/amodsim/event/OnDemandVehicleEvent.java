/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.event;

/**
 *
 * @author fido
 */
public enum OnDemandVehicleEvent{
    LEAVE_STATION,
    PICKUP,
    DROP_OFF,
    REACH_NEAREST_STATION,
    START_REBALANCING,
    FINISH_REBALANCING
}

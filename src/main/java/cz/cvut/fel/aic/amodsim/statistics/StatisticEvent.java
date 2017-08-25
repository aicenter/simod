/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cz.cvut.fel.aic.amodsim.statistics;

import cz.cvut.fel.aic.alite.common.event.EventType;

/**
 *
 * @author fido
 */
public enum StatisticEvent implements EventType{
    TICK,
    VEHICLE_LEFT_STATION_TO_SERVE_DEMAND,
    DEMAND_PICKED_UP
}

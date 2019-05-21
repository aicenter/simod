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
package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Ridesharing {
  public Integer maxDirectSpeedEstimationKmh;

  public Vga vga;

  public Integer batchPeriod;

  public String method;

  public InsertionHeuristic insertionHeuristic;

  public String discomfortConstraint;

  public Double maximumRelativeDiscomfort;

  public Integer maxProlongationInSeconds;

  public Double weightParameter;

  public Integer vehicleCapacity;

  public Boolean on;

  public Ridesharing(Map ridesharing) {
    this.maxDirectSpeedEstimationKmh = (Integer) ridesharing.get("max_direct_speed_estimation_kmh");
    this.vga = new Vga((Map) ridesharing.get("vga"));
    this.batchPeriod = (Integer) ridesharing.get("batch_period");
    this.method = (String) ridesharing.get("method");
    this.insertionHeuristic = new InsertionHeuristic((Map) ridesharing.get("insertion_heuristic"));
    this.discomfortConstraint = (String) ridesharing.get("discomfort_constraint");
    this.maximumRelativeDiscomfort = (Double) ridesharing.get("maximum_relative_discomfort");
    this.maxProlongationInSeconds = (Integer) ridesharing.get("max_prolongation_in_seconds");
    this.weightParameter = (Double) ridesharing.get("weight_parameter");
    this.vehicleCapacity = (Integer) ridesharing.get("vehicle_capacity");
    this.on = (Boolean) ridesharing.get("on");
  }
}

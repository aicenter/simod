/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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
package cz.cvut.fel.aic.simod.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Vga {
  public Boolean optimizeParkedVehicles;

  public Double solverMinGap;

  public Boolean logPlanComputationalTime;

  public Integer maxOptimalGroupSize;

  public Integer maxGroupSize;

  public Integer solverTimeLimit;

  public Integer groupGenerationTimeLimit;

  public Integer solverMaxTripsPerRequest;

  public Boolean exportGroupData;

  public Integer maxVehiclesPerRequest;

  public String groupGeneratorLogFilepath;

  public String modelExportFilePath;

  public Vga(Map vga) {
    this.optimizeParkedVehicles = (Boolean) vga.get("optimize_parked_vehicles");
    this.solverMinGap = (Double) vga.get("solver_min_gap");
    this.logPlanComputationalTime = (Boolean) vga.get("log_plan_computational_time");
    this.maxOptimalGroupSize = (Integer) vga.get("max_optimal_group_size");
    this.maxGroupSize = (Integer) vga.get("max_group_size");
    this.solverTimeLimit = (Integer) vga.get("solver_time_limit");
    this.groupGenerationTimeLimit = (Integer) vga.get("group_generation_time_limit");
    this.solverMaxTripsPerRequest = (Integer) vga.get("solver_max_trips_per_request");
    this.exportGroupData = (Boolean) vga.get("export_group_data");
    this.maxVehiclesPerRequest = (Integer) vga.get("max_vehicles_per_request");
    this.groupGeneratorLogFilepath = (String) vga.get("group_generator_log_filepath");
    this.modelExportFilePath = (String) vga.get("model_export_file_path");
  }
}

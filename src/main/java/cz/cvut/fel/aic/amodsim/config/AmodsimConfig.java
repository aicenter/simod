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
import ninja.fido.config.GeneratedConfig;

public class AmodsimConfig implements GeneratedConfig {
  public Integer vehicleSpeedInMeters;

  public Boolean useTripCache;

  public String experimentName;

  public String tripsFilename;

  public String distanceMatrixFilepath;

  public String edgePairsFilePath;

  public String tripCacheFile;

  public String mapDir;

  public Integer startTime;

  public String tripsPath;

  public String amodsimExperimentDir;

  public String amodsimDataDir;

  public String edgesFilePath;

  public Double tripsMultiplier;

  public Integer vehiclesPerStation;

  public Boolean simplifyGraph;

  public Ridesharing ridesharing;

  public String stationPositionFilepath;

  public Rebalancing rebalancing;

  public Statistics statistics;

  public AmodsimConfig() {
  }

  public AmodsimConfig fill(Map amodsimConfig) {
    this.vehicleSpeedInMeters = (Integer) amodsimConfig.get("vehicle_speed_in_meters");
    this.useTripCache = (Boolean) amodsimConfig.get("use_trip_cache");
    this.experimentName = (String) amodsimConfig.get("experiment_name");
    this.tripsFilename = (String) amodsimConfig.get("trips_filename");
    this.distanceMatrixFilepath = (String) amodsimConfig.get("distance_matrix_filepath");
    this.edgePairsFilePath = (String) amodsimConfig.get("edge_pairs_file_path");
    this.tripCacheFile = (String) amodsimConfig.get("trip_cache_file");
    this.mapDir = (String) amodsimConfig.get("map_dir");
    this.startTime = (Integer) amodsimConfig.get("start_time");
    this.tripsPath = (String) amodsimConfig.get("trips_path");
    this.amodsimExperimentDir = (String) amodsimConfig.get("amodsim_experiment_dir");
    this.amodsimDataDir = (String) amodsimConfig.get("amodsim_data_dir");
    this.edgesFilePath = (String) amodsimConfig.get("edges_file_path");
    this.tripsMultiplier = (Double) amodsimConfig.get("trips_multiplier");
    this.vehiclesPerStation = (Integer) amodsimConfig.get("vehicles_per_station");
    this.simplifyGraph = (Boolean) amodsimConfig.get("simplify_graph");
    this.ridesharing = new Ridesharing((Map) amodsimConfig.get("ridesharing"));
    this.stationPositionFilepath = (String) amodsimConfig.get("station_position_filepath");
    this.rebalancing = new Rebalancing((Map) amodsimConfig.get("rebalancing"));
    this.statistics = new Statistics((Map) amodsimConfig.get("statistics"));
    return this;
  }
}

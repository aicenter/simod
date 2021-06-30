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
import ninja.fido.config.GeneratedConfig;

public class SimodConfig implements GeneratedConfig {
  public Boolean useTripCache;

  public String experimentName;

  public String tripsFilename;

  public String distanceMatrixFilepath;

  public String edgePairsFilePath;

  public String tripCacheFile;

  public Stations stations;

  public String mapDir;

  public String travelTimeProvider;

  public Shortestpaths shortestpaths;

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

  public SimodConfig() {
  }

  public SimodConfig fill(Map simodConfig) {
    this.useTripCache = (Boolean) simodConfig.get("use_trip_cache");
    this.experimentName = (String) simodConfig.get("experiment_name");
    this.tripsFilename = (String) simodConfig.get("trips_filename");
    this.distanceMatrixFilepath = (String) simodConfig.get("distance_matrix_filepath");
    this.edgePairsFilePath = (String) simodConfig.get("edge_pairs_file_path");
    this.tripCacheFile = (String) simodConfig.get("trip_cache_file");
    this.stations = new Stations((Map) simodConfig.get("stations"));
    this.mapDir = (String) simodConfig.get("map_dir");
    this.travelTimeProvider = (String) simodConfig.get("travel_time_provider");
    this.shortestpaths = new Shortestpaths((Map) simodConfig.get("shortestpaths"));
    this.startTime = (Integer) simodConfig.get("start_time");
    this.tripsPath = (String) simodConfig.get("trips_path");
    this.amodsimExperimentDir = (String) simodConfig.get("amodsim_experiment_dir");
    this.amodsimDataDir = (String) simodConfig.get("amodsim_data_dir");
    this.edgesFilePath = (String) simodConfig.get("edges_file_path");
    this.tripsMultiplier = (Double) simodConfig.get("trips_multiplier");
    this.vehiclesPerStation = (Integer) simodConfig.get("vehicles_per_station");
    this.simplifyGraph = (Boolean) simodConfig.get("simplify_graph");
    this.ridesharing = new Ridesharing((Map) simodConfig.get("ridesharing"));
    this.stationPositionFilepath = (String) simodConfig.get("station_position_filepath");
    this.rebalancing = new Rebalancing((Map) simodConfig.get("rebalancing"));
    this.statistics = new Statistics((Map) simodConfig.get("statistics"));
    return this;
  }
}

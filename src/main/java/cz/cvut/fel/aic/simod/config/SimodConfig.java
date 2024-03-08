package cz.cvut.fel.aic.simod.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;
import ninja.fido.config.GeneratedConfig;

public class SimodConfig implements GeneratedConfig {
  public String simodDataDir;

  public Boolean reconfigurableVehicles;

  public String edgePairsFilePath;

  public Vehicles vehicles;

  public String tripCacheFile;

  public String mapDir;

  public Shortestpaths shortestpaths;

  public String tripsPath;

  public String edgesFilePath;

  public Double tripsMultiplier;

  public Integer vehiclesPerStation;

  public Ridesharing ridesharing;

  public String stationPositionFilepath;

  public Boolean useTripCache;

  public String simodExperimentDir;

  public Integer serviceTime;

  public String experimentName;

  public String tripsFilename;

  public String distanceMatrixFilepath;

  public Stations stations;

  public Boolean heterogeneousVehicles;

  public String travelTimeProvider;

  public Integer startTime;

  public String vehiclesFilePath;

  public Boolean simplifyGraph;

  public Rebalancing rebalancing;

  public Statistics statistics;

  public SimodConfig() {
  }

  public SimodConfig fill(Map simodConfig) {
    this.simodDataDir = (String) simodConfig.get("simod_data_dir");
    this.reconfigurableVehicles = (Boolean) simodConfig.get("reconfigurable_vehicles");
    this.edgePairsFilePath = (String) simodConfig.get("edge_pairs_file_path");
    this.vehicles = new Vehicles((Map) simodConfig.get("vehicles"));
    this.tripCacheFile = (String) simodConfig.get("trip_cache_file");
    this.mapDir = (String) simodConfig.get("map_dir");
    this.shortestpaths = new Shortestpaths((Map) simodConfig.get("shortestpaths"));
    this.tripsPath = (String) simodConfig.get("trips_path");
    this.edgesFilePath = (String) simodConfig.get("edges_file_path");
    this.tripsMultiplier = (Double) simodConfig.get("trips_multiplier");
    this.vehiclesPerStation = (Integer) simodConfig.get("vehicles_per_station");
    this.ridesharing = new Ridesharing((Map) simodConfig.get("ridesharing"));
    this.stationPositionFilepath = (String) simodConfig.get("station_position_filepath");
    this.useTripCache = (Boolean) simodConfig.get("use_trip_cache");
    this.simodExperimentDir = (String) simodConfig.get("simod_experiment_dir");
    this.serviceTime = (Integer) simodConfig.get("service_time");
    this.experimentName = (String) simodConfig.get("experiment_name");
    this.tripsFilename = (String) simodConfig.get("trips_filename");
    this.distanceMatrixFilepath = (String) simodConfig.get("distance_matrix_filepath");
    this.stations = new Stations((Map) simodConfig.get("stations"));
    this.heterogeneousVehicles = (Boolean) simodConfig.get("heterogeneous_vehicles");
    this.travelTimeProvider = (String) simodConfig.get("travel_time_provider");
    this.startTime = (Integer) simodConfig.get("start_time");
    this.vehiclesFilePath = (String) simodConfig.get("vehicles_file_path");
    this.simplifyGraph = (Boolean) simodConfig.get("simplify_graph");
    this.rebalancing = new Rebalancing((Map) simodConfig.get("rebalancing"));
    this.statistics = new Statistics((Map) simodConfig.get("statistics"));
    return this;
  }
}

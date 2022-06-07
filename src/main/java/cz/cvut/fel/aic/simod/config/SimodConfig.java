package cz.cvut.fel.aic.simod.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;
import ninja.fido.config.GeneratedConfig;

public class SimodConfig implements GeneratedConfig {
  public String simodDataDir;

  public Integer packagesMaxDelay;

  public String edgePairsFilePath;

  public Boolean packagesOn;

  public String tripCacheFile;

  public String packagesFilename;

  public String mapDir;

  public Shortestpaths shortestpaths;

  public String tripsPath;

  public String edgesFilePath;

  public Integer packagesCapacity;

  public Double tripsMultiplier;

  public Integer vehiclesPerStation;

  public Ridesharing ridesharing;

  public String stationPositionFilepath;

  public Boolean useTripCache;

  public String simodExperimentDir;

  public String experimentName;

  public String tripsFilename;

  public String distanceMatrixFilepath;

  public Stations stations;

  public String travelTimeProvider;

  public Integer startTime;

  public String packagesPath;

  public Boolean simplifyGraph;

  public Rebalancing rebalancing;

  public Statistics statistics;

  public SimodConfig() {
  }

  public SimodConfig fill(Map simodConfig) {
    this.simodDataDir = (String) simodConfig.get("simod_data_dir");
    this.packagesMaxDelay = (Integer) simodConfig.get("packages_max_delay");
    this.edgePairsFilePath = (String) simodConfig.get("edge_pairs_file_path");
    this.packagesOn = (Boolean) simodConfig.get("packages_on");
    this.tripCacheFile = (String) simodConfig.get("trip_cache_file");
    this.packagesFilename = (String) simodConfig.get("packages_filename");
    this.mapDir = (String) simodConfig.get("map_dir");
    this.shortestpaths = new Shortestpaths((Map) simodConfig.get("shortestpaths"));
    this.tripsPath = (String) simodConfig.get("trips_path");
    this.edgesFilePath = (String) simodConfig.get("edges_file_path");
    this.packagesCapacity = (Integer) simodConfig.get("packages_capacity");
    this.tripsMultiplier = (Double) simodConfig.get("trips_multiplier");
    this.vehiclesPerStation = (Integer) simodConfig.get("vehicles_per_station");
    this.ridesharing = new Ridesharing((Map) simodConfig.get("ridesharing"));
    this.stationPositionFilepath = (String) simodConfig.get("station_position_filepath");
    this.useTripCache = (Boolean) simodConfig.get("use_trip_cache");
    this.simodExperimentDir = (String) simodConfig.get("simod_experiment_dir");
    this.experimentName = (String) simodConfig.get("experiment_name");
    this.tripsFilename = (String) simodConfig.get("trips_filename");
    this.distanceMatrixFilepath = (String) simodConfig.get("distance_matrix_filepath");
    this.stations = new Stations((Map) simodConfig.get("stations"));
    this.travelTimeProvider = (String) simodConfig.get("travel_time_provider");
    this.startTime = (Integer) simodConfig.get("start_time");
    this.packagesPath = (String) simodConfig.get("packages_path");
    this.simplifyGraph = (Boolean) simodConfig.get("simplify_graph");
    this.rebalancing = new Rebalancing((Map) simodConfig.get("rebalancing"));
    this.statistics = new Statistics((Map) simodConfig.get("statistics"));
    return this;
  }
}

package cz.cvut.fel.aic.amodsim.config;

import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;
import ninja.fido.config.GeneratedConfig;

public class AmodsimConfig implements GeneratedConfig {
  public Double vehicleSpeedInMeters;

  public String experimentName;

  public String tripsFilename;

  public Images images;

  public Shapefiles shapefiles;

  public Stations stations;

  public Analysis analysis;

  public String mapDir;

  public String pythonExperimentDir;

  public Integer srid;

  public String amodsimExperimentDir;

  public String amodsimDataDir;

  public Double tripsMultiplier;

  public Double criticalDensity;

  public String pythonDataDir;

  public String tripsFilePath;

  public Integer tripsLimit;

  public Db db;

  public Amodsim amodsim;

  public Rebalancing rebalancing;

  public AmodsimConfig() {
  }

  public AmodsimConfig fill(Map amodsimConfig) {
    this.vehicleSpeedInMeters = (Double) amodsimConfig.get("vehicle_speed_in_meters");
    this.experimentName = (String) amodsimConfig.get("experiment_name");
    this.tripsFilename = (String) amodsimConfig.get("trips_filename");
    this.images = new Images((Map) amodsimConfig.get("images"));
    this.shapefiles = new Shapefiles((Map) amodsimConfig.get("shapefiles"));
    this.stations = new Stations((Map) amodsimConfig.get("stations"));
    this.analysis = new Analysis((Map) amodsimConfig.get("analysis"));
    this.mapDir = (String) amodsimConfig.get("map_dir");
    this.pythonExperimentDir = (String) amodsimConfig.get("python_experiment_dir");
    this.srid = (Integer) amodsimConfig.get("srid");
    this.amodsimExperimentDir = (String) amodsimConfig.get("amodsim_experiment_dir");
    this.amodsimDataDir = (String) amodsimConfig.get("amodsim_data_dir");
    this.tripsMultiplier = (Double) amodsimConfig.get("trips_multiplier");
    this.criticalDensity = (Double) amodsimConfig.get("critical_density");
    this.pythonDataDir = (String) amodsimConfig.get("python_data_dir");
    this.tripsFilePath = (String) amodsimConfig.get("trips_file_path");
    this.tripsLimit = (Integer) amodsimConfig.get("trips_limit");
    this.db = new Db((Map) amodsimConfig.get("db"));
    this.amodsim = new Amodsim((Map) amodsimConfig.get("amodsim"));
    this.rebalancing = new Rebalancing((Map) amodsimConfig.get("rebalancing"));
    return this;
  }
}

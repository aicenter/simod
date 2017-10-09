package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Amodsim {
  public Boolean useTripCache;

  public String preprocessedTrips;

  public String edgePairsFilePath;

  public String tripCacheFile;

  public String preprocessorPath;

  public Integer startTime;

  public String tripsPath;

  public String edgesFilePath;

  public Integer simulationDurationInMillis;

  public Boolean showVisio;

  public Boolean simplifyGraph;

  public Boolean ridesharing;

  public Statistics statistics;

  public Amodsim(Map amodsim) {
    this.useTripCache = (Boolean) amodsim.get("use_trip_cache");
    this.preprocessedTrips = (String) amodsim.get("preprocessed_trips");
    this.edgePairsFilePath = (String) amodsim.get("edge_pairs_file_path");
    this.tripCacheFile = (String) amodsim.get("trip_cache_file");
    this.preprocessorPath = (String) amodsim.get("preprocessor_path");
    this.startTime = (Integer) amodsim.get("start_time");
    this.tripsPath = (String) amodsim.get("trips_path");
    this.edgesFilePath = (String) amodsim.get("edges_file_path");
    this.simulationDurationInMillis = (Integer) amodsim.get("simulation_duration_in_millis");
    this.showVisio = (Boolean) amodsim.get("show_visio");
    this.simplifyGraph = (Boolean) amodsim.get("simplify_graph");
    this.ridesharing = (Boolean) amodsim.get("ridesharing");
    this.statistics = new Statistics((Map) amodsim.get("statistics"));
  }
}

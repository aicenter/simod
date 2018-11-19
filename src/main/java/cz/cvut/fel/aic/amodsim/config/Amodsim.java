package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Amodsim {
  public Boolean useTripCache;

  public Integer startTime;

  public String preprocessedTrips;

  public String tripsPath;

  public String edgesFilePath;

  public String edgePairsFilePath;

  public String tripCacheFile;

  public AmodsimRebalancing amodsimRebalancing;

  public String preprocessorPath;

  public Boolean simplifyGraph;

  public Ridesharing ridesharing;

  public Statistics statistics;

  public Amodsim(Map amodsim) {
    this.useTripCache = (Boolean) amodsim.get("use_trip_cache");
    this.startTime = (Integer) amodsim.get("start_time");
    this.preprocessedTrips = (String) amodsim.get("preprocessed_trips");
    this.tripsPath = (String) amodsim.get("trips_path");
    this.edgesFilePath = (String) amodsim.get("edges_file_path");
    this.edgePairsFilePath = (String) amodsim.get("edge_pairs_file_path");
    this.tripCacheFile = (String) amodsim.get("trip_cache_file");
    this.amodsimRebalancing = new AmodsimRebalancing((Map) amodsim.get("amodsim_rebalancing"));
    this.preprocessorPath = (String) amodsim.get("preprocessor_path");
    this.simplifyGraph = (Boolean) amodsim.get("simplify_graph");
    this.ridesharing = new Ridesharing((Map) amodsim.get("ridesharing"));
    this.statistics = new Statistics((Map) amodsim.get("statistics"));
  }
}

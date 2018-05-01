package cz.cvut.fel.aic.amodsim.config;

import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Analysis {
  public String ridesharingOnExperimentPath;

  public String edgeLoadRidesharingOnFilepath;

  public String edgeLoadRidesharingOffFilepath;

  public String resultsRidesharingOffFilepath;

  public Double tripsMultiplier;

  public Integer chosenWindowStart;

  public Integer chosenWindowEnd;

  public String ridesharingOffExperimentPath;

  public String resultsRidesharingOnFilepath;

  public Analysis(Map analysis) {
    this.ridesharingOnExperimentPath = (String) analysis.get("ridesharing_on_experiment_path");
    this.edgeLoadRidesharingOnFilepath = (String) analysis.get("edge_load_ridesharing_on_filepath");
    this.edgeLoadRidesharingOffFilepath = (String) analysis.get("edge_load_ridesharing_off_filepath");
    this.resultsRidesharingOffFilepath = (String) analysis.get("results_ridesharing_off_filepath");
    this.tripsMultiplier = (Double) analysis.get("trips_multiplier");
    this.chosenWindowStart = (Integer) analysis.get("chosen_window_start");
    this.chosenWindowEnd = (Integer) analysis.get("chosen_window_end");
    this.ridesharingOffExperimentPath = (String) analysis.get("ridesharing_off_experiment_path");
    this.resultsRidesharingOnFilepath = (String) analysis.get("results_ridesharing_on_filepath");
  }
}

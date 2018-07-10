package cz.cvut.fel.aic.amodsim.config;

import java.lang.String;
import java.util.Map;

public class Images {
  public String trafficDensityMapComparison;

  public String occupancyHistogramWindow;

  public String imagesExperimentDir;

  public String tripStartHistogram;

  public String occupancyHistogram;

  public String trafficDensityHistogramComparison;

  public String trafficDensityHistogramComparisonAlt;

  public String waitTimeComparisonDir;

  public String mainMap;

  public String occupancyComparison;

  public String serviceComparison;

  public String distanceComparison;

  public String imagesDir;

  public String comparisonDir;

  public String trafficDensityCurrent;

  public String droppedDemandsComparison;

  public String trafficDensityFutureDetail;

  public String trafficDensityFutureDetailStacked;

  public String trafficDensityCurrentDetail;

  public Images(Map images) {
    this.trafficDensityMapComparison = (String) images.get("traffic_density_map_comparison");
    this.occupancyHistogramWindow = (String) images.get("occupancy_histogram_window");
    this.imagesExperimentDir = (String) images.get("images_experiment_dir");
    this.tripStartHistogram = (String) images.get("trip_start_histogram");
    this.occupancyHistogram = (String) images.get("occupancy_histogram");
    this.trafficDensityHistogramComparison = (String) images.get("traffic_density_histogram_comparison");
    this.trafficDensityHistogramComparisonAlt = (String) images.get("traffic_density_histogram_comparison_alt");
    this.waitTimeComparisonDir = (String) images.get("wait_time_comparison_dir");
    this.mainMap = (String) images.get("main_map");
    this.occupancyComparison = (String) images.get("occupancy_comparison");
    this.serviceComparison = (String) images.get("service_comparison");
    this.distanceComparison = (String) images.get("distance_comparison");
    this.imagesDir = (String) images.get("images_dir");
    this.comparisonDir = (String) images.get("comparison_dir");
    this.trafficDensityCurrent = (String) images.get("traffic_density_current");
    this.droppedDemandsComparison = (String) images.get("dropped_demands_comparison");
    this.trafficDensityFutureDetail = (String) images.get("traffic_density_future_detail");
    this.trafficDensityFutureDetailStacked = (String) images.get("traffic_density_future_detail_stacked");
    this.trafficDensityCurrentDetail = (String) images.get("traffic_density_current_detail");
  }
}

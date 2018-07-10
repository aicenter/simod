package cz.cvut.fel.aic.amodsim.config;

import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Statistics {
  public String resultFilePath;

  public String occupanciesFilePath;

  public Integer statisticIntervalMilis;

  public String transitStatisticFilePath;

  public String allEdgesLoadHistoryFilePath;

  public OnDemandVehicleStatistic onDemandVehicleStatistic;

  public String resultFileName;

  public Integer allEdgesLoadIntervalMilis;

  public String tripDistancesFilePath;

  public String occupanciesFileName;

  public String allEdgesLoadHistoryFileName;

  public String serviceFileName;

  public String serviceFilePath;

  public Statistics(Map statistics) {
    this.resultFilePath = (String) statistics.get("result_file_path");
    this.occupanciesFilePath = (String) statistics.get("occupancies_file_path");
    this.statisticIntervalMilis = (Integer) statistics.get("statistic_interval_milis");
    this.transitStatisticFilePath = (String) statistics.get("transit_statistic_file_path");
    this.allEdgesLoadHistoryFilePath = (String) statistics.get("all_edges_load_history_file_path");
    this.onDemandVehicleStatistic = new OnDemandVehicleStatistic((Map) statistics.get("on_demand_vehicle_statistic"));
    this.resultFileName = (String) statistics.get("result_file_name");
    this.allEdgesLoadIntervalMilis = (Integer) statistics.get("all_edges_load_interval_milis");
    this.tripDistancesFilePath = (String) statistics.get("trip_distances_file_path");
    this.occupanciesFileName = (String) statistics.get("occupancies_file_name");
    this.allEdgesLoadHistoryFileName = (String) statistics.get("all_edges_load_history_file_name");
    this.serviceFileName = (String) statistics.get("service_file_name");
    this.serviceFilePath = (String) statistics.get("service_file_path");
  }
}

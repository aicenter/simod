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

import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Statistics {
  public String resultFilePath;

  public String occupanciesFilePath;

  public String ridesharingFileName;

  public Integer statisticIntervalMilis;

  public String groupDataFilePath;

  public String transitStatisticFilePath;

  public String allEdgesLoadHistoryFilePath;

  public OnDemandVehicleStatistic onDemandVehicleStatistic;

  public String resultFileName;

  public Integer allEdgesLoadIntervalMilis;

  public String tripDistancesFilePath;

  public String occupanciesFileName;

  public String darpSolverComputationalTimesFilePath;

  public String groupDataFilename;

  public String allEdgesLoadHistoryFileName;

  public String serviceFileName;

  public String serviceFilePath;

  public String ridesharingFilePath;

  public Statistics(Map statistics) {
    this.resultFilePath = (String) statistics.get("result_file_path");
    this.occupanciesFilePath = (String) statistics.get("occupancies_file_path");
    this.ridesharingFileName = (String) statistics.get("ridesharing_file_name");
    this.statisticIntervalMilis = (Integer) statistics.get("statistic_interval_milis");
    this.groupDataFilePath = (String) statistics.get("group_data_file_path");
    this.transitStatisticFilePath = (String) statistics.get("transit_statistic_file_path");
    this.allEdgesLoadHistoryFilePath = (String) statistics.get("all_edges_load_history_file_path");
    this.onDemandVehicleStatistic = new OnDemandVehicleStatistic((Map) statistics.get("on_demand_vehicle_statistic"));
    this.resultFileName = (String) statistics.get("result_file_name");
    this.allEdgesLoadIntervalMilis = (Integer) statistics.get("all_edges_load_interval_milis");
    this.tripDistancesFilePath = (String) statistics.get("trip_distances_file_path");
    this.occupanciesFileName = (String) statistics.get("occupancies_file_name");
    this.darpSolverComputationalTimesFilePath = (String) statistics.get("darp_solver_computational_times_file_path");
    this.groupDataFilename = (String) statistics.get("group_data_filename");
    this.allEdgesLoadHistoryFileName = (String) statistics.get("all_edges_load_history_file_name");
    this.serviceFileName = (String) statistics.get("service_file_name");
    this.serviceFilePath = (String) statistics.get("service_file_path");
    this.ridesharingFilePath = (String) statistics.get("ridesharing_file_path");
  }
}

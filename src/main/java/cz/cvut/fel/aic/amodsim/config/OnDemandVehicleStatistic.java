/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
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
package cz.cvut.fel.aic.amodsim.config;

import java.util.Map;

public class OnDemandVehicleStatistic {
  public String leaveStationFilePath;

  public String reachNearestStationFilePath;

  public String pickupFilePath;

  public String dropOffFilePath;

  public String finishRebalancingFilePath;

  public String dirPath;

  public String startRebalancingFilePath;

  public OnDemandVehicleStatistic(Map onDemandVehicleStatistic) {
    this.leaveStationFilePath = (String) onDemandVehicleStatistic.get("leave_station_file_path");
    this.reachNearestStationFilePath = (String) onDemandVehicleStatistic.get("reach_nearest_station_file_path");
    this.pickupFilePath = (String) onDemandVehicleStatistic.get("pickup_file_path");
    this.dropOffFilePath = (String) onDemandVehicleStatistic.get("drop_off_file_path");
    this.finishRebalancingFilePath = (String) onDemandVehicleStatistic.get("finish_rebalancing_file_path");
    this.dirPath = (String) onDemandVehicleStatistic.get("dir_path");
    this.startRebalancingFilePath = (String) onDemandVehicleStatistic.get("start_rebalancing_file_path");
  }
}

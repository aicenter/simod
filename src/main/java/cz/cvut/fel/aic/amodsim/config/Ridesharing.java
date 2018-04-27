package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Integer;
import java.util.Map;

public class Ridesharing {
  public Integer maxSpeedEstimation;

  public Integer vehicleCapacity;

  public Integer maxWaitTime;

  public Boolean on;

  public Ridesharing(Map ridesharing) {
    this.maxSpeedEstimation = (Integer) ridesharing.get("max_speed_estimation");
    this.vehicleCapacity = (Integer) ridesharing.get("vehicle_capacity");
    this.maxWaitTime = (Integer) ridesharing.get("max_wait_time");
    this.on = (Boolean) ridesharing.get("on");
  }
}

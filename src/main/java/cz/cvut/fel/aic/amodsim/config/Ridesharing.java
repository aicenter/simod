package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Integer;
import java.util.Map;

public class Ridesharing {
  public Integer chargingTime;

  public Integer drivingRange;

  public Integer maxRideTime;

  public Integer maxSpeedEstimation;

  public Integer vehicleCapacity;

  public Integer maxWaitTime;

  public Integer pickupRadius;

  public Boolean on;

  public Ridesharing(Map ridesharing) {
    this.chargingTime = (Integer) ridesharing.get("charging_time");
    this.drivingRange = (Integer) ridesharing.get("driving_range");
    this.maxRideTime = (Integer) ridesharing.get("max_ride_time");
    this.maxSpeedEstimation = (Integer) ridesharing.get("max_speed_estimation");
    this.vehicleCapacity = (Integer) ridesharing.get("vehicle_capacity");
    this.maxWaitTime = (Integer) ridesharing.get("max_wait_time");
    this.pickupRadius = (Integer) ridesharing.get("pickup_radius");
    this.on = (Boolean) ridesharing.get("on");
  }
}

package cz.cvut.fel.aic.simod.config;

import java.lang.Boolean;
import java.lang.Integer;
import java.util.Map;

public class Stations {
  public Integer vehicleBufferUnpopulatedStations;

  public Boolean on;

  public Integer vehicleBuffer;

  public Stations(Map stations) {
    this.vehicleBufferUnpopulatedStations = (Integer) stations.get("vehicle_buffer_unpopulated_stations");
    this.on = (Boolean) stations.get("on");
    this.vehicleBuffer = (Integer) stations.get("vehicle_buffer");
  }
}

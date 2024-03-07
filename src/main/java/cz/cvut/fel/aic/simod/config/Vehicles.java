package cz.cvut.fel.aic.simod.config;

import java.lang.Integer;
import java.util.Map;

public class Vehicles {
  public Integer minPauseLength;

  public Integer maxPauseInterval;

  public Vehicles(Map vehicles) {
    this.minPauseLength = (Integer) vehicles.get("min_pause_length");
    this.maxPauseInterval = (Integer) vehicles.get("max_pause_interval");
  }
}

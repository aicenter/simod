package cz.cvut.fel.aic.simod.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.util.Map;

public class Rebalancing {
  public Double bufferExcess;

  public Double bufferShortage;

  public Integer period;

  public Boolean on;

  public Rebalancing(Map rebalancing) {
    this.bufferExcess = (Double) rebalancing.get("buffer_excess");
    this.bufferShortage = (Double) rebalancing.get("buffer_shortage");
    this.period = (Integer) rebalancing.get("period");
    this.on = (Boolean) rebalancing.get("on");
  }
}

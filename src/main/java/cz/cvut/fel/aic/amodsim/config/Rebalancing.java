package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.util.Map;

public class Rebalancing {
  public Integer period;

  public Double buffer;

  public Boolean on;

  public Rebalancing(Map rebalancing) {
    this.period = (Integer) rebalancing.get("period");
    this.buffer = (Double) rebalancing.get("buffer");
    this.on = (Boolean) rebalancing.get("on");
  }
}

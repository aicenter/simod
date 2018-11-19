package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.util.Map;

public class AmodsimRebalancing {
  public Integer period;

  public Double buffer;

  public Boolean on;

  public AmodsimRebalancing(Map amodsimRebalancing) {
    this.period = (Integer) amodsimRebalancing.get("period");
    this.buffer = (Double) amodsimRebalancing.get("buffer");
    this.on = (Boolean) amodsimRebalancing.get("on");
  }
}

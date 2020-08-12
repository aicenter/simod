package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.util.Map;

public class Congestion {
  public Boolean on;

  public Congestion(Map congestion) {
    this.on = (Boolean) congestion.get("on");
  }
}

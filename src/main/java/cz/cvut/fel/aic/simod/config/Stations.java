package cz.cvut.fel.aic.simod.config;

import java.lang.Boolean;
import java.util.Map;

public class Stations {
  public Boolean on;

  public Stations(Map stations) {
    this.on = (Boolean) stations.get("on");
  }
}

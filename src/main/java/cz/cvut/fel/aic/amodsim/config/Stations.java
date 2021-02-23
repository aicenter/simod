package cz.cvut.fel.aic.amodsim.config;

import java.util.Map;

public class Stations {
  public Boolean on;

  public Stations(Map stations) {
    this.on = (Boolean) stations.get("on");
  }
}

package cz.cvut.fel.aic.amodsim.config;

import java.lang.Double;
import java.util.Map;

public class TutmProjectionCentre {
  public Double latitude;

  public Double longitude;

  public TutmProjectionCentre(Map tutmProjectionCentre) {
    this.latitude = (Double) tutmProjectionCentre.get("latitude");
    this.longitude = (Double) tutmProjectionCentre.get("longitude");
  }
}

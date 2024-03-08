package cz.cvut.fel.aic.simod.config;

import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class MaxTravelTimeDelay {
  public String mode;

  public Integer seconds;

  public Double relative;

  public MaxTravelTimeDelay(Map maxTravelTimeDelay) {
    this.mode = (String) maxTravelTimeDelay.get("mode");
    this.seconds = (Integer) maxTravelTimeDelay.get("seconds");
    this.relative = (Double) maxTravelTimeDelay.get("relative");
  }
}

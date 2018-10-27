package cz.cvut.fel.aic.amodsim.config;

import java.lang.Double;
import java.util.Map;

public class Vga {
  public Double maximumRelativeDiscomfort;

  public Double weightParameter;

  public Vga(Map vga) {
    this.maximumRelativeDiscomfort = (Double) vga.get("maximum_relative_discomfort");
    this.weightParameter = (Double) vga.get("weight_parameter");
  }
}

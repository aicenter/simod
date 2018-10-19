package cz.cvut.fel.aic.amodsim.config;

import java.lang.Double;
import java.util.Map;

public class Vga {
  public Double maximumRelativeDiscomfort;

  public Vga(Map vga) {
    this.maximumRelativeDiscomfort = (Double) vga.get("maximum_relative_discomfort");
  }
}

package cz.cvut.fel.aic.amodsim.config;

import java.lang.Double;
import java.lang.Integer;
import java.util.Map;

public class Vga {
  public Integer batchPeriod;

  public Double maximumRelativeDiscomfort;

  public Double weightParameter;

  public Vga(Map vga) {
    this.batchPeriod = (Integer) vga.get("batch_period");
    this.maximumRelativeDiscomfort = (Double) vga.get("maximum_relative_discomfort");
    this.weightParameter = (Double) vga.get("weight_parameter");
  }
}

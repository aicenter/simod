package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.String;
import java.util.Map;

public class Vga {
  public Boolean logPlanComputationalTime;

  public String groupGeneratorLogFilepath;

  public Vga(Map vga) {
    this.logPlanComputationalTime = (Boolean) vga.get("log_plan_computational_time");
    this.groupGeneratorLogFilepath = (String) vga.get("group_generator_log_filepath");
  }
}

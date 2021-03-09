package cz.cvut.fel.aic.amodsim.config;

import java.lang.Boolean;
import java.lang.Double;
import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Vga {
  public Double solverMinGap;

  public Boolean logPlanComputationalTime;

  public Integer maxGroupSize;

  public Integer solverTimeLimit;

  public Integer groupGenerationTimeLimit;

  public Integer solverMaxTripsPerRequest;

  public Boolean exportGroupData;

  public String groupGeneratorLogFilepath;

  public Vga(Map vga) {
    this.solverMinGap = (Double) vga.get("solver_min_gap");
    this.logPlanComputationalTime = (Boolean) vga.get("log_plan_computational_time");
    this.maxGroupSize = (Integer) vga.get("max_group_size");
    this.solverTimeLimit = (Integer) vga.get("solver_time_limit");
    this.groupGenerationTimeLimit = (Integer) vga.get("group_generation_time_limit");
    this.solverMaxTripsPerRequest = (Integer) vga.get("solver_max_trips_per_request");
    this.exportGroupData = (Boolean) vga.get("export_group_data");
    this.groupGeneratorLogFilepath = (String) vga.get("group_generator_log_filepath");
  }
}

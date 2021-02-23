package cz.cvut.fel.aic.amodsim.config;

import java.util.Map;

public class Vga {
  public Boolean logPlanComputationalTime;

  public Integer maxGroupSize;

  public Integer solverTimeLimit;

  public Integer groupGenerationTimeLimit;

  public Boolean exportGroupData;

  public String groupGeneratorLogFilepath;

  public Vga(Map vga) {
    this.logPlanComputationalTime = (Boolean) vga.get("log_plan_computational_time");
    this.maxGroupSize = (Integer) vga.get("max_group_size");
    this.solverTimeLimit = (Integer) vga.get("solver_time_limit");
    this.groupGenerationTimeLimit = (Integer) vga.get("group_generation_time_limit");
    this.exportGroupData = (Boolean) vga.get("export_group_data");
    this.groupGeneratorLogFilepath = (String) vga.get("group_generator_log_filepath");
  }
}

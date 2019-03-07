package cz.cvut.fel.aic.amodsim.config;

import java.lang.String;
import java.util.Map;

public class Vga {
  public String groupGeneratorLogFilepath;

  public Vga(Map vga) {
    this.groupGeneratorLogFilepath = (String) vga.get("group_generator_log_filepath");
  }
}

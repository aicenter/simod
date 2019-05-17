package cz.cvut.fel.aic.amodsim.config;

import java.lang.String;
import java.util.Map;

public class External {
  public String policyFilePath;

  public External(Map external) {
	this.policyFilePath = (String) external.get("policy_file_path");
  }
}

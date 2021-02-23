package cz.cvut.fel.aic.amodsim.config;

import java.util.Map;

public class InsertionHeuristic {
  public Boolean recomputeWaitingRequests;

  public InsertionHeuristic(Map insertionHeuristic) {
    this.recomputeWaitingRequests = (Boolean) insertionHeuristic.get("recompute_waiting_requests");
  }
}

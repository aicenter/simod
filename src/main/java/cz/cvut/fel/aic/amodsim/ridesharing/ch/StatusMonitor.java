package cz.cvut.fel.aic.amodsim.ridesharing.ch;

public interface StatusMonitor {
    
    public void updateStatus(MonitoredProcess process, long completed, long total);
    
}

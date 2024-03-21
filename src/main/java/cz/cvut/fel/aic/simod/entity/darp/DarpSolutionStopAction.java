package cz.cvut.fel.aic.simod.entity.darp;

public class DarpSolutionStopAction {
    private String arrival_time;
    private String departure_time;
    private DarpSolutionStopActionDetails action;

    public DarpSolutionStopAction(String arrival_time, String departure_time, DarpSolutionStopActionDetails action) {
        this.arrival_time = arrival_time;
        this.departure_time = departure_time;
        this.action = action;
    }
    

    public String getArrival_time() {
        return arrival_time;
    }

    public void setArrival_time(String arrival_time) {
        this.arrival_time = arrival_time;
    }

    public String getDeparture_time() {
        return departure_time;
    }

    public void setDeparture_time(String departure_time) {
        this.departure_time = departure_time;
    }

    public DarpSolutionStopActionDetails getAction() {
        return action;
    }

    public void setAction(DarpSolutionStopActionDetails action) {
        this.action = action;
    }
}

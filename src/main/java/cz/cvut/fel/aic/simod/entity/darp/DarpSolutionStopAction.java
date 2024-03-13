package cz.cvut.fel.aic.simod.entity.darp;

public class DarpSolutionStopAction {
    private int arrival_time;
    private int departure_time;
    private DarpSolutionStopActionDetails action;

    public DarpSolutionStopAction(int arrival_time, int departure_time, DarpSolutionStopActionDetails action) {
        this.arrival_time = arrival_time;
        this.departure_time = departure_time;
        this.action = action;
    }
    

    public int getArrival_time() {
        return arrival_time;
    }

    public void setArrival_time(int arrival_time) {
        this.arrival_time = arrival_time;
    }

    public int getDeparture_time() {
        return departure_time;
    }

    public void setDeparture_time(int departure_time) {
        this.departure_time = departure_time;
    }

    public DarpSolutionStopActionDetails getAction() {
        return action;
    }

    public void setAction(DarpSolutionStopActionDetails action) {
        this.action = action;
    }
}

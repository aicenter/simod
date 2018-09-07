package cz.cvut.fel.aic.amodsim.ridesharing.vga.model;

public class TimeWindow {
    
    private double end;
    private double start;
    
    public TimeWindow(double start, double end){
        this.end = end;
        this.start = start;        
    }
    
    public boolean isInWindow(double time){
        return  time >= start && time <= end;
    }
    
}

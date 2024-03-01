package cz.cvut.fel.aic.simod.entity.darp;

public class DarpSolutionPlanVehicle {
    private int index;
    private DarpSolutionPosition init_position;
    private int capacity;

    public DarpSolutionPlanVehicle(int index, DarpSolutionPosition init_position, int capacity) {
        this.index = index;
        this.init_position = init_position;
        this.capacity = capacity;
    }
    
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public DarpSolutionPosition getInit_position() {
        return init_position;
    }

    public void setInit_position(DarpSolutionPosition init_position) {
        this.init_position = init_position;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}

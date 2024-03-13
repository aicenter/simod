package cz.cvut.fel.aic.simod.entity.darp;

public class DarpSolutionStopActionDetails {
    private int id;
    private int request_index;
    private String type;
    private DarpSolutionPosition position;
    private int min_time;
    private int max_time;
    private int service_duration;

    public DarpSolutionStopActionDetails(int id, int request_index, String type, DarpSolutionPosition position, int min_time, int max_time, int service_duration) {
        this.id = id;
        this.request_index = request_index;
        this.type = type;
        this.position = position;
        this.min_time = min_time;
        this.max_time = max_time;
        this.service_duration = service_duration;
    }
    

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getRequest_index() {
        return request_index;
    }

    public void setRequest_index(int request_index) {
        this.request_index = request_index;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public DarpSolutionPosition getPosition() {
        return position;
    }

    public void setPosition(DarpSolutionPosition position) {
        this.position = position;
    }

    public int getMin_time() {
        return min_time;
    }

    public void setMin_time(int min_time) {
        this.min_time = min_time;
    }

    public int getMax_time() {
        return max_time;
    }

    public void setMax_time(int max_time) {
        this.max_time = max_time;
    }

    public int getService_duration() {
        return service_duration;
    }

    public void setService_duration(int service_duration) {
        this.service_duration = service_duration;
    }
}

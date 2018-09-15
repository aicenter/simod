
class Rebalancing:
    def __init__(self, properties: dict=None):
        self.timestep = properties.get("timestep")
        self.type = properties.get("type")
        self.file_path = properties.get("file_path")
        self.use_smoothed_demand = properties.get("use_smoothed_demand")
        self.max_wait_in_queue = properties.get("max_wait_in_queue")
        self.method = properties.get("method")
        self.vehicle_limit = properties.get("vehicle_limit")
        self.veh_coef = properties.get("veh_coef")
        self.load_shapes = properties.get("load_shapes")
        self.policy_file_path = properties.get("policy_file_path")


        pass


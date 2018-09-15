
class OnDemandVehicleStatistic:
    def __init__(self, properties: dict=None):
        self.dir_path = properties.get("dir_path")
        self.leave_station_file_path = properties.get("leave_station_file_path")
        self.pickup_file_path = properties.get("pickup_file_path")
        self.drop_off_file_path = properties.get("drop_off_file_path")
        self.reach_nearest_station_file_path = properties.get("reach_nearest_station_file_path")
        self.start_rebalancing_file_path = properties.get("start_rebalancing_file_path")
        self.finish_rebalancing_file_path = properties.get("finish_rebalancing_file_path")


        pass


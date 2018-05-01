

from amodsim.config.on_demand_vehicle_statistic import OnDemandVehicleStatistic

class Statistics:

    def __init__(self, properties: dict=None):
        self.statistic_interval_milis = properties.get("statistic_interval_milis")
        self.result_file_name = properties.get("result_file_name")
        self.result_file_path = properties.get("result_file_path")
        self.all_edges_load_interval_milis = properties.get("all_edges_load_interval_milis")
        self.all_edges_load_history_file_name = properties.get("all_edges_load_history_file_name")
        self.all_edges_load_history_file_path = properties.get("all_edges_load_history_file_path")
        self.transit_statistic_file_path = properties.get("transit_statistic_file_path")
        self.service_file_name = properties.get("service_file_name")
        self.service_file_path = properties.get("service_file_path")
        self.trip_distances_file_path = properties.get("trip_distances_file_path")
        self.occupancies_file_name = properties.get("occupancies_file_name")
        self.occupancies_file_path = properties.get("occupancies_file_path")

        self.on_demand_vehicle_statistic = OnDemandVehicleStatistic(properties.get("on_demand_vehicle_statistic"))


        pass


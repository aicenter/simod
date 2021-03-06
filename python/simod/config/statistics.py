
class Statistics:

    def __init__(self, properties: dict = None):
        self.result_file_name = properties.get("result_file_name")
        self.result_file_path = properties.get("result_file_path")
        self.all_edges_load_history_file_name = properties.get("all_edges_load_history_file_name")
        self.all_edges_load_history_file_path = properties.get("all_edges_load_history_file_path")
        self.occupancies_file_name = properties.get("occupancies_file_name")
        self.occupancies_file_path = properties.get("occupancies_file_path")
        self.service_file_name = properties.get("service_file_name")
        self.transit_file_name = properties.get("transit_file_name")
        self.ridesharing_stats_file_name = properties.get("ridesharing_stats_file_name")
        self.trip_distances_file_path = properties.get("trip_distances_file_path")
        pass

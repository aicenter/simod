
class Statistics:

    def __init__(self, properties: dict = None):
        self.result_file_name = properties.get("result_file_name")
        self.result_file_path = properties.get("result_file_path")
        self.all_edges_load_history_file_name = properties.get("all_edges_load_history_file_name")
        self.all_edges_load_history_file_path = properties.get("all_edges_load_history_file_path")

        self.occupancies_file_name = properties.get("occupancies_file_name")
        self.occupancies_file_path = properties.get("occupancies_file_path")
        self.packages_occupancies_file_name = properties.get("packages_occupancies_file_name")
        self.packages_occupancies_file_path = properties.get("packages_occupancies_file_path")

        self.no_people_occupancies_file_name = properties.get("no_people_occupancies_file_name")
        self.no_people_occupancies_file_path = properties.get("no_people_occupancies_file_path")
        self.people_onboard_occupancies_file_name = properties.get("people_onboard_occupancies_file_name")
        self.people_onboard_occupancies_file_path = properties.get("people_onboard_occupancies_file_path")

        self.service_file_name = properties.get("service_file_name")
        self.transit_file_name = properties.get("transit_file_name")
        self.ridesharing_stats_file_name = properties.get("ridesharing_stats_file_name")
        self.trip_distances_file_path = properties.get("trip_distances_file_path")
        pass

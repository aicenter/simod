
class Stations:
    def __init__(self, properties: dict=None):
        self.regions = properties.get("regions")
        self.timestamps = properties.get("timestamps")
        self.dir = properties.get("dir")
        self.stations_file_path = properties.get("stations_file_path")
        self.demand_file_path = properties.get("demand_file_path")
        self.centroids_file_path = properties.get("centroids_file_path")
        self.distance_calculator_path = properties.get("distance_calculator_path")
        self.distance_matrix_output_path = properties.get("distance_matrix_output_path")
        self.distance_matrix_path = properties.get("distance_matrix_path")
        self.smoothed_demand_file_path = properties.get("smoothed_demand_file_path")


        pass


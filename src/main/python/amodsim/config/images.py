


class Images:

    def __init__(self, properties: dict=None):
        self.images_dir = properties.get("images_dir")
        self.images_experiment_dir = properties.get("images_experiment_dir")
        self.trip_start_histogram = properties.get("trip_start_histogram")
        self.main_map = properties.get("main_map")
        self.traffic_density_current = properties.get("traffic_density_current")
        self.traffic_density_current_detail = properties.get("traffic_density_current_detail")
        self.traffic_density_future_detail = properties.get("traffic_density_future_detail")
        self.traffic_density_future_detail_stacked = properties.get("traffic_density_future_detail_stacked")
        self.occupancy_histogram = properties.get("occupancy_histogram")
        self.occupancy_histogram_window = properties.get("occupancy_histogram_window")
        self.comparison_dir = properties.get("comparison_dir")
        self.traffic_density_map_comparison = properties.get("traffic_density_map_comparison")
        self.traffic_density_histogram_comparison = properties.get("traffic_density_histogram_comparison")
        self.wait_time_comparison_dir = properties.get("wait_time_comparison_dir")
        self.occupancy_comparison = properties.get("occupancy_comparison")
        self.distance_comparison = properties.get("distance_comparison")
        self.service_comparison = properties.get("service_comparison")
        self.dropped_demands_comparison = properties.get("dropped_demands_comparison")



        pass


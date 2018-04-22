


class Images:

    def __init__(self, properties: dict=None):
        self.images_dir = properties.get("images_dir")
        self.trip_start_histogram = properties.get("trip_start_histogram")
        self.main_map = properties.get("main_map")
        self.traffic_density_current = properties.get("traffic_density_current")
        self.traffic_density_current_detail = properties.get("traffic_density_current_detail")
        self.traffic_density_future_detail = properties.get("traffic_density_future_detail")
        self.traffic_density_future_detail_stacked = properties.get("traffic_density_future_detail_stacked")



        pass


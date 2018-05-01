

from amodsim.config.ridesharing import Ridesharing
from amodsim.config.statistics import Statistics

class Amodsim:

    def __init__(self, properties: dict=None):
        self.preprocessor_path = properties.get("preprocessor_path")
        self.trips_path = properties.get("trips_path")
        self.preprocessed_trips = properties.get("preprocessed_trips")
        self.start_time = properties.get("start_time")
        self.use_trip_cache = properties.get("use_trip_cache")
        self.trip_cache_file = properties.get("trip_cache_file")
        self.edges_file_path = properties.get("edges_file_path")
        self.edge_pairs_file_path = properties.get("edge_pairs_file_path")
        self.simplify_graph = properties.get("simplify_graph")

        self.ridesharing = Ridesharing(properties.get("ridesharing"))
        self.statistics = Statistics(properties.get("statistics"))


        pass


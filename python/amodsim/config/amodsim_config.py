
import fconfig.configuration

from fconfig.config import Config
from amodsim.config.statistics import Statistics
from amodsim.config.analysis import Analysis
from amodsim.config.images import Images
from amodsim.config.comparison import Comparison
from amodsim.config.ridesharing import Ridesharing
from amodsim.config.agentpolis import Agentpolis
import roadmaptools.config.roadmaptools_config
from roadmaptools.config.roadmaptools_config import RoadmaptoolsConfig

class AmodsimConfig(Config):
    def __init__(self, properties: dict=None):
        self.data_dir = properties.get("data_dir")
        self.map_dir = properties.get("map_dir")
        self.experiments_dir = properties.get("experiments_dir")
        self.rci_experiments_dir = properties.get("rci_experiments_dir")
        self.experiment_name = properties.get("experiment_name")
        self.experiment_dir = properties.get("experiment_dir")
        self.critical_density = properties.get("critical_density")
        self.simplify_graph = properties.get("simplify_graph")
        self.edges_file_path = properties.get("edges_file_path")
        self.edge_pairs_file_path = properties.get("edge_pairs_file_path")
        self.trips_filename = properties.get("trips_filename")
        self.trips_path = properties.get("trips_path")
        self.distance_matrix_filepath = properties.get("distance_matrix_filepath")
        self.station_position_filepath = properties.get("station_position_filepath")
        self.vehicle_speed_in_meters_per_second = properties.get("vehicle_speed_in_meters_per_second")

        self.statistics = Statistics(properties.get("statistics"))
        self.analysis = Analysis(properties.get("analysis"))
        self.images = Images(properties.get("images"))
        self.comparison = Comparison(properties.get("comparison"))
        self.ridesharing = Ridesharing(properties.get("ridesharing"))
        self.agentpolis = Agentpolis(properties.get("agentpolis"))
        self.roadmaptools = RoadmaptoolsConfig(properties.get("roadmaptools"))
        roadmaptools.config.roadmaptools_config.config = self.roadmaptools

        pass

config: AmodsimConfig = fconfig.configuration.load((RoadmaptoolsConfig, 'roadmaptools'), (AmodsimConfig, None))




import fconfig.configuration

from fconfig.config import Config
from amodsim.config.db import Db
from amodsim.config.tutm_projection_centre import TutmProjectionCentre
from amodsim.config.stations import Stations
from amodsim.config.shapefiles import Shapefiles
from amodsim.config.rebalancing import Rebalancing
from amodsim.config.agentpolis import Agentpolis
import roadmaptools.config.roadmaptools_config
from roadmaptools.config.roadmaptools_config import RoadmaptoolsConfig
from amodsim.config.amodsim import Amodsim
from amodsim.config.analysis import Analysis
from amodsim.config.images import Images

class AmodsimConfig(Config):
    def __init__(self, properties: dict=None):
        self.experiment_name = properties.get("experiment_name")
        self.amodsim_data_dir = properties.get("amodsim_data_dir")
        self.python_data_dir = properties.get("python_data_dir")
        self.python_experiment_dir = properties.get("python_experiment_dir")
        self.amodsim_experiment_dir = properties.get("amodsim_experiment_dir")
        self.map_dir = properties.get("map_dir")
        self.srid = properties.get("srid")
        self.trips_filename = properties.get("trips_filename")
        self.trips_file_path = properties.get("trips_file_path")
        self.trips_multiplier = properties.get("trips_multiplier")
        self.vehicle_speed_in_meters = properties.get("vehicle_speed_in_meters")
        self.trips_limit = properties.get("trips_limit")
        self.critical_density = properties.get("critical_density")

        self.db = Db(properties.get("db"))
        self.tutm_projection_centre = TutmProjectionCentre(properties.get("tutm_projection_centre"))
        self.stations = Stations(properties.get("stations"))
        self.shapefiles = Shapefiles(properties.get("shapefiles"))
        self.rebalancing = Rebalancing(properties.get("rebalancing"))
        self.agentpolis = Agentpolis(properties.get("agentpolis"))
        self.roadmaptools = RoadmaptoolsConfig(properties.get("roadmaptools"))
        roadmaptools.config.roadmaptools_config.config = self.roadmaptools
        self.amodsim = Amodsim(properties.get("amodsim"))
        self.analysis = Analysis(properties.get("analysis"))
        self.images = Images(properties.get("images"))

        pass

config: AmodsimConfig = fconfig.configuration.load((RoadmaptoolsConfig, 'roadmaptools'), (AmodsimConfig, None))



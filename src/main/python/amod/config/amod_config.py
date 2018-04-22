
from fconfig.config import Config


from amod.config.db import Db
from amod.config.tutm_projection_centre import TutmProjectionCentre
from amod.config.stations import Stations
from amod.config.shapefiles import Shapefiles
from amod.config.rebalancing import Rebalancing
from amod.config.amodsim import Amodsim
from amod.config.analysis import Analysis
from amod.config.images import Images

class AmodConfig(Config):

    def __init__(self):
        self.experiment_name = None
        self.amodsim_data_dir = None
        self.python_data_dir = None
        self.python_experiment_dir = None
        self.amodsim_experiment_dir = None
        self.map_nodes_filepath = None
        self.map_edges_filepath = None
        self.srid = None
        self.trips_filename = None
        self.trips_file_path = None
        self.trips_multiplier = None
        self.vehicle_speed_in_meters = None
        self.trips_limit = None
        self.critical_density = None

        self.db = None
        self.tutm_projection_centre = None
        self.stations = None
        self.shapefiles = None
        self.rebalancing = None
        self.amodsim = None
        self.analysis = None
        self.images = None


        pass

    def fill(self, properties: dict=None):
        self.experiment_name = properties.get("experiment_name")
        self.amodsim_data_dir = properties.get("amodsim_data_dir")
        self.python_data_dir = properties.get("python_data_dir")
        self.python_experiment_dir = properties.get("python_experiment_dir")
        self.amodsim_experiment_dir = properties.get("amodsim_experiment_dir")
        self.map_nodes_filepath = properties.get("map_nodes_filepath")
        self.map_edges_filepath = properties.get("map_edges_filepath")
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
        self.amodsim = Amodsim(properties.get("amodsim"))
        self.analysis = Analysis(properties.get("analysis"))
        self.images = Images(properties.get("images"))


        pass


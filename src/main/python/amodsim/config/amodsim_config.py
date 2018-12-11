
import fconfig.configuration

from fconfig.config import Config
from amodsim.config.amodsim import Amodsim
import roadmaptools.config.roadmaptools_config
from roadmaptools.config.roadmaptools_config import RoadmaptoolsConfig

class AmodsimConfig(Config):
    def __init__(self, properties: dict=None):
        self.experiment_name = properties.get("experiment_name")
        self.amodsim_experiment_dir = properties.get("amodsim_experiment_dir")

        self.amodsim = Amodsim(properties.get("amodsim"))
        self.roadmaptools = RoadmaptoolsConfig(properties.get("roadmaptools"))
        roadmaptools.config.roadmaptools_config.config = self.roadmaptools

        pass

config: AmodsimConfig = fconfig.configuration.load((RoadmaptoolsConfig, 'roadmaptools'), (AmodsimConfig, None))



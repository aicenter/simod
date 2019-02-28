
import fconfig.configuration

from fconfig.config import Config
from amodsim.config.amodsim import Amodsim
from amodsim.config.analysis import Analysis
from amodsim.config.images import Images
import roadmaptools.config.roadmaptools_config
from roadmaptools.config.roadmaptools_config import RoadmaptoolsConfig

class AmodsimConfig(Config):
    def __init__(self, properties: dict=None):
        self.experiment_name = properties.get("experiment_name")
        self.experiment_dir = properties.get("experiment_dir")

        self.amodsim = Amodsim(properties.get("amodsim"))
        self.analysis = Analysis(properties.get("analysis"))
        self.images = Images(properties.get("images"))
        self.roadmaptools = RoadmaptoolsConfig(properties.get("roadmaptools"))
        roadmaptools.config.roadmaptools_config.config = self.roadmaptools

        pass

config: AmodsimConfig = fconfig.configuration.load((RoadmaptoolsConfig, 'roadmaptools'), (AmodsimConfig, None))



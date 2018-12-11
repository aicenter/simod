
from amodsim.config.statistics import Statistics
class Amodsim:
    def __init__(self, properties: dict=None):

        self.statistics = Statistics(properties.get("statistics"))

        pass


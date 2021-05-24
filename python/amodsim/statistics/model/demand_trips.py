from amodsim.init import config

import numpy as np


def load() -> np.ndarray:
    trips_data = np.genfromtxt(config.statistics.trip_distances_file_path)
    return trips_data
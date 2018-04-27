
from amodsim.init import config, roadmaptools_config

from tqdm import tqdm
import numpy as np

import roadmaptools.inout


data = np.genfromtxt(config.amodsim.statistics.occupancies_file_path, delimiter=',')

occupancy_col = data[:,2]

avg_occupancy = np.mean(occupancy_col)

avg_occupancy_demands = np.sum(occupancy_col) / np.count_nonzero(occupancy_col)

print("Average occupancy: {}".format(avg_occupancy))

print("Average occupancy demands: {}".format(avg_occupancy_demands))


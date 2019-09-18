
from amodsim.init import config, roadmaptools_config

from tqdm import tqdm
import numpy as np
import matplotlib.pyplot as plt
import matplotlib
import roadmaptools.inout
import amodsim.utils

from matplotlib.ticker import FuncFormatter
from pandas import DataFrame, Series


data = np.genfromtxt(config.amodsim.statistics.service_file_path, delimiter=',')

df = DataFrame(data, columns=["demand_time", "demand_id", "vehicle_id", "pickup_time", "dropoff_time", "min_possible_delay"])

delays = df["dropoff_time"] - df["demand_time"]

min_delays = df["min_possible_delay"]

prolongations = delays - min_delays

print("Average prolongation: {}".format(Series.mean(prolongations)))

plt.hist(prolongations / 60000, bins=30)

plt.show()
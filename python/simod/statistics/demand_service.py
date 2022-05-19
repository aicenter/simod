#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of the SiMoD project.
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU Lesser General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.
#

from simod.init import config
from roadmaptools.config import roadmaptools_config

from tqdm import tqdm
import numpy as np
import matplotlib.pyplot as plt
import matplotlib
import roadmaptools.inout
import simod.utils

from matplotlib.ticker import FuncFormatter
from pandas import DataFrame, Series


data = np.genfromtxt(config.simod.statistics.service_file_path, delimiter=',')

df = DataFrame(data, columns=["demand_time", "demand_id", "vehicle_id", "pickup_time", "dropoff_time", "min_possible_delay"])

delays = df["dropoff_time"] - df["demand_time"]

min_delays = df["min_possible_delay"]

prolongations = delays - min_delays

print("Average prolongation: {}".format(Series.mean(prolongations)))

plt.hist(prolongations / 60000, bins=30)

plt.show()
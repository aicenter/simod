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
from amodsim.init import config

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter
from amodsim.statistics.model import demand_trips

trips_data = demand_trips.load()

# trip start histogram
fig, axis = plt.subplots(figsize=(6, 4))

trip_data_minutes = trips_data / 60

print("Max trip length: {} min".format(trip_data_minutes.max()))
print("Min trip length: {} s".format(trips_data.min()))

bins = np.arange(0.49, 37.51, 1)

_counts, _bins, patches = axis.hist(trip_data_minutes, bins, color="white", edgecolor="black")

# tick_interval = int(HISTOGRAM_SAMPLES / 10)

# axis.set_xticks(bins[0::tick_interval])
# axis.xaxis.set_major_formatter(FuncFormatter(format_timestamps))
labels = axis.get_xticklabels()
plt.subplots_adjust(bottom=0.2)
axis.get_xaxis().set_tick_params(direction='out')

axis.set_xlabel("Trip duration [min]")

plt.savefig(config.images.demand_trip_duration_histogram, bbox_inches='tight', transparent=True, pad_inches=0)

plt.show()
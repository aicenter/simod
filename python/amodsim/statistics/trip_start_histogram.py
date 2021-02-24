#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of Amodsim project.
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

import matplotlib
import matplotlib.pyplot as plt
from matplotlib.ticker import FuncFormatter
from amodsim.statistics.model import trips

HOURS_IN_DAY = 24

MINUTES_IN_HOUR = 60

HISTOGRAM_SAMPLES = 96


def format_timestamps(tick, tick_index):
	hours, rem = divmod(tick_index * tick_interval, HISTOGRAM_SAMPLES / HOURS_IN_DAY)
	minutes = rem * MINUTES_IN_HOUR / (HISTOGRAM_SAMPLES / HOURS_IN_DAY)
	return "{:0>2}:{:0>2}".format(int(hours), int(minutes))


trips_data = trips.load()


fig, axis = plt.subplots(figsize=(6, 4))

counts, bins, patches = axis.hist(trips_data["start_time"], HISTOGRAM_SAMPLES)

tick_interval = int(HISTOGRAM_SAMPLES / 16)

axis.set_xticks(bins[0::tick_interval])
axis.xaxis.set_major_formatter(FuncFormatter(format_timestamps))
labels = axis.get_xticklabels()
plt.setp(labels, rotation=90)
plt.subplots_adjust(bottom=0.2)
axis.get_xaxis().set_tick_params(direction='out')


# plt.savefig(config.images.trip_start_histogram, bbox_inches='tight', transparent=True, pad_inches=0)

plt.show()
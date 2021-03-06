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

import matplotlib.pyplot as plt
import numpy as np
import statistics.model.traffic_load as traffic_load

from matplotlib.axes import Axes
from amodsim.statistics.traffic_density_histogram import TrafficDensityHistogram, HIGH_THRESHOLD, HISTOGRAM_SAMPLES
from statistics.model.traffic_load import VehiclePhase


def configure_axis(axes: Axes, first_line=True):
	axes.grid(True)

	# critical density line
	axes.axvline(x=config.critical_density, linewidth=3, color='black', linestyle='--', label='critical density')

	# legend
	axes.legend(prop={'size': 13})

	if first_line:
		axes.set_xlabel("traffic density")

		# limits
		axes.set_xlim(0.04, 0.16)
		axes.set_ylim(0, 300)
	else:
		axes.set_ylim(0, 12000)


edges = traffic_load.load_edges_mapped_by_id()

loads_capacity_1 = traffic_load.load_all_edges_load_history(config.analysis.edge_load_ridesharing_off_filepath)

loads_capacity_5 = traffic_load.load_all_edges_load_history(config.analysis.edge_load_ridesharing_on_filepath)

fig, axis = \
		plt.subplots(1, 3, sharex=True, sharey=False, subplot_kw={"adjustable": 'box'}, figsize=(12, 3))

# axis configuration
# cannot be used because of the double call on the first element
# np.vectorize(configure_axis, otypes="None")(list(axis))
for axes in axis:
	configure_axis(axes)
axis[0].set_ylabel("edge count")

histogram = TrafficDensityHistogram(edges)

# for the sum of right outliers
hist_step = HIGH_THRESHOLD / HISTOGRAM_SAMPLES
bins = np.arange(0, HIGH_THRESHOLD + hist_step, hist_step)
centers = [x + (hist_step / 2) for x in bins[0:HISTOGRAM_SAMPLES]]
colors = np.asarray(list(map(traffic_load.get_color_from_normalized_load, np.copy(bins))))


# curent histogram
average_density_list_total_current = histogram.get_average_density_list(loads_capacity_1[VehiclePhase.DRIVING_TO_TARGET_LOCATION.name])
hist = histogram.plot_state(axis[0], average_density_list_total_current, hist_step, bins, centers, colors)

# MoD histogram
average_density_list_total_future = histogram.get_average_density_list(loads_capacity_1["ALL"])
hist_total = histogram.plot_state(axis[1], average_density_list_total_future, hist_step, bins, centers, colors)

# MoD histogram
average_density_list_mod_ridesharing = histogram.get_average_density_list(loads_capacity_5["ALL"])
hist_mod_ridesharing = histogram.plot_state(axis[2], average_density_list_mod_ridesharing, hist_step, bins, centers, colors)

plt.savefig(config.images.traffic_density_histogram_comparison, bbox_inches='tight', transparent=True)


# alternative look
fig, axis = \
		plt.subplots(2, 3, sharex=False, sharey=False, subplot_kw={"adjustable": 'box'}, figsize=(12, 4))
for axes in axis[0]:
	configure_axis(axes, False)
for axes in axis[1]:
	configure_axis(axes)
axis[0][0].set_ylabel("edge count")
axis[1][0].set_ylabel("edge count")


# curent histogram
average_density_list_total_current = histogram.get_average_density_list(loads_capacity_1[VehiclePhase.DRIVING_TO_TARGET_LOCATION.name])
hist = histogram.plot_state(axis[0][0], average_density_list_total_current, hist_step, bins, centers, colors)

# MoD histogram
average_density_list_total_future = histogram.get_average_density_list(loads_capacity_1["ALL"])
hist_total = histogram.plot_state(axis[0][1], average_density_list_total_future, hist_step, bins, centers, colors)

# MoD histogram
average_density_list_mod_ridesharing = histogram.get_average_density_list(loads_capacity_5["ALL"])
hist_mod_ridesharing = histogram.plot_state(axis[0][2], average_density_list_mod_ridesharing, hist_step, bins, centers, colors)

# curent histogram
average_density_list_total_current = histogram.get_average_density_list(loads_capacity_1[VehiclePhase.DRIVING_TO_TARGET_LOCATION.name])
hist = histogram.plot_state(axis[1][0], average_density_list_total_current, hist_step, bins, centers, colors)

# MoD histogram
average_density_list_total_future = histogram.get_average_density_list(loads_capacity_1["ALL"])
hist_total = histogram.plot_state(axis[1][1], average_density_list_total_future, hist_step, bins, centers, colors)

# MoD histogram
average_density_list_mod_ridesharing = histogram.get_average_density_list(loads_capacity_5["ALL"])
hist_mod_ridesharing = histogram.plot_state(axis[1][2], average_density_list_mod_ridesharing, hist_step, bins, centers, colors)

plt.savefig(config.images.traffic_density_histogram_comparison_alt, bbox_inches='tight', transparent=True)

plt.show()
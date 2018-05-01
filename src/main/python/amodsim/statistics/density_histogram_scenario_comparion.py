
from amodsim.init import config, roadmaptools_config

import matplotlib.pyplot as plt
import numpy as np
import amodsim.traffic_load as traffic_load

from matplotlib import rcParams
from matplotlib.axes import Axes
from roadmaptools.printer import print_info
from amodsim.statistics.traffic_density_histogram import TrafficDensityHistogram, HIGH_THRESHOLD, LOW_THRESHOLD,\
	HISTOGRAM_SAMPLES
from amodsim.traffic_load import VehiclePhase


def configure_axis(axes: Axes):
	axes.set_xlabel("traffic density")
	axes.grid(True)

	# critical density line
	axes.axvline(x=config.critical_density, linewidth=3, color='black', linestyle='--', label='critical density')

	# legend
	axes.legend(prop={'size': 13})

	# limits
	axes.set_xlim(0.04, 0.16)
	axes.set_ylim(0, 220)


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
average_density_list_total_current = histogram.get_average_density_list(loads_capacity_1[VehiclePhase.DRIVING_TO_TARGET_LOCATION.name]);
hist = histogram.plot_state(axis[0], average_density_list_total_current, hist_step, bins, centers, colors)

# MoD histogram
average_density_list_total_future = histogram.get_average_density_list(loads_capacity_1["ALL"]);
hist_total = histogram.plot_state(axis[1], average_density_list_total_future, hist_step, bins, centers, colors)

# MoD histogram
average_density_list_mod_ridesharing = histogram.get_average_density_list(loads_capacity_5["ALL"]);
hist_mod_ridesharing = histogram.plot_state(axis[2], average_density_list_mod_ridesharing, hist_step, bins, centers, colors)

plt.savefig(config.images.traffic_density_histogram_comparison, bbox_inches='tight', transparent=True)

plt.show()
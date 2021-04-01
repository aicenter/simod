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
import amodsim.statistics.model.edges as edges
import amodsim.statistics.model.transit as transit
import amodsim.statistics.model.service as service

from typing import Tuple, List
from pandas import DataFrame
from amodsim.statistics.model.vehicle_state import VehicleState

FONT_SIZE = 18

matplotlib.rcParams['text.usetex'] = True
matplotlib.rcParams.update({'font.size': FONT_SIZE})


def get_data_for_dir_current(experiment_dir: str, edge_data: DataFrame) -> Tuple[int, float]:

	# km total
	transit_data = transit.load(experiment_dir)
	km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True,
														   VehicleState.DRIVING_TO_TARGET_LOCATION) / 1000 / 100 / 1000))

	# delay
	service_stat = service.load_dataframe(experiment_dir)
	delays_window = service.get_delays(service_stat, True, False)
	mean_delay = int(round(delays_window.mean() / 1000))

	return km_total_window, 0


def get_data_for_dir(experiment_dir: str, edge_data: DataFrame) -> Tuple[int, float]:

	# km total
	transit_data = transit.load(experiment_dir)
	km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True) / 1000 / 100 / 1000))

	# delay
	service_stat = service.load_dataframe(experiment_dir)
	delays_window = service.get_delays(service_stat, True, False)
	mean_delay = int(round(delays_window.mean() / 1000))

	return km_total_window, mean_delay


def plot_data_for_window(
		axis,
		edge_data: DataFrame,
		dir_cap_1: str,
		dir_ih: str,
		dir_vga: str,
		dir_vga_limited: str,
		dir_vga_pnas: str
):
	labels = ['Current State', 'No Ridesharing', 'IH', 'VGA (optimal)', 'VGA (limited)', 'VGA (PNAS)']
	label_offsets = [(0, 30), (-30, -35), (50, 0), (55, 30), (25, -50), (90, 0)]
	distances = []
	delays = []
	cur_state = True
	for dir in [dir_cap_1, dir_cap_1, dir_ih, dir_vga, dir_vga_limited, dir_vga_pnas]:
		if cur_state:
			distance, delay = get_data_for_dir_current(dir, edge_data)
		else:
			distance, delay = get_data_for_dir(dir, edge_data)
		distances.append(distance)
		delays.append(delay)
		cur_state = False

	axis.scatter(distances, delays, marker='x')

	for index, label in enumerate(labels):
		axis.annotate(label,  # this is the text
				 (distances[index], delays[index]),  # this is the point to label
				 textcoords="offset pixels",  # how to position the text
				 xytext=label_offsets[index],  # distance from text to points (x,y)
				 ha='center',  # horizontal alignment can be left, right or center
				va='center',
				arrowprops={'arrowstyle': '-', 'shrinkA': 0, 'shrinkB': 5})

	axis.set_ylim(min(delays) - 10, max(delays) + 50)


edge_data = edges.load_table()

exp_dir_1 = config.comparison.experiment_1_dir
exp_dir_2 = config.comparison.experiment_2_dir
exp_dir_3 = config.comparison.experiment_3_dir
exp_dir_4 = config.comparison.experiment_4_dir
exp_dir_5 = config.comparison.experiment_5_dir
exp_dir_6 = config.comparison.experiment_6_dir
exp_dir_7 = config.comparison.experiment_7_dir
exp_dir_8 = config.comparison.experiment_8_dir
exp_dir_9 = config.comparison.experiment_9_dir
exp_dir_10 = config.comparison.experiment_10_dir

fig, axes = plt.subplots(1, 2, figsize=(8, 3))

# decrease space between subplots
fig.subplots_adjust(wspace=0.05)

axis1 = axes[0]
axis2 = axes[1]

axis1.set_title("a) Peak")
axis1.set_xlabel(r"Total Distance [km $\cdot 10^3$]")
axis1.set_ylabel("Avg. Delay [s]")
axis2.set_title("b) Off-peak")
axis2.set_xlabel("Total Distance [km $\cdot 10^3$]")
axis2.tick_params(
    axis='y',          # changes apply to the x-axis
    which='both',      # both major and minor ticks are affected
    left=False,      # ticks along the bottom edge are off
    right=False,         # ticks along the top edge are off
    labelleft=False,
	labelright=False)
axis1.set_xlim(330, 1100)
axis2.set_xlim(150, 380)

plot_data_for_window(axis1, edge_data, exp_dir_1, exp_dir_2, exp_dir_3, exp_dir_4, exp_dir_9)
plot_data_for_window(axis2, edge_data, exp_dir_5, exp_dir_6, exp_dir_7, exp_dir_8, exp_dir_10)

plt.savefig(config.images.distance_delay_tradeoff, bbox_inches='tight', transparent=True, pad_inches=0.0, dpi=fig.dpi)

plt.show()

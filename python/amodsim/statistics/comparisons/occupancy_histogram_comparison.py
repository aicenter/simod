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

import numpy as np
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import datetime
import roadmaptools.inout
import amodsim.statistics.model.occupancy as occupancy
import amodsim.statistics.comparisons.common as common

from matplotlib.ticker import FuncFormatter

FONT_SIZE = 16

matplotlib.rcParams['text.usetex'] = True
matplotlib.rcParams.update({'font.size': FONT_SIZE})


def format_time(minutes: int, position) -> str:
	# return str(datetime.timedelta(minutes=minutes))
	return str(int(round(minutes / 60)))


def configure_subplot(axis):
	axis.yaxis.set_ticks(np.arange(0, 1020001, 120000))
	axis.yaxis.set_major_formatter(FuncFormatter(format_time))
	axis.xaxis.set_ticks(np.arange(0, 6, 1))


data_1 = occupancy.load(config.comparison.experiment_1_dir)
data_2 = occupancy.load(config.comparison.experiment_2_dir)
data_3 = occupancy.load(config.comparison.experiment_3_dir)
data_4 = occupancy.load(config.comparison.experiment_4_dir)

occupancies_1 = occupancy.get_occupancies(data_1)
occupancies_2 = occupancy.get_occupancies(data_2)
occupancies_3 = occupancy.get_occupancies(data_3)
occupancies_4 = occupancy.get_occupancies(data_4)

occupancies_in_window_2 = occupancy.get_occupancies(data_1, True)
occupancies_in_window_3 = occupancy.get_occupancies(data_2, True)
occupancies_in_window_4 = occupancy.get_occupancies(data_3, True)
occupancies_in_window_5 = occupancy.get_occupancies(data_4, True)
occupancies_in_window_1 = pd.Series(1, index=np.arange(len(occupancies_in_window_2)))

bins = np.arange(-0.5, 6.5, 1)

hatches = ['\\\\', '//', '++', '**']


fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
_n, _bins, patches = axes.hist([occupancies_1, occupancies_2, occupancies_3, occupancies_4], bins,
		  label=['No Ridesharing', 'Insertion Heuristic', 'VGA', 'VGA limited'])
axes.yaxis.set_ticks(np.arange(0, 1200001, 120000))
axes.yaxis.set_major_formatter(FuncFormatter(format_time))
axes.set_ylabel("vehicle hours")

for patch_set, hatch in zip(patches, hatches):
	plt.setp(patch_set, hatch=hatch)

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
_n, _bins, patches = axes.hist([occupancies_in_window_1, occupancies_in_window_2, occupancies_in_window_3,
								occupancies_in_window_4, occupancies_in_window_4], bins,
		  label=common.labels)
axes.yaxis.set_ticks(np.arange(0, 840001, 120000))
axes.yaxis.set_major_formatter(FuncFormatter(format_time))
axes.set_ylabel("vehicle hours")

for patch_set, hatch in zip(patches, hatches):
	plt.setp(patch_set, hatch=hatch)

plt.legend(loc='upper right')

# plt.savefig(config.images.occupancy_histogram_comparison, bbox_inches='tight', transparent=True)


# combined histograms
data_5 = occupancy.load(config.comparison.experiment_5_dir)
data_6 = occupancy.load(config.comparison.experiment_6_dir)
data_7 = occupancy.load(config.comparison.experiment_7_dir)
data_8 = occupancy.load(config.comparison.experiment_8_dir)

# occupancies_1 = occupancy.get_occupancies(data_1)
# occupancies_2 = occupancy.get_occupancies(data_2)
# occupancies_3 = occupancy.get_occupancies(data_3)
# occupancies_4 = occupancy.get_occupancies(data_4)

occupancies_in_window_7 = occupancy.get_occupancies(data_5, True)
occupancies_in_window_8 = occupancy.get_occupancies(data_6, True)
occupancies_in_window_9 = occupancy.get_occupancies(data_7, True)
occupancies_in_window_10 = occupancy.get_occupancies(data_8, True)
occupancies_in_window_6 = pd.Series(1, index=np.arange(len(occupancies_in_window_7)))

fig, axes = plt.subplots(1, 2, figsize=(8, 3), sharex=True, sharey=True)

# decrease space between subplots
fig.subplots_adjust(wspace=0.05)

axis1 = axes[0]
axis2 = axes[1]

axis1.set_title("a) Peak")
axis2.set_title("b) Off-peak")
axis1.set_xlabel("persons per vehicle")
axis2.set_xlabel("persons per vehicle")
configure_subplot(axis1)

_n, _bins, patches = axis1.hist([occupancies_in_window_1, occupancies_in_window_2, occupancies_in_window_3, occupancies_in_window_4,
	occupancies_in_window_5], bins, label=common.labels, color=common.colors)

axis1.set_ylabel("vehicle hours")

for patch_set, hatch in zip(patches, common.hatches):
	plt.setp(patch_set, hatch=hatch)

plt.legend(loc='upper right')



_n, _bins, patches = axis2.hist([occupancies_in_window_6, occupancies_in_window_7,
	occupancies_in_window_8, occupancies_in_window_9, occupancies_in_window_10], bins,
								label=common.labels, color=common.colors)

configure_subplot(axis2)

for patch_set, hatch in zip(patches, common.hatches):
	plt.setp(patch_set, hatch=hatch)

plt.legend(loc='upper right', labelspacing=0.01)

plt.savefig(config.images.occupancy_histogram_comparison_combined, bbox_inches='tight', transparent=True, pad_inches=0.0, dpi=fig.dpi)

plt.show()



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

import numpy as np
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import simod.statistics.model.service as service
import simod.statistics.comparisons.common as common

from matplotlib.ticker import FuncFormatter

FONT_SIZE = 18

matplotlib.rcParams['text.usetex'] = True
matplotlib.rcParams.update({'font.size': FONT_SIZE})


def to_percent(y, position):
	# Ignore the passed in position. This has the effect of scaling the default
	# tick locations.
	s = str(int(round(100 * y)))

	# The percent symbol needs escaping in latex
	if matplotlib.rcParams['text.usetex'] is True:
		return s + r'$\%$'
	else:
		return s + '%'


def configure_subplot(axis):
	# axis.yaxis.set_major_formatter(FuncFormatter(to_percent))
	axis.set_xlabel("delay [min]")
	axis.xaxis.set_ticks(np.arange(0, 6, 1))


service_1 = service.load_dataframe(config.comparison.experiment_1_dir)
service_2 = service.load_dataframe(config.comparison.experiment_2_dir)
service_3 = service.load_dataframe(config.comparison.experiment_3_dir)
service_4 = service.load_dataframe(config.comparison.experiment_4_dir)
service_5 = service.load_dataframe(config.comparison.experiment_5_dir)
service_6 = service.load_dataframe(config.comparison.experiment_6_dir)
service_7 = service.load_dataframe(config.comparison.experiment_7_dir)
service_8 = service.load_dataframe(config.comparison.experiment_8_dir)
service_9 = service.load_dataframe(config.comparison.experiment_9_dir)
service_10 = service.load_dataframe(config.comparison.experiment_10_dir)

delays_2 = service.get_delays(service_1)
delays_3 = service.get_delays(service_2)
delays_4 = service.get_delays(service_3)
delays_5 = service.get_delays(service_4)
delays_6 = service.get_delays(service_9)
delays_1 = pd.Series(0, index=np.arange(len(delays_2)))

delays_window_2 = service.get_delays(service_1, True)
delays_window_3 = service.get_delays(service_2, True)
delays_window_4 = service.get_delays(service_3, True)
delays_window_5 = service.get_delays(service_4, True)
delays_window_1 = pd.Series(0, index=np.arange(len(delays_window_2)))
delays_window_7 = service.get_delays(service_5, True)
delays_window_8 = service.get_delays(service_6, True)
delays_window_9 = service.get_delays(service_7, True)
delays_window_10 = service.get_delays(service_8, True)
delays_window_6 = pd.Series(0, index=np.arange(len(delays_window_7)))
delays_window_11 = service.get_delays(service_9, True)
delays_window_12 = service.get_delays(service_10, True)

bins = np.arange(-0.49, 5.51, 1)

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
# axes.hist([delays_1, delays_2, delays_3], bins, label=['No Ridesharing', 'Insertion Heuristic', 'VGA'], histtype='step',
# 		  density=True)
axes.hist([delays_1, delays_2, delays_3, delays_4, delays_5, delays_6], bins,
		label=common.labels, density=True, histtype='step')
axes.yaxis.set_major_formatter(FuncFormatter(to_percent))
plt.legend(loc='upper right')

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([delays_window_1, delays_window_2, delays_window_3, delays_window_4, delays_window_5, delays_window_6], bins,
		  label=common.labels, density=True, histtype='step')
axes.set_xlabel("delay [min]")
axes.yaxis.set_major_formatter(FuncFormatter(to_percent))

plt.legend(loc='upper right')

plt.savefig(config.images.delay_histogram_comparison, bbox_inches='tight', transparent=True)


# combined plots

delays_list = [delays_window_2, delays_window_3, delays_window_4, delays_window_5, delays_window_11, delays_window_7, delays_window_8,
			   delays_window_9, delays_window_10, delays_window_12]
# rounding
delays_list = [delays.round(1) for delays in delays_list]

fig, axes = plt.subplots(1, 2, figsize=(8, 3), sharex=True, sharey=True)

# decrease space between subplots
fig.subplots_adjust(wspace=0.05)

axis1 = axes[0]
axis2 = axes[1]



configure_subplot(axis1)
configure_subplot(axis2)

axis1.set_title("a) Peak")
axis1.set_xlabel("minutes")
axis1.set_ylabel("customers")
axis2.set_title("b) Off-peak")
axis2.set_xlabel("minutes")

_n, _bins, patches = axis1.hist(delays_list[0:5], bins,
		  label=common.labels[1:], color=common.colors[1:])
for patch_set, hatch in zip(patches, common.hatches[1:]):
	plt.setp(patch_set, hatch=hatch)

_n, _bins, patches = axis2.hist(delays_list[5:10], bins,
		  label=common.labels[1:], color=common.colors[1:])
for patch_set, hatch in zip(patches, common.hatches[1:]):
	plt.setp(patch_set, hatch=hatch)

plt.legend(loc='upper right', labelspacing=0.01)

plt.savefig(config.images.delay_histogram_comparison_combined, bbox_inches='tight', transparent=True, pad_inches=0.0, dpi=fig.dpi)

plt.show()



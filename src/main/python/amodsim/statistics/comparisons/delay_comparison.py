from amodsim.init import config

import numpy as np
import pandas as pd
import matplotlib
import matplotlib.pyplot as plt
import amodsim.statistics.model.service as service
import amodsim.statistics.comparisons.common as common

from matplotlib.ticker import FuncFormatter


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


service_1 = service.load_dataframe(config.comparison.experiment_1_dir)
service_2 = service.load_dataframe(config.comparison.experiment_2_dir)
service_3 = service.load_dataframe(config.comparison.experiment_3_dir)
service_4 = service.load_dataframe(config.comparison.experiment_4_dir)
service_5 = service.load_dataframe(config.comparison.experiment_5_dir)
service_6 = service.load_dataframe(config.comparison.experiment_6_dir)
service_7 = service.load_dataframe(config.comparison.experiment_7_dir)
service_8 = service.load_dataframe(config.comparison.experiment_8_dir)

delays_2 = service.get_delays(service_1)
delays_3 = service.get_delays(service_2)
delays_4 = service.get_delays(service_3)
delays_5 = service.get_delays(service_4)
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

bins = np.arange(-0.5, 20.5, 0.5)

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
# axes.hist([delays_1, delays_2, delays_3], bins, label=['No Ridesharing', 'Insertion Heuristic', 'VGA'], histtype='step',
# 		  normed=True)
axes.hist([delays_1, delays_2, delays_3, delays_4, delays_5], bins,
		label=common.labels, normed=True, histtype='step')
axes.yaxis.set_major_formatter(FuncFormatter(to_percent))
plt.legend(loc='upper right')

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([delays_window_1, delays_window_2, delays_window_3, delays_window_4, delays_window_5], bins,
		  label=common.labels, normed=True, histtype='step')
axes.set_xlabel("delay [min]")
axes.yaxis.set_major_formatter(FuncFormatter(to_percent))

plt.legend(loc='upper right')

plt.savefig(config.images.delay_histogram_comparison, bbox_inches='tight', transparent=True)


# combined plots
fig, axes = plt.subplots(1, 2, figsize=(8, 3), sharex=True, sharey=True)

# decrease space between subplots
fig.subplots_adjust(wspace=0.05)

axis1 = axes[0]
axis2 = axes[1]



configure_subplot(axis1)
configure_subplot(axis2)

axis1.set_xlabel("a) Peak")
axis2.set_xlabel("b) Off-peak")

_n, _bins, patches = axis1.hist([delays_window_5, delays_window_4, delays_window_3, delays_window_2], bins,
		  label=common.labels[:0:-1], color=common.colors[:0:-1], histtype='step')
# for patch_set, hatch in zip(patches, common.hatches[:0:-1]):
# 	plt.setp(patch_set, hatch=hatch)
_n, _bins, patches = axis2.hist([delays_window_10, delays_window_9, delays_window_8, delays_window_7], bins,
		  label=common.labels[:0:-1], color=common.colors[:0:-1], histtype='step')
# for patch_set, hatch in zip(patches, common.hatches[:0:-1]):
# 	plt.setp(patch_set, hatch=hatch)
plt.legend(loc='upper right')

plt.savefig(config.images.delay_histogram_comparison_combined, bbox_inches='tight', transparent=True, pad_inches=0.0, dpi=fig.dpi)

plt.show()


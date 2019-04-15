from amodsim.init import config

import numpy as np
import matplotlib
import matplotlib.pyplot as plt
import amodsim.statistics.model.service as service

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


service_1 = service.load_dataframe(config.comparison.experiment_1_dir)
service_2 = service.load_dataframe(config.comparison.experiment_2_dir)
service_3 = service.load_dataframe(config.comparison.experiment_3_dir)
service_4 = service.load_dataframe(config.comparison.experiment_4_dir)

delays_1 = service.get_delays(service_1)
delays_2 = service.get_delays(service_2)
delays_3 = service.get_delays(service_3)
delays_4 = service.get_delays(service_3)

delays_window_1 = service.get_delays(service_1, True)
delays_window_2 = service.get_delays(service_2, True)
delays_window_3 = service.get_delays(service_3, True)
delays_window_4 = service.get_delays(service_4, True)

bins = np.arange(-0.5, 20.5, 0.5)

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
# axes.hist([delays_1, delays_2, delays_3], bins, label=['No Ridesharing', 'Insertion Heuristic', 'VGA'], histtype='step',
# 		  normed=True)
axes.hist([delays_1, delays_2, delays_3, delays_4], bins,
          label=['No Ridesharing', 'Insertion Heuristic', 'VGA', 'VGA limited'], normed=True, histtype='step')
axes.yaxis.set_major_formatter(FuncFormatter(to_percent))
plt.legend(loc='upper right')

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([delays_window_1, delays_window_2, delays_window_3, delays_window_4], bins,
		  label=['No Ridesharing', 'Insertion Heuristic', 'VGA', 'VGA limited'], normed=True, histtype='step')
axes.set_xlabel("delay [min]")
axes.yaxis.set_major_formatter(FuncFormatter(to_percent))

plt.legend(loc='upper right')

plt.savefig(config.images.delay_histogram_comparison, bbox_inches='tight', transparent=True)

plt.show()



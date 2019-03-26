from amodsim.init import config

import numpy as np
import matplotlib.pyplot as plt
import roadmaptools.inout
import amodsim.statistics.model.service as service


# occupancies
service_1 = service.load_dataframe(config.comparison.experiment_1_dir)
service_2 = service.load_dataframe(config.comparison.experiment_2_dir)
service_3 = service.load_dataframe(config.comparison.experiment_3_dir)

delays_1 = service.get_delays(service_1)
delays_2 = service.get_delays(service_2)
delays_3 = service.get_delays(service_3)

delays_window_1 = service.get_delays(service_1, True)
delays_window_2 = service.get_delays(service_2, True)
delays_window_3 = service.get_delays(service_3, True)

bins = np.arange(-0.5, 20.5, 1)

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([delays_1, delays_2, delays_3], bins, label=['No Ridesharing', 'Insertion Heuristic', 'VGA'])

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([delays_window_1, delays_window_2, delays_window_3], bins, label=['No Ridesharing', 'Insertion Heuristic', 'VGA'])

plt.savefig(config.images.delay_histogram_comparison, bbox_inches='tight', transparent=True)

plt.legend(loc='upper right')

plt.show()



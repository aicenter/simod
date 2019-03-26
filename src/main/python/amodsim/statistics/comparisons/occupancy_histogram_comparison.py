from amodsim.init import config

import numpy as np
import matplotlib.pyplot as plt
import roadmaptools.inout
import amodsim.statistics.model.occupancy as occupancy


# occupancies
# occupancies_1 = roadmaptools.inout.load_csv(
# 	config.comparison.experiment_1_dir + config.statistics.occupancies_file_name, delimiter=',')
# occupancies_2 = roadmaptools.inout.load_csv(
# 	config.comparison.experiment_2_dir + config.statistics.occupancies_file_name, delimiter=',')
# occupancies_3 = roadmaptools.inout.load_csv(
# 	config.comparison.experiment_3_dir + config.statistics.occupancies_file_name, delimiter=',')

data_1 = occupancy.load(config.comparison.experiment_1_dir)
data_2 = occupancy.load(config.comparison.experiment_2_dir)
data_3 = occupancy.load(config.comparison.experiment_3_dir)

occupancies_1 = occupancy.get_occupancies(data_1)
occupancies_2 = occupancy.get_occupancies(data_2)
occupancies_3 = occupancy.get_occupancies(data_3)

occupancies_in_window_1 = occupancy.get_occupancies(data_1, True)
occupancies_in_window_2 = occupancy.get_occupancies(data_2, True)
occupancies_in_window_3 = occupancy.get_occupancies(data_3, True)

bins = np.arange(-0.5, 6.5, 1)

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([occupancies_1, occupancies_2, occupancies_3], bins, label=['No Ridesharing', 'Insertion Heuristic', 'VGA'])

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([occupancies_in_window_1, occupancies_in_window_2, occupancies_in_window_3], bins,
		  label=['No Ridesharing', 'Insertion Heuristic', 'VGA'])

plt.legend(loc='upper right')

plt.savefig(config.images.occupancy_histogram_comparison, bbox_inches='tight', transparent=True)

plt.show()



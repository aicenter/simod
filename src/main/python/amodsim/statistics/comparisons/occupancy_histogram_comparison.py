from amodsim.init import config

import numpy as np
import matplotlib.pyplot as plt
import datetime
import roadmaptools.inout
import amodsim.statistics.model.occupancy as occupancy

from matplotlib.ticker import FuncFormatter


def format_time(minutes: int, position) -> str:
	# return str(datetime.timedelta(minutes=minutes))
	return str(int(round(minutes / 60)))


data_1 = occupancy.load(config.comparison.experiment_1_dir)
data_2 = occupancy.load(config.comparison.experiment_2_dir)
data_3 = occupancy.load(config.comparison.experiment_3_dir)
data_4 = occupancy.load(config.comparison.experiment_4_dir)

occupancies_1 = occupancy.get_occupancies(data_1)
occupancies_2 = occupancy.get_occupancies(data_2)
occupancies_3 = occupancy.get_occupancies(data_3)
occupancies_4 = occupancy.get_occupancies(data_4)

occupancies_in_window_1 = occupancy.get_occupancies(data_1, True)
occupancies_in_window_2 = occupancy.get_occupancies(data_2, True)
occupancies_in_window_3 = occupancy.get_occupancies(data_3, True)
occupancies_in_window_4 = occupancy.get_occupancies(data_4, True)


bins = np.arange(-0.5, 6.5, 1)


fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([occupancies_1, occupancies_2, occupancies_3, occupancies_4], bins,
		  label=['No Ridesharing', 'Insertion Heuristic', 'VGA', 'VGA limited'])
axes.yaxis.set_ticks(np.arange(0, 1200001, 120000))
axes.yaxis.set_major_formatter(FuncFormatter(format_time))
axes.set_ylabel("vehicle hours")

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axes.hist([occupancies_in_window_1, occupancies_in_window_2, occupancies_in_window_3, occupancies_in_window_4], bins,
		  label=['No Ridesharing', 'Insertion Heuristic', 'VGA', 'VGA limited'])
axes.yaxis.set_ticks(np.arange(0, 840001, 120000))
axes.yaxis.set_major_formatter(FuncFormatter(format_time))
axes.set_ylabel("vehicle hours")

plt.legend(loc='upper right')

plt.savefig(config.images.occupancy_histogram_comparison, bbox_inches='tight', transparent=True)

plt.show()



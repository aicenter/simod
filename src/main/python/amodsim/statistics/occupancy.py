
from amodsim.init import config

from tqdm import tqdm
import numpy as np
import matplotlib.pyplot as plt
import matplotlib
import roadmaptools.inout
import amodsim.utils

from matplotlib.ticker import FuncFormatter
from pandas import DataFrame


def to_percent(y, position):
    # Ignore the passed in position. This has the effect of scaling the default
    # tick locations.
    s = str(int(round(100 * y)))

    # The percent symbol needs escaping in latex
    if matplotlib.rcParams['text.usetex'] is True:
        return s + r'$\%$'
    else:
        return s + '%'


results = roadmaptools.inout.load_json(config.amodsim.statistics.result_file_path)
data = np.genfromtxt(config.amodsim.statistics.occupancies_file_path, delimiter=',')

occupancy_col = data[:,2]

avg_occupancy = np.mean(occupancy_col)
avg_occupancy_demands = np.sum(occupancy_col) / np.count_nonzero(occupancy_col)


# globally
print("Average occupancy: {}".format(avg_occupancy))
print("Average occupancy - demands: {}".format(avg_occupancy_demands))

fig, axis = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
plt.gca().yaxis.set_major_formatter(FuncFormatter(to_percent))

bins = np.arange(-0.5, 6.5, 1)

axis.hist(occupancy_col, bins, normed=True)

# plt.savefig(config.images.occupancy_histogram, bbox_inches='tight', transparent=True)


# in window
rows = np.where(data[:,0] > config.analysis.chosen_window_start * 10)
data_in_window = data[rows]
occupancy_in_window = data_in_window[:,2]
avg_occupancy_demands_window = np.sum(occupancy_in_window) / np.count_nonzero(occupancy_in_window)

print("Average occupancy in window: {}".format(np.mean(occupancy_in_window)))
print("Average occupancy in window - demands: {}".format(avg_occupancy_demands_window))

fig, axis = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(2, 1.5))
plt.gca().yaxis.set_major_formatter(FuncFormatter(to_percent))

plt.setp(axis, xticks=range(0, 6, 1))

# axis.set_xlabel("Vehicle occupancy [persons]")
# axis.set_ylabel("Share of vehicles")

axis.hist(occupancy_in_window, bins, normed=True)

plt.savefig(config.images.occupancy_histogram_window, bbox_inches='tight', transparent=True)

# occupancy in time
df = DataFrame(data, columns=["period", "id", "occupancy"])
# avg_occupancies_per_window = df.join(df.drop('occupancy', 1), on="occupancy", rs)
# avg_occupancies_per_window = df.merge(df, on=['period'], how='inner').groupby(['period'], as_index=False).agg(np.average)
gb = df.groupby(['period'])
avg_occupancies_per_period = gb['occupancy'].agg(np.mean)

fig, axis = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axis.plot(avg_occupancies_per_period)

plt.show()
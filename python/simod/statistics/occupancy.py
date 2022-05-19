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

from tqdm import tqdm
import numpy as np
import matplotlib.pyplot as plt
import matplotlib
import roadmaptools.inout
import simod.utils

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


exp_dir_1 = '/Users/adela/Documents/bakalarka/vysledky2/InsertionHeuristic/Archiv/experiments/test/result.json'
exp_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/experiments/test/result.json'
exp_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/experiments/test/result.json'

# results = roadmaptools.inout.load_json(config.statistics.result_file_path)
# TODO change dir
results = roadmaptools.inout.load_json(exp_dir_3)

occ_dir_1 = '/Users/adela/Documents/bakalarka/vysledky2/InsertionHeuristic/Archiv/experiments/test/vehicle_occupancy.csv'
occ_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/experiments/test/vehicle_occupancy.csv'
occ_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/experiments/test/vehicle_occupancy.csv'
# TODO change dir
data = np.genfromtxt(occ_dir_3, delimiter=',')

save_dir_1 = '/Users/adela/Documents/bakalarka/vysledky2/InsertionHeuristic/Archiv/img/'
save_dir_2 = '/Users/adela/Documents/bakalarka/vysledky2/transferInsertionHeuristic/Archiv/img/'
save_dir_3 = '/Users/adela/Documents/bakalarka/vysledky2/taset/Archiv/img/'
# TODO change dir
save_results_to = save_dir_3

occupancy_col = data[:,2]

avg_occupancy = np.mean(occupancy_col)
avg_occupancy_demands = np.sum(occupancy_col) / np.count_nonzero(occupancy_col)


# globally
print("Average occupancy: {}".format(avg_occupancy))
print("Average occupancy - demands: {}".format(avg_occupancy_demands))

fig, axis = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
plt.gca().yaxis.set_major_formatter(FuncFormatter(to_percent))

bins = np.arange(-0.5, 6.5, 1)

# axis.hist(occupancy_col, bins, normed=True)
axis.hist(occupancy_col, bins, density=True, stacked=True)
# plt.title('Vehicle occupancy')

plt.savefig(save_results_to + 'occupancy', bbox_inches='tight', transparent=True)


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

# axis.hist(occupancy_in_window, bins, normed=True)
axis.hist(occupancy_in_window, bins, density=True, stacked=True)

# plt.savefig(config.images.occupancy_histogram_window, bbox_inches='tight', transparent=True)
plt.savefig(save_results_to + 'occupancy-window', bbox_inches='tight', transparent=True)


# occupancy in time
df = DataFrame(data, columns=["period", "id", "occupancy"])
# avg_occupancies_per_window = df.join(df.drop('occupancy', 1), on="occupancy", rs)
# avg_occupancies_per_window = df.merge(df, on=['period'], how='inner').groupby(['period'], as_index=False).agg(np.average)
gb = df.groupby(['period'])
avg_occupancies_per_period = gb['occupancy'].agg(np.mean)

fig, axis = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))
axis.plot(avg_occupancies_per_period)
# plt.title("Average vehicle occupancy over time")
plt.savefig(save_results_to + 'occupancy-in-time', bbox_inches='tight', transparent=True)

plt.show()

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

import csv
import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from matplotlib import ticker, gridspec
from typing import Tuple

from amodsim.statistics.comparisons.common import Experiment


def get_max_group_size_row(row: pd.Series, max_group_size: int = 11) -> int:
    next_level = False

    for size in range(1,max_group_size + 1):
        if row["{} Groups Count".format(size)] > 0:
            if size == max_group_size:
                next_level = True
            continue
        else:
            return int((size - 1) / 2)

    if next_level:
        for size in range(1, max_group_size):
            if row["{} Feasible Groups Count".format(size)] > 0:
                continue
            else:
                return int((max_group_size + size - 1) / 2)

    return max_group_size


def get_group_times_per_row(row: pd.Series, max_group_size: int = 11) -> pd.Series:
    next_level = False
    times = []
    for size in range(1,max_group_size + 1):
        if row["{} Groups Total Time".format(size)] > 0:
            if size == max_group_size:
                next_level = True
            times.append(row["{} Groups Total Time".format(size)])
        else:
            break

    if next_level:
        for size in range(1, max_group_size):
            if row["{} Feasible Groups Total Time".format(size)] > 0:
                times.append(row["{} Feasible Groups Total Time".format(size)])
            else:
                break

    max_group_size = int(len(times) / 2)

    return pd.Series(times[0:max_group_size + 1])


def get_group_counts_per_row(row: pd.Series, max_group_size: int = 11) -> pd.Series:
    next_level = False
    counts = []
    for size in range(1, max_group_size + 1):
        if row["{} Groups Count".format(size)] > 0:
            if size == max_group_size:
                next_level = True
            counts.append(row["{} Groups Count".format(size)])
        else:
            break

    if next_level:
        for size in range(1, max_group_size):
            if row["{} Feasible Groups Count".format(size)] > 0:
                counts.append(row["{} Feasible Groups Count".format(size)])
            else:
                break

    max_group_size = int(len(counts) / 2)

    return pd.Series(counts[0:max_group_size + 1])

ridesharing_filepath = r"C:/AIC Experiment Data/VGA/experiments/vga_gap_0002/ridesharing.csv"
ridesharing_filepath_lim = r"C:/AIC Experiment Data/VGA/experiments/vga-lim_gap_005_gg_60ms/ridesharing.csv"


# ridesharing_filepath = r"O:/AIC data/Shared/amod-data/VGA Evaluation/experiments/icreased_trip_multiplication_time_shift/vga-weight0/ridesharing.csv"
# ridesharing_filepath_lim = r"O:/AIC data/Shared/amod-data/VGA Evaluation/experiments/icreased_trip_multiplication_time_shift/vga-limited-weight0-lim30ms/ridesharing.csv"

# ridesharing_filepath = r"C:/AIC data/Shared/amod-data/VGA Evaluation/experiments/icreased_trip_multiplication_time_shift/vga-weight0/ridesharing.csv"
# ridesharing_filepath_lim = r"C:/AIC data/Shared/amod-data/VGA Evaluation/experiments/icreased_trip_multiplication_time_shift/vga-limited-weight0-lim30ms/ridesharing.csv"

with open(ridesharing_filepath, 'r', encoding="utf-8") as ridesharing:
    r = pd.read_csv(ridesharing)

with open(ridesharing_filepath_lim, 'r', encoding="utf-8") as ridesharing:
    r_lim = pd.read_csv(ridesharing)

# max_group_sizes = pd.Series()
# for row_index in range(len(r) - 1):
#     max_group_sizes.append(get_max_group_size_row(row_index))

max_group_sizes = r.apply(get_max_group_size_row, axis=1)
max_group_sizes_lim = r_lim.apply(get_max_group_size_row, args=(7,), axis=1)

# group_times = r.apply(max_group_sizes, axis=1)
group_times = []
group_counts = []
for _, row in r.iterrows():
    group_times.append(get_group_times_per_row(row))
    group_counts.append(get_group_counts_per_row(row))

group_times_df = pd.DataFrame(group_times)
group_counts_df = pd.DataFrame(group_counts)

group_times_sum = group_times_df.sum()
group_counts_sum = group_counts_df.sum()

group_times_lim = []
group_counts_lim = []
for _, row in r_lim.iterrows():
    group_times_lim.append(get_group_times_per_row(row, 7))
    group_counts_lim.append(get_group_counts_per_row(row, 7))
group_times_lim[0] = group_times_lim[0].append(pd.Series(np.zeros(9), index=range(2,11)))
group_counts_lim[0] = group_counts_lim[0].append(pd.Series(np.zeros(7), index=range(4,11)))

group_times_df_lim = pd.DataFrame(group_times_lim)
group_counts_df_lim = pd.DataFrame(group_counts_lim)

group_times_sum_lim = group_times_df_lim.sum()
group_counts_sum_lim = group_counts_df_lim.sum()

group_column_names = []
for i in range(12):
    group_column_names.append("{} Group Time".format(i))

# Plotting

optimal_color = Experiment.VGA.color
limited_color = Experiment.VGA_LIMITED.color

# FuncFormatter can be used as a decorator
@ticker.FuncFormatter
def minute_formater(x, pos):
    return "{}".format(int(x / 2))


# Simulation Time Plot
# gs = gridspec.GridSpec(3, 2, width_ratios=[3, 1])
fig, axes = plt.subplots(3,1, figsize=(5,6), gridspec_kw={'height_ratios': [1, 2, 5]}, sharex=True)
ax1 = axes[0]
ax2 = axes[1]
ax3 = axes[2]

# Axis 1 - Active Requests
ax1.plot(r["Active Request Count"] / 1000, color="red")
ax1.set_ylabel(r"Ac. req. $ \times 10^3 $")

# Axis 2 - Max Group Size
ax2.plot(max_group_sizes, label="VGA optimal", color=optimal_color)
ax2.plot(max_group_sizes_lim, label="VGA limited", color=limited_color)
ax2.set_ylabel("Max group size")
ax2.set_ylim(1.5, 12)
ax2.set_yticks(np.arange(2, 13, 2))
ax2.legend(loc=4, prop={'size': 8})

# Axis 3
ax3.plot(r["Group Generation Time"] / 1000, '-', label="VGA optimal group generation time", color=optimal_color)
ax3.plot(r["Solver Time"] / 1000, '--', label="VGA optimal ILP solver time", color="aqua")
ax3.plot(r_lim["Group Generation Time"] / 1000, '-.', label="VGA limited group generation time", color=limited_color)
ax3.plot(r_lim["Solver Time"] / 1000, ':', label="VGA limited ILP solver time", color="orangered")
ax3.set_ylabel("Comp. time [s]")
ax3.set_ylim(0.5, 16000)
ax3.set_yscale("log")
ax3.axhline(y=30, linewidth=0.5, color='black', linestyle='-', label='VGA limited ILP solver limit')
ax3.legend(loc=1, prop={'size': 8})

ax3.set_xlabel("Simulation time [min]")
ax3.set_xlim(0, 180)
ax3.xaxis.set_major_locator(ticker.MultipleLocator(20))
ax3.xaxis.set_major_formatter(minute_formater)

plt.savefig(config.images.vga_times, bbox_inches='tight', transparent=True)


# Group Size Plot
fig2, axes2 = plt.subplots(2,1, figsize=(5,5), sharex=True)
ax1 = axes2[0]
ax2 = axes2[1]

# Axis 1 - Computational Time
width = 0.4
x = np.arange(1,12)
ax1.bar(x - width / 2, group_times_sum / 1000, width, label="VGA optimal", color=optimal_color, hatch=Experiment.VGA.pattern)
ax1.bar(x + width / 2, group_times_sum_lim / 1000, width, label="VGA limited", color=limited_color, hatch=Experiment.VGA_LIMITED.pattern)
ax1.set_ylabel("Comp. time [s]")
ax1.set_yscale("log")
# ax1.legend(loc=1, prop={'size': 8})

# Axis 2 - Group Counts
ax2.bar(x - width / 2, group_counts_sum / 1, width, label="VGA optimal", color=optimal_color, hatch=Experiment.VGA.pattern)
ax2.bar(x + width / 2, group_counts_sum_lim / 1, width, label="VGA limited", color=limited_color, hatch=Experiment.VGA_LIMITED.pattern)
ax2.set_ylabel("Number of groups")
ax2.set_yscale("log")
ax2.legend(loc=1, prop={'size': 8})

# x Axis config
ax2.set_xlim(0, 12)
ax2.set_xlabel("Group size")
plt.xticks(np.arange(min(x), max(x)+1, 1.0))

plt.savefig(config.images.vga_group_size, bbox_inches='tight', transparent=True)

plt.show()

# # OLD
# # Times
# fig, axes = plt.subplots(2,1, figsize=(5,6))
# ax1 = axes[0]
# ax2 = axes[1]
#
# # Axis 2
# ax1.plot(r["Group Generation Time"] / 1000)
# ax1.plot(r["Solver Time"] / 1000)
# ax1.set_ylim(0, 500)
# ax1.set_xlim(0, 180)
# ax1.set_xlabel("simulation time [min]")
# ax1.set_ylabel("computational time [s]")
#
# # second plot
# ax1r = ax1.twinx()
# ax1r.plot(r["Active Request Count"] / 1000, color='r')
# ax1r.set_ylabel("active requests [thousands]", color='r')
# ax1r.tick_params('y', colors='r')
# ax1r.set_ylim(0, 30)
#
# ax1.legend(loc=1, prop={'size': 10})
#
# ax1.xaxis.set_major_locator(ticker.MultipleLocator(20))
# ax1.xaxis.set_major_formatter(minute_formater)
#
#
# # Bars
# width = 0.4
# x = np.arange(1,12)
# ax2.bar(x - width/2, group_times_sum / 1000 / 60, width)
#
# ax2.set_xlabel("group size")
# ax2.set_ylabel("computational time [min]")
#
#
#
# # second plot
# ax2r = ax2.twinx()
# ax2r.bar(x + width/2, group_counts_sum / 1000_000_000, width, color="red")
# ax2r.set_ylabel("group counts [billions]", color='r')
# ax2r.tick_params('y', colors='r')
# # ax2r.set_ylim(0, 25)







# # Lines
# fig, axes = plt.subplots(2,1, figsize=(5,6))


# # Axis 1
# ax1 = axes[0]
# ax2 = axes[1]
# ax1.plot(r["Active Request Count"] / 1000)
# ax1.set_xlabel("simulation time [min]")
# ax1.set_ylabel("active requests [thousands]")
#
# # second plot
# ax1r = ax1.twinx()         # creates second Axes object with invisible x-Axis but independent y-Axis
# ax1r.plot(max_group_sizes, color='r')
# ax1r.set_ylabel("max group size", color='r')
# ax1r.tick_params('y', colors='r')
# ax1r.set_ylim(0, 12)


# # Axis 2
# ax2.plot(r["Group Generation Time"] / 1000)
# ax2.plot(r["Solver Time"] / 1000)
# ax2.set_ylim(0, 500)
# ax2.set_xlim(0, 180)
# ax2.set_xlabel("simulation time [min]")
# ax2.set_ylabel("computational time [s]")
#
# # second plot
# ax2r = ax2.twinx()
# ax2r.plot(max_group_sizes, color='r')
# ax2r.set_ylabel("max group size", color='r')
# ax2r.tick_params('y', colors='r')
# ax2r.set_ylim(0, 15)
#
# ax2.legend(loc=1, prop={'size': 10})
#
#
# # FuncFormatter can be used as a decorator
# @ticker.FuncFormatter
# def minute_formater(x, pos):
#     return "{}".format(int(x / 2))
#
#
# ax1.xaxis.set_major_locator(ticker.MultipleLocator(20))
# ax1.xaxis.set_major_formatter(minute_formater)
#
# ax2.xaxis.set_major_locator(ticker.MultipleLocator(20))
# ax2.xaxis.set_major_formatter(minute_formater)
#
# plt.savefig(r"C:\Users\Fido\Downloads/vga_times.png", bbox_inches='tight', transparent=True)
#
# plt.show()





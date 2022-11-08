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
import matplotlib
import matplotlib.pyplot as plt
import pandas as pd
import datetime
import roadmaptools.inout
import simod.statistics.model.occupancy as occupancy
import simod.statistics.comparisons.common as common

from matplotlib.ticker import FuncFormatter

FONT_SIZE = 16

matplotlib.rcParams['text.usetex'] = False
matplotlib.rcParams.update({'font.size': FONT_SIZE})


def format_time(minutes: int, position) -> str:
	# return str(datetime.timedelta(minutes=minutes))
	return str(int(round(minutes / 60)))


def configure_subplot(axis):
	axis.yaxis.set_ticks(np.arange(0, 1020001, 30000))
	axis.yaxis.set_major_formatter(FuncFormatter(format_time))
	axis.xaxis.set_ticks(np.arange(0, 10, 1))

# data_1_num_people = occupancy.load(config.comparison.experiment_1_dir)
# data_1_no_people = occupancy.load_no_people(config.comparison.experiment_1_dir)
# data_1_with_people = occupancy.load_people_onboard(config.comparison.experiment_1_dir)

# data_3_people = occupancy.load(config.comparison.experiment_3_dir)
# data_3_packages = occupancy.load_packages(config.comparison.experiment_3_dir)
#
# data_4_people = occupancy.load(config.comparison.experiment_4_dir)
# data_4_packages = occupancy.load_packages(config.comparison.experiment_4_dir)
#
# data_5_people = occupancy.load(config.comparison.experiment_5_dir)
# data_5_packages = occupancy.load_packages(config.comparison.experiment_5_dir)
#
# data_6_people = occupancy.load(config.comparison.experiment_6_dir)
# data_6_packages = occupancy.load_packages(config.comparison.experiment_6_dir)
#
# data_9_people = occupancy.load(config.comparison.experiment_9_dir)
# data_9_packages = occupancy.load_packages(config.comparison.experiment_9_dir)
#
# data_10_people = occupancy.load(config.comparison.experiment_10_dir)
# data_10_packages = occupancy.load_packages(config.comparison.experiment_10_dir)
# data_10_combined = occupancy.load_combined_occupancy(config.comparison.experiment_10_dir)

# 25k 1h
data_8_people = occupancy.load(config.comparison.experiment_8_dir)
data_8_packages = occupancy.load_packages(config.comparison.experiment_8_dir)
data_8_combined = occupancy.load_combined_occupancy(config.comparison.experiment_8_dir)

data_9_people = occupancy.load(config.comparison.experiment_9_dir)
data_9_packages = occupancy.load_packages(config.comparison.experiment_9_dir)
data_9_combined = occupancy.load_combined_occupancy(config.comparison.experiment_9_dir)

data_10_people = occupancy.load(config.comparison.experiment_10_dir)
data_10_packages = occupancy.load_packages(config.comparison.experiment_10_dir)
data_10_combined = occupancy.load_combined_occupancy(config.comparison.experiment_10_dir)

# 50k 1h
data_11_people = occupancy.load(config.comparison.experiment_11_dir)
data_11_packages = occupancy.load_packages(config.comparison.experiment_11_dir)
data_11_combined = occupancy.load_combined_occupancy(config.comparison.experiment_11_dir)

data_12_people = occupancy.load(config.comparison.experiment_12_dir)
data_12_packages = occupancy.load_packages(config.comparison.experiment_12_dir)
data_12_combined = occupancy.load_combined_occupancy(config.comparison.experiment_12_dir)

data_13_people = occupancy.load(config.comparison.experiment_13_dir)
data_13_packages = occupancy.load_packages(config.comparison.experiment_13_dir)
data_13_combined = occupancy.load_combined_occupancy(config.comparison.experiment_13_dir)

# 24k 24h
data_14_people = occupancy.load(config.comparison.experiment_14_dir)
data_14_packages = occupancy.load_packages(config.comparison.experiment_14_dir)
data_14_combined = occupancy.load_combined_occupancy(config.comparison.experiment_14_dir)

data_15_people = occupancy.load(config.comparison.experiment_15_dir)
data_15_packages = occupancy.load_packages(config.comparison.experiment_15_dir)
data_15_combined = occupancy.load_combined_occupancy(config.comparison.experiment_15_dir)

data_16_people = occupancy.load(config.comparison.experiment_16_dir)
data_16_packages = occupancy.load_packages(config.comparison.experiment_16_dir)
data_16_combined = occupancy.load_combined_occupancy(config.comparison.experiment_16_dir)


# occupancies_1_num_people = occupancy.get_occupancies(data_1_num_people)
# occupancies_1_no_people = occupancy.get_occupancies(data_1_no_people)
# occupancies_1_with_people = occupancy.get_occupancies(data_1_with_people)


# occupancies_3_people = occupancy.get_occupancies(data_3_people)
# occupancies_3_packages = occupancy.get_occupancies(data_3_packages)
#
# occupancies_4_people = occupancy.get_occupancies(data_4_people)
# occupancies_4_packages = occupancy.get_occupancies(data_4_packages)
#
# occupancies_5_people = occupancy.get_occupancies(data_5_people)
# occupancies_5_packages = occupancy.get_occupancies(data_5_packages)
#
# occupancies_6_people = occupancy.get_occupancies(data_6_people)
# occupancies_6_packages = occupancy.get_occupancies(data_6_packages)
#
#
# occupancies_9_people = occupancy.get_occupancies(data_9_people)
# occupancies_9_packages = occupancy.get_occupancies(data_9_packages)

occupancies_8_people = occupancy.get_occupancies(data_8_people)
occupancies_8_packages = occupancy.get_occupancies(data_8_packages)
occupancies_8_combined = occupancy.get_combined_occupancies(data_8_combined)

occupancies_9_people = occupancy.get_occupancies(data_9_people)
occupancies_9_packages = occupancy.get_occupancies(data_9_packages)
occupancies_9_combined = occupancy.get_combined_occupancies(data_9_combined)

occupancies_10_people = occupancy.get_occupancies(data_10_people)
occupancies_10_packages = occupancy.get_occupancies(data_10_packages)
occupancies_10_combined = occupancy.get_combined_occupancies(data_10_combined)

occupancies_11_people = occupancy.get_occupancies(data_11_people)
occupancies_11_packages = occupancy.get_occupancies(data_11_packages)
occupancies_11_combined = occupancy.get_combined_occupancies(data_11_combined)

occupancies_12_people = occupancy.get_occupancies(data_12_people)
occupancies_12_packages = occupancy.get_occupancies(data_12_packages)
occupancies_12_combined = occupancy.get_combined_occupancies(data_12_combined)

occupancies_13_people = occupancy.get_occupancies(data_13_people)
occupancies_13_packages = occupancy.get_occupancies(data_13_packages)
occupancies_13_combined = occupancy.get_combined_occupancies(data_13_combined)

occupancies_14_people = occupancy.get_occupancies(data_14_people)
occupancies_14_packages = occupancy.get_occupancies(data_14_packages)
occupancies_14_combined = occupancy.get_combined_occupancies(data_14_combined)

occupancies_15_people = occupancy.get_occupancies(data_15_people)
occupancies_15_packages = occupancy.get_occupancies(data_15_packages)
occupancies_15_combined = occupancy.get_combined_occupancies(data_15_combined)

occupancies_16_people = occupancy.get_occupancies(data_16_people)
occupancies_16_packages = occupancy.get_occupancies(data_16_packages)
occupancies_16_combined = occupancy.get_combined_occupancies(data_16_combined)



bins = np.arange(-0.5, 10.5, 1)

hatches = ['\\\\', '//', '++', '**']

""" ---------------------- FIG  ------------------------- """
# fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(10, 5))
# _n, _bins, patches = axes.hist([occupancies_1_num_people], bins, rwidth=0.9)
# # axes.yaxis.set_ticks(np.arange(0, 1200001, 120000))
# axes.yaxis.set_major_formatter(FuncFormatter(format_time))
# axes.set_ylabel("vehicle hours")
# axes.set_xlabel("passengers per vehicle")
# # plt.legend(loc='upper right')

""" ---------------------- FIG  ---------------------- """
# fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(10, 5))
# _n, _bins, patches = axes.hist([occupancies_1_with_person], bins, label=['1 person onboard'], rwidth=0.9)
# # axes.yaxis.set_ticks(np.arange(0, 1200001, 120000))
# axes.yaxis.set_major_formatter(FuncFormatter(format_time))
# axes.set_ylabel("vehicle hours")
# axes.set_xlabel("packages per vehicle")
# plt.legend(loc='upper right')



# for patch_set, hatch in zip(patches, hatches):
# 	plt.setp(patch_set, hatch=hatch)

"""
fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(10, 3))
_n, _bins, patches = axes.hist([occupancies_in_window_1], bins,
							   label=common.labels)
# , occupancies_in_window_3,
# 								occupancies_in_window_4, occupancies_in_window_4, occupancies_in_window_10
axes.yaxis.set_ticks(np.arange(0, 840001, 120000))
axes.yaxis.set_major_formatter(FuncFormatter(format_time))
axes.set_ylabel("vehicle hours")

for patch_set, hatch in zip(patches, hatches):
	plt.setp(patch_set, hatch=hatch)
"""


# plt.savefig(config.images.occupancy_histogram_comparison, bbox_inches='tight', transparent=True)


""" --------------------------------- FIG 1 ------------------------------- """
# fig, axes = plt.subplots(1, 2, figsize=(10, 5), sharex=True, sharey=True)
#
# # decrease space between subplots
# fig.subplots_adjust(wspace=0.05)
#
# axis1 = axes[0]
# axis2 = axes[1]
# # axis1.yaxis.set_ticks(np.arange(0, 1200001, 120000))
#
# axis1.set_title("Base version 10h")
# axis1.set_xlabel("Passengers per vehicle")
#
# # axis2.set_title("Packages occupancy")
# axis2.set_xlabel("Packages per vehicle")
#
# configure_subplot(axis1)
#
# _n, _bins, patches = axis1.hist([occupancies_3_people], bins, rwidth=0.9)
# _n, _bins, patches = axis2.hist([occupancies_3_packages], bins, rwidth=0.9)
#
# axis1.set_ylabel("vehicle hours")
""" -----------------------------------------------------------------------------"""


""" ------------------------------------ FIG 2 --------------------------------- """
# fig, axes = plt.subplots(1, 2, figsize=(10, 5), sharex=True, sharey=True)
#
# # decrease space between subplots
# fig.subplots_adjust(wspace=0.05)
#
# axis1 = axes[0]
# axis2 = axes[1]
#
# axis1.set_title("Insertion version 10h")
# axis1.set_xlabel("Passengers per vehicle")
#
# # axis2.set_title("Packages occupancy")
# axis2.set_xlabel("Packages per vehicle")
# configure_subplot(axis1)
#
# _n, _bins, patches = axis1.hist([occupancies_4_people], bins, rwidth=0.9)
# _n, _bins, patches = axis2.hist([occupancies_4_packages], bins, rwidth=0.9)
#
# axis1.set_ylabel("vehicle hours")
""" -----------------------------------------------------------------------------"""


""" --------------------------------- FIG 3 ------------------------------- """
# fig, axes = plt.subplots(1, 2, figsize=(10, 5), sharex=True, sharey=True)
#
# # decrease space between subplots
# fig.subplots_adjust(wspace=0.05)
#
# axis1 = axes[0]
# axis2 = axes[1]
#
# axis1.set_title("Base version 2h")
# axis1.set_xlabel("Passengers per vehicle")
#
# # axis2.set_title("Packages occupancy")
# axis2.set_xlabel("Packages per vehicle")
# configure_subplot(axis1)
#
# _n, _bins, patches = axis1.hist([occupancies_5_people], bins,  rwidth=0.9)
# _n, _bins, patches = axis2.hist([occupancies_5_packages], bins, rwidth=0.9)
#
# axis1.set_ylabel("vehicle hours")
""" -----------------------------------------------------------------------------"""


""" ------------------------------------ FIG 4 --------------------------------- """
# fig, axes = plt.subplots(1, 2, figsize=(10, 5), sharex=True, sharey=True)
#
# # decrease space between subplots
# fig.subplots_adjust(wspace=0.05)
#
# axis1 = axes[0]
# axis2 = axes[1]
#
# axis1.set_title("Insertion version 2h")
# axis1.set_xlabel("Passengers per vehicle")
#
# # axis2.set_title("Packages occupancy")
# axis2.set_xlabel("Packages per vehicle")
# configure_subplot(axis1)
#
# _n, _bins, patches = axis1.hist([occupancies_6_people], bins,  rwidth=0.9)
# _n, _bins, patches = axis2.hist([occupancies_6_packages], bins, rwidth=0.9)
#
# axis1.set_ylabel("vehicle hours")
""" -----------------------------------------------------------------------------"""


""" --------------------------------- FIG 5 ------------------------------- """
# fig, axes = plt.subplots(1, 2, figsize=(10, 5), sharex=True, sharey=True)
#
# # decrease space between subplots
# fig.subplots_adjust(wspace=0.05)
#
# axis1 = axes[0]
# axis2 = axes[1]
#
# axis1.set_title("Base version Manhattan 1h")
# axis1.set_xlabel("Passengers per vehicle")
#
# # axis2.set_title("Packages occupancy")
# axis2.set_xlabel("Packages per vehicle")
# configure_subplot(axis1)
#
# _n, _bins, patches = axis1.hist([occupancies_9_people], bins,  rwidth=0.9)
# _n, _bins, patches = axis2.hist([occupancies_9_packages], bins, rwidth=0.9)
#
# axis1.set_ylabel("vehicle hours")
""" -----------------------------------------------------------------------------"""


""" --------------------------------- FIG 6 ------------------------------- """
# fig, axes = plt.subplots(1, 2, figsize=(10, 5), sharex=True, sharey=True)
#
# # decrease space between subplots
# fig.subplots_adjust(wspace=0.05)
#
# axis1 = axes[0]
# axis2 = axes[1]
#
# axis1.set_title("Insertion version Manhattan 1h")
# axis1.set_xlabel("Passengers per vehicle")
#
# # axis2.set_title("Packages occupancy")
# axis2.set_xlabel("Packages per vehicle")
# configure_subplot(axis1)
#
# _n, _bins, patches = axis1.hist([occupancies_10_people], bins,  rwidth=0.9)
# _n, _bins, patches = axis2.hist([occupancies_10_packages], bins, rwidth=0.9)
#
# axis1.set_ylabel("vehicle hours")
""" -----------------------------------------------------------------------------"""


""" --------------------------------- FIG 13 ------------------------------- """
# fig, axes = plt.subplots(1, 2, figsize=(10, 5), sharex=True, sharey=True)
#
# # decrease space between subplots
# fig.subplots_adjust(wspace=0.05)
#
# axis1 = axes[0]
# axis2 = axes[1]
#
# axis1.set_title("Base version Manhattan 1h 50k")
# axis1.set_xlabel("Passengers per vehicle")
#
# # axis2.set_title("Packages occupancy")
# axis2.set_xlabel("Packages per vehicle")
# configure_subplot(axis1)
#
# _n, _bins, patches = axis1.hist([occupancies_8_people], bins, rwidth=0.9)
# _n, _bins, patches = axis2.hist([occupancies_8_packages], bins, rwidth=0.9)
#
# axis1.set_ylabel("vehicle hours")
""" -----------------------------------------------------------------------------"""

""" --------------------------------- FIG 14 ------------------------------- """
# fig, axes = plt.subplots(1, 2, figsize=(10, 5), sharex=True, sharey=True)
#
# # decrease space between subplots
# fig.subplots_adjust(wspace=0.05)
#
# axis1 = axes[0]
# axis2 = axes[1]
#
# axis1.set_title("Insertion version Manhattan 1h 50k")
# axis1.set_xlabel("Passengers per vehicle")
#
# # axis2.set_title("Packages occupancy")
# axis2.set_xlabel("Packages per vehicle")
# configure_subplot(axis1)
#
# _n, _bins, patches = axis1.hist([occupancies_10_people], bins, rwidth=0.9)
# _n, _bins, patches = axis2.hist([occupancies_10_packages], bins, rwidth=0.9)
#
# axis1.set_ylabel("vehicle hours")
""" -----------------------------------------------------------------------------"""





# for patch_set, hatch in zip(patches, common.hatches):
# 	plt.setp(patch_set, hatch=hatch)

# plt.legend(loc='upper right')

"""
# _n, _bins, patches = axis2.hist([occupancies_in_window_6, occupancies_in_window_7,
# 	occupancies_in_window_8, occupancies_in_window_9, occupancies_in_window_10, occupancies_in_window_11], bins,
# 								label=common.labels, color=common.colors)

configure_subplot(axis2)

for patch_set, hatch in zip(patches, common.hatches):
	plt.setp(patch_set, hatch=hatch)

plt.legend(loc='upper right', labelspacing=0.01)

"""

# plt.savefig(config.images.occupancy_histogram_comparison_combined, bbox_inches='tight', transparent=True, pad_inches=0.0, dpi=fig.dpi)


""" --------------------- 3D Histogram experiment 8 = 1h 25k BASE -------------------- """
# x = data_8_combined["people_occupancy"]
# y = data_8_combined["package_occupancy"]
#
# fig = plt.figure()
# ax = fig.add_subplot(projection='3d')
# ax.set_xlabel("Passengers per vehicle")
# ax.set_ylabel("Packages per vehicle")
# ax.set_zlabel("Vehicle hours", labelpad=10.0)
# ax.zaxis.set_ticks(np.arange(0, 1020001, 30000))
# ax.zaxis.set_major_formatter(FuncFormatter(format_time))
#
# hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])
#
# # Construct arrays for the anchor positions of the 16 bars.
# xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
# xpos = xpos.ravel()
# ypos = ypos.ravel()
# zpos = np.zeros_like(xpos)
#
# # Construct arrays with the dimensions for the 16 bars.
# dx = dy = 0.9 * np.ones_like(zpos)
# dz = hist.ravel()
#
# ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""

""" --------------------------3D Histogram experiment 9 = 1h 25k MULTIPASS -------------------- """
# x = data_9_combined["people_occupancy"]
# y = data_9_combined["package_occupancy"]
#
# fig = plt.figure()
# ax = fig.add_subplot(projection='3d')
# ax.set_xlabel("Passengers per vehicle")
# ax.set_ylabel("Packages per vehicle")
# ax.set_zlabel("Vehicle hours", labelpad=10.0)
# ax.zaxis.set_ticks(np.arange(0, 1020001, 30000))
# ax.zaxis.set_major_formatter(FuncFormatter(format_time))
#
# hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])
#
# # Construct arrays for the anchor positions of the 16 bars.
# xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
# xpos = xpos.ravel()
# ypos = ypos.ravel()
# zpos = np.zeros_like(xpos)
#
# # Construct arrays with the dimensions for the 16 bars.
# dx = dy = 0.9 * np.ones_like(zpos)
# dz = hist.ravel()
#
# ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""

""" --------------------------------- 3D Histogram experiment 10 1h 25k INSERTION ------------ """
# x = data_10_combined["people_occupancy"]
# y = data_10_combined["package_occupancy"]
#
# fig = plt.figure()
# ax = fig.add_subplot(projection='3d')
# ax.set_xlabel("Passengers per vehicle")
# ax.set_ylabel("Packages per vehicle")
# ax.set_zlabel("Vehicle hours", labelpad=10.0)
# ax.zaxis.set_ticks(np.arange(0, 1020001, 30000))
# ax.zaxis.set_major_formatter(FuncFormatter(format_time))
#
# hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])
# # print(hist)
#
# # Construct arrays for the anchor positions of the 16 bars.
# xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
# # print(xpos)
# # print(ypos)
# xpos = xpos.ravel()
# ypos = ypos.ravel()
# zpos = np.zeros_like(xpos)
#
# # Construct arrays with the dimensions for the 16 bars.
# dx = dy = 0.9 * np.ones_like(zpos)
# dz = hist.ravel()
#
# ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""

""" --------------------- 3D Histogram experiment 11 = 1h 50k BASE -------------------- """
# x = data_11_combined["people_occupancy"]
# y = data_11_combined["package_occupancy"]
#
# fig = plt.figure()
# ax = fig.add_subplot(projection='3d')
# ax.set_xlabel("Passengers per vehicle")
# ax.set_ylabel("Packages per vehicle")
# ax.set_zlabel("Vehicle hours", labelpad=10.0)
# ax.zaxis.set_ticks(np.arange(0, 1020001, 60000))
# ax.zaxis.set_major_formatter(FuncFormatter(format_time))
#
# hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])
#
# # Construct arrays for the anchor positions of the 16 bars.
# xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
# xpos = xpos.ravel()
# ypos = ypos.ravel()
# zpos = np.zeros_like(xpos)
#
# # Construct arrays with the dimensions for the 16 bars.
# dx = dy = 0.9 * np.ones_like(zpos)
# dz = hist.ravel()
#
# ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""

""" --------------------------3D Histogram experiment 12 = 1h 50k MULTIPASS -------------------- """
# x = data_12_combined["people_occupancy"]
# y = data_12_combined["package_occupancy"]
#
# fig = plt.figure()
# ax = fig.add_subplot(projection='3d')
# ax.set_xlabel("Passengers per vehicle")
# ax.set_ylabel("Packages per vehicle")
# ax.set_zlabel("Vehicle hours", labelpad=10.0)
# ax.zaxis.set_ticks(np.arange(0, 1020001, 30000))
# ax.zaxis.set_major_formatter(FuncFormatter(format_time))
#
# hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])
#
# # Construct arrays for the anchor positions of the 16 bars.
# xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
# xpos = xpos.ravel()
# ypos = ypos.ravel()
# zpos = np.zeros_like(xpos)
#
# # Construct arrays with the dimensions for the 16 bars.
# dx = dy = 0.9 * np.ones_like(zpos)
# dz = hist.ravel()
#
# ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""

""" --------------------------------- 3D Histogram experiment 13 1h 50k INSERTION ------------ """
# x = data_13_combined["people_occupancy"]
# y = data_13_combined["package_occupancy"]
#
# fig = plt.figure()
# ax = fig.add_subplot(projection='3d')
# ax.set_xlabel("Passengers per vehicle")
# ax.set_ylabel("Packages per vehicle")
# ax.set_zlabel("Vehicle hours", labelpad=10.0)
# ax.zaxis.set_ticks(np.arange(0, 1020001, 30000))
# ax.zaxis.set_major_formatter(FuncFormatter(format_time))
#
# hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])
# # print(hist)
#
# # Construct arrays for the anchor positions of the 16 bars.
# xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
# # print(xpos)
# # print(ypos)
# xpos = xpos.ravel()
# ypos = ypos.ravel()
# zpos = np.zeros_like(xpos)
#
# # Construct arrays with the dimensions for the 16 bars.
# dx = dy = 0.9 * np.ones_like(zpos)
# dz = hist.ravel()
#
# ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""

""" --------------------- 3D Histogram experiment 14 = 24h 24k BASE -------------------- """
x = data_14_combined["people_occupancy"]
y = data_14_combined["package_occupancy"]

fig = plt.figure()
ax = fig.add_subplot(projection='3d')
ax.set_xlabel("Passengers per vehicle")
ax.set_ylabel("Packages per vehicle")
ax.set_zlabel("Vehicle hours", labelpad=10.0)
ax.zaxis.set_ticks(np.arange(0, 1020001, 60000))
ax.zaxis.set_major_formatter(FuncFormatter(format_time))

hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])

# Construct arrays for the anchor positions of the 16 bars.
xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
xpos = xpos.ravel()
ypos = ypos.ravel()
zpos = np.zeros_like(xpos)

# Construct arrays with the dimensions for the 16 bars.
dx = dy = 0.9 * np.ones_like(zpos)
dz = hist.ravel()

ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""

""" --------------------------3D Histogram experiment 15 = 24h 24k MULTIPASS -------------------- """
x = data_15_combined["people_occupancy"]
y = data_15_combined["package_occupancy"]

fig = plt.figure()
ax = fig.add_subplot(projection='3d')
ax.set_xlabel("Passengers per vehicle")
ax.set_ylabel("Packages per vehicle")
ax.set_zlabel("Vehicle hours", labelpad=10.0)
ax.zaxis.set_ticks(np.arange(0, 1020001, 30000))
ax.zaxis.set_major_formatter(FuncFormatter(format_time))

hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])

# Construct arrays for the anchor positions of the 16 bars.
xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
xpos = xpos.ravel()
ypos = ypos.ravel()
zpos = np.zeros_like(xpos)

# Construct arrays with the dimensions for the 16 bars.
dx = dy = 0.9 * np.ones_like(zpos)
dz = hist.ravel()

ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""

""" --------------------------------- 3D Histogram experiment 16 24h 24k INSERTION ------------ """
x = data_16_combined["people_occupancy"]
y = data_16_combined["package_occupancy"]

fig = plt.figure()
ax = fig.add_subplot(projection='3d')
ax.set_xlabel("Passengers per vehicle")
ax.set_ylabel("Packages per vehicle")
ax.set_zlabel("Vehicle hours", labelpad=10.0)
ax.zaxis.set_ticks(np.arange(0, 1020001, 30000))
ax.zaxis.set_major_formatter(FuncFormatter(format_time))

hist, xedges, yedges = np.histogram2d(x, y, bins=4, range=[[0, 4], [0, 4]])
# print(hist)

# Construct arrays for the anchor positions of the 16 bars.
xpos, ypos = np.meshgrid(xedges[:-1] - 0.5, yedges[:-1] - 0.5, indexing="ij")
# print(xpos)
# print(ypos)
xpos = xpos.ravel()
ypos = ypos.ravel()
zpos = np.zeros_like(xpos)

# Construct arrays with the dimensions for the 16 bars.
dx = dy = 0.9 * np.ones_like(zpos)
dz = hist.ravel()

ax.bar3d(xpos, ypos, zpos, dx, dy, dz, zsort='average')
""" -----------------------------------------------------------------------------"""


plt.show()
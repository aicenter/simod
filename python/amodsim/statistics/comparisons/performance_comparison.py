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
from init import config

import matplotlib.pyplot as plt
import amodsim.statistics.model.ridesharing as ridesharing


ih_capacity1_stats = ridesharing.load(config.comparison.experiment_1_dir)
ih_capacity5_stats = ridesharing.load(config.comparison.experiment_2_dir)
vga_stats = ridesharing.load(config.comparison.experiment_3_dir)

fig, axes = plt.subplots(1, 1, subplot_kw={"adjustable": 'box'}, figsize=(4, 3))

axes.plot(ih_capacity1_stats['Batch'], ih_capacity1_stats['Insertion Heuristic Time'] / 1000)
axes.plot(ih_capacity5_stats['Batch'], ih_capacity5_stats['Insertion Heuristic Time'] / 1000)
axes.plot(vga_stats['Batch'], (vga_stats['Group Generation Time'] + vga_stats['Solver Time']) / 1000)

axes.legend(['No Ridesharing', 'Insertion Heuristic', 'VGA'])

axes.set_xlabel("batch")
axes.set_ylabel("computational time [s]")

plt.savefig(config.images.ridesharing_performance_comparison, bbox_inches='tight', transparent=True)

plt.show()
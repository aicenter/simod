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
from amodsim.init import config

from typing import List, Tuple
import roadmaptools.inout
import amodsim.statistics.model.edges as edges
import amodsim.statistics.model.transit as transit
import amodsim.statistics.model.ridesharing as ridesharing
import amodsim.statistics.model.service as service
import matplotlib.pyplot as plt

delay_experiments = ["sw-vga-max_delay_3_min", "sw-vga", "sw-vga-max_delay_5_min", "sw-vga-max_delay_6_min"]
batch_experiments = ["sw-vga", "sw-vga-batch_60s", "sw-vga-batch_90s", "sw-vga-batch_120s"]

loaded_edges = roadmaptools.inout.load_geojson(config.agentpolis.map_edges_filepath)
edge_data = edges.make_data_frame(loaded_edges)


def load_experiment_data(experiment_name: str):
    # distance
    experiment_dir = "{}/{}/".format(config.experiments_dir, experiment_name)
    transit_data = transit.load(experiment_dir)
    km_total_window = int(round(transit.get_total_distance(transit_data, edge_data, True) / 1000 / 100))

    # time
    performance_data = ridesharing.load(experiment_dir)
    avg_time = performance_data['Group Generation Time'].mean() + performance_data['Solver Time'].mean()
    # avg_time = int(round(avg_time / 1000))
    avg_time = int(round(avg_time))

    # delay
    service_stat = service.load_dataframe(experiment_dir)
    delays_window = service.get_delays(service_stat, True, False)
    mean_delay = int(round(delays_window.mean() / 1000))

    return km_total_window, avg_time, mean_delay


speed_data = []
tt_data = []
delay_data = []
for exp_name in delay_experiments:
    record = load_experiment_data(exp_name)
    speed_data.append(record[0])
    tt_data.append(record[1])
    delay_data.append(record[2])

for exp_name in batch_experiments:
    record = load_experiment_data(exp_name)
    speed_data.append(record[0])
    tt_data.append(record[1])
    delay_data.append(record[2])

speed_data = [rec / 1000 for rec in speed_data]
tt_data = [rec / 1000 for rec in tt_data]


def plot_ax(xvalues, data: List[float], axis):
    axis.plot(xvalues, data, marker='o', linestyle='dashed', color='black')

x_values_delay = [3, 4, 5, 6]
x_values_batch = [30, 60, 90, 120]

fig, axes = plt.subplots(3, 2, sharey='row', sharex='col')
axes[0][0].set_ylabel("Total Distance \n" r"[km $\cdot 10^3$]")
axes[1][0].set_ylabel("Avg. comp. \n time [s]")
axes[2][0].set_ylabel("Avg. delay [s]")
axes[2][0].set_xlabel(r"Batch length [s]")
axes[2][0].set_xticks(x_values_batch)
axes[2][1].set_xlabel(r"Max delay [min]")

plot_ax(x_values_batch, speed_data[4:], axes[0][0])
plot_ax(x_values_delay, speed_data[:4], axes[0][1])
plot_ax(x_values_batch, tt_data[4:], axes[1][0])
plot_ax(x_values_delay, tt_data[:4], axes[1][1])
plot_ax(x_values_batch, delay_data[4:], axes[2][0])
plot_ax(x_values_delay, delay_data[:4], axes[2][1])

plt.savefig(config.images.sensitivity_analysis, bbox_inches='tight', transparent=True, pad_inches=0.0, dpi=fig.dpi)


plt.show()
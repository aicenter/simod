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
from typing import List, Tuple, Dict
import pandas as pd
import matplotlib.pyplot as plt
from itertools import islice
import os.path

from simod.init import config

import roadmaptools.inout
import simod.statistics.model.edges as edges
import simod.statistics.model.transit as transit
import simod.statistics.model.ridesharing as ridesharing
import simod.statistics.model.service as service
import simod.statistics.comparisons.common as common


def check_experiments(experiments: Dict[str, List[List[str]]]):
    for experiment_set in experiments.values():
        for method in experiment_set:
            for exp_name in method:
                experiment_dir = "{}/{}/".format(config.experiments_dir, exp_name)
                if not os.path.exists(experiment_dir):
                    raise Exception(f"experiment {experiment_dir} not found")


def load_experiment_data(experiment_name: str, edge_data: pd.DataFrame):
    experiment_dir = "{}/{}/".format(config.experiments_dir, experiment_name)

    # distance
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


def load_experiments(experiments: Dict[str, List[List[str]]], edge_data: pd.DataFrame):
    distance_data = []
    comp_time_data = []
    delay_data = []

    for experiment_set in experiments.values():
        for method in experiment_set:
            for exp_name in method:
                record = load_experiment_data(exp_name, edge_data)
                distance_data.append(record[0])
                comp_time_data.append(record[1])
                delay_data.append(record[2])

    speed_data = [rec / 1000 for rec in distance_data]
    tt_data = [rec / 1000 for rec in comp_time_data]

    return distance_data, comp_time_data, delay_data


def plot_ax(xvalues, data: List[float], axis, experiment: common.Experiment):
    axis.plot(xvalues, data, marker=experiment.marker, linestyle='dashed', color=experiment.color, label=experiment.label)


def plot_sensitivity_analysis(
        experiments: Dict[str, List[List[str]]],
        distance_data,
        comp_time_data,
        delay_data,
        x_values,
        exp_set_name: str = "peak"
):
    fig, axes = plt.subplots(3, 3, sharex='col')
    axes[0][0].set_ylabel("Avg. comp. \n time [s]")
    axes[1][0].set_ylabel("Total Distance \n" r"[km $\cdot 10^3$]")
    axes[2][0].set_ylabel("Avg. delay [s]")
    axes[2][0].set_xlabel(r"Batch length [s]")
    axes[2][0].set_xticks(x_values[0])
    axes[2][1].set_xlabel(r"Max delay [min]")
    axes[2][1].set_xticks(x_values[1])
    axes[2][2].set_xlabel(r"Capacity")
    axes[2][2].set_xticks(x_values[2])

    to_index = 0
    from_index = 0
    for experiment_set, axis_index in zip(experiments.values(), [1, 0, 2]):
        x_values_l = x_values[axis_index]
        for method, experiment in zip(experiment_set, islice(common.Experiment, 3, 6)):
            to_index += len(method)

            plot_ax(x_values_l[:len(method)], comp_time_data[from_index:to_index], axes[0][axis_index], experiment)
            plot_ax(x_values_l[:len(method)], distance_data[from_index:to_index], axes[1][axis_index], experiment)
            plot_ax(x_values_l[:len(method)], delay_data[from_index:to_index], axes[2][axis_index], experiment)

            from_index = to_index

    axes[0][0].legend(loc=1, fontsize='small', borderaxespad=0.2)
    plt.savefig(
        f"{config.images.images_dir}/sensitivity_analysis-{exp_set_name}.pdf",
        bbox_inches='tight',
        transparent=True,
        pad_inches=0.0,
        dpi=fig.dpi
    )


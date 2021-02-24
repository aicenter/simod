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

import matplotlib.pyplot as plt
import numpy as np
from matplotlib import rcParams

from roadmaptools.printer import print_info, print_table
from amodsim.statistics.model.traffic_load import WINDOW_START, WINDOW_END, WINDOW_LENGTH
from amodsim.statistics.model.vehicle_state import VehicleState
from amodsim.utils import to_percetnt, col_to_percent
import amodsim.statistics.model.traffic_load as traffic_load


HISTOGRAM_SAMPLES = 16
LOW_THRESHOLD = traffic_load.CRITICAL_DENSITY * 0.01
HIGH_THRESHOLD = traffic_load.CRITICAL_DENSITY * 2
CONGESTION_INDEX = int(traffic_load.CRITICAL_DENSITY / (HIGH_THRESHOLD / HISTOGRAM_SAMPLES))


class TrafficDensityHistogram:

    def __init__(self, edges):
        self.edges = edges
        pass
        # self.edge_ids_for_window = self.get_all_edge_ids_for_window()

    @staticmethod
    def get_all_edge_ids_for_window(load, window_start, window_end):
        edge_set = set()
        i = window_start
        while i <= window_end:
            window = load[i];
            for edge_id in window:
                edge_set.add(edge_id)
            i += 1
        return edge_set

    @staticmethod
    def get_histogram(average_density_list, bins, hist_step):
        average_density_list = np.clip(average_density_list, bins[0], bins[-1] + hist_step / 2)
        hist, bins = np.histogram(average_density_list, bins=bins)
        return hist, bins

    def plot_state(self, axis, average_density_list, hist_step, bins, centers, colors):

        hist, bins = self.get_histogram(average_density_list, bins, hist_step)

        axis.bar(centers, hist, hist_step, color=colors)

        return hist

    def plot_phases_share_histogram(self, loads, axis, average_density_list_total, hist_step, bins, centers, colors):
        average_density_by_phase = self.get_average_density_by_phase(loads)
        average_density_share_by_phase = average_density_by_phase / average_density_by_phase.sum(axis=0)

        hist_total, bins_total = self.get_histogram(average_density_list_total, bins, hist_step)

        hist_per_phase = []

        for phase in VehicleState:
            hist_per_phase.append(np.zeros(len(hist_total)))

        for index, density in enumerate(average_density_list_total):
            target_bin_index = 0
            for bin_index, bin_min_density in enumerate(bins_total):
                if bin_min_density > density:
                    target_bin_index = bin_index - 1
                    break

            for phase in VehicleState:
                hist_per_phase[phase.index][target_bin_index] += average_density_share_by_phase[phase.index][index]

        bottom = np.zeros(len(hist_total))
        for phase in VehicleState:
            axis.bar(centers, hist_per_phase[phase.index], 0.01, hatch=phase.pattern, color=colors, bottom=bottom,
                     edgecolor='lightgrey', label=phase.label, linewidth=0) #color=phase.color
            bottom += hist_per_phase[phase.index]

        return hist_per_phase

    def get_non_empty_edges_statistic(self, load):
        edge_id_set = self.get_all_edge_ids_for_window(load, WINDOW_START, WINDOW_END)
        length_sum = 0
        lane_count_sum = 0
        for edge_id in edge_id_set:
            edge = edges[edge_id]
            length_sum += edge["length"];
            lane_count_sum += edge["laneCount"]

        length_avg = length_sum / len(edge_id_set)
        lane_count_avg = lane_count_sum / len(edge_id_set)

        return length_avg, lane_count_avg

    def get_average_density_list(self, load):
        edge_id_set = self.get_all_edge_ids_for_window(load, WINDOW_START, WINDOW_END)
        average_density_list = np.zeros(len(edge_id_set))

        print_info("counting average load")

        i = 0
        for edge_id in edge_id_set:
            w = WINDOW_START
            sum = 0
            edge = self.edges[edge_id]
            while w <= WINDOW_END:
                if edge_id in load[w]:
                    sum += traffic_load.get_normalized_load(load[w][edge_id], edge["length"], edge["lanes"])
                w += 1
            average_density_list[i] = sum / WINDOW_LENGTH
            i += 1

        del edge_id_set
        del load

        # average_density_list_filtered = average_density_list[np.nonzero(average_density_list)]
        average_density_list_filtered = average_density_list[average_density_list > LOW_THRESHOLD]
        return average_density_list_filtered

    def get_average_density_by_phase(self, loads):
        edge_id_set = self.get_all_edge_ids_for_window(loads["ALL"], WINDOW_START, WINDOW_END)
        average_density_lists = np.zeros((5,len(edge_id_set)))

        print_info("counting average load")

        i = 0
        for edge_id in edge_id_set:
            edge = edges[edge_id]
            for phase in VehicleState:
                w = WINDOW_START
                sum = 0
                load = loads[phase.name]
                while w <= WINDOW_END:
                    if edge_id in load[w]:
                        sum += traffic_load.get_normalized_load(load[w][edge_id], edge["length"], edge["laneCount"])
                    w += 1
                average_density_lists[phase.index][i] = sum / WINDOW_LENGTH
            i += 1

        del edge_id_set
        del load

        # average_density_list_filtered = average_density_list[np.nonzero(average_density_list)]
        # average_density_list_filtered = average_density_list[average_density_list > LOW_THRESHOLD]
        # return average_density_list_filtered
        return average_density_lists

    def plot_phases_histogram(self,loads, axis):

        # for the sum of right outliers
        hist_step = HIGH_THRESHOLD / HISTOGRAM_SAMPLES
        bins = np.arange(0, HIGH_THRESHOLD, hist_step)

        average_density_list_by_phase = []
        colors = []
        for phase in VehicleState:
            average_density_list = self.get_average_density_list(loads[phase.name])

            # for the sum of right outliers
            average_density_list = np.clip(average_density_list, bins[0], bins[-1])

            average_density_list_by_phase.append(average_density_list)
            colors.append(phase.color)

        print_info("plotting")

        counts, bins, patches = axis.hist(average_density_list_by_phase, normed=False, bins=bins, stacked=True, color=colors)

        # labels
        self.set_histogram_labels(axis, bins)

        # plt.hist(average_density_list_by_phase, bins, stacked=True, color=colors)

    @staticmethod
    def set_histogram_labels(axis, bins):
        labels = [str("%.3f" % b) for b in bins[1:]]
        labels[-1] = str(HIGH_THRESHOLD) + '+'
        bin_centers = 0.5 * np.diff(bins) + bins[:-1]
        i = 0
        for x in bin_centers:
            # Label the raw counts
            axis.annotate(labels[i], xy=(x, 0), xycoords=('data', 'axes fraction'), xytext=(0, -18),
                        textcoords='offset points', va='top', ha='center')
            i += 1;

    @staticmethod
    def get_number_of_congested_edges(hist, congestion_index):
        return np.sum(hist[congestion_index:])


if __name__ == "__main__":
    edges = traffic_load.load_edges_mapped_by_id()
    loads = traffic_load.load_all_edges_load_history()

    fig, axis = \
        plt.subplots(1, 1, sharex=True, sharey=True, subplot_kw={"adjustable": 'datalim'}, figsize=(6, 4))

    # fix for the cut off label when resizing the graph
    rcParams.update({'figure.autolayout': True})

    # axis.set_xlabel("y")
    axis.set_xlabel("traffic density")
    axis.set_ylabel("edge count")

    # grid
    np.vectorize(lambda x: x.grid(True))(axis)

    histogram = TrafficDensityHistogram(edges)

    # for the sum of right outliers
    hist_step = HIGH_THRESHOLD / HISTOGRAM_SAMPLES
    bins = np.arange(0, HIGH_THRESHOLD + hist_step, hist_step)
    centers = [x + (hist_step / 2) for x in bins[0:HISTOGRAM_SAMPLES]]
    # colors = np.vectorize(traffic_load.get_color_from_normalized_load, [np.float, np.str])(np.copy(bins))
    # colors = np.frompyfunc(traffic_load.get_color_from_normalized_load, 1, 1)(np.copy(bins))
    colors = np.asarray(list(map(traffic_load.get_color_from_normalized_load, np.copy(bins))))

    # histogram.plot_phases_histogram(loads, axis[1])

    # critical density line
    axis.axvline(x=config.critical_density, linewidth=3, color='black', linestyle='--', label='critical density')

    # curent histogram
    average_density_list_total_current = histogram.get_average_density_list(loads[VehicleState.DRIVING_TO_TARGET_LOCATION.name]);
    hist = histogram.plot_state(axis, average_density_list_total_current, hist_step, bins, centers, colors)

    # legend
    axis.legend(prop={'size': 13})

    plt.savefig(config.images.traffic_density_current, bbox_inches='tight', transparent=True)


    # DETAILED HISTOGRAMS

    #  current histogram - detailed
    plt.axis([0.04, 0.16, 0, 100])
    plt.savefig(config.images.traffic_density_current_detail, bbox_inches='tight', transparent=True)

    # future histogram
    average_density_list_total_future = histogram.get_average_density_list(loads["ALL"]);
    hist_total = histogram.plot_state(axis, average_density_list_total_future, hist_step, bins, centers, colors)
    plt.savefig(config.images.traffic_density_future_detail, bbox_inches='tight', transparent=True)

    # stacked histogram
    hist_per_phase = histogram.plot_phases_share_histogram(
        loads, axis, average_density_list_total_future, hist_step, bins, centers, colors)

    # legend
    axis.legend(prop={'size': 13})
    np.vectorize(lambda x: x.set_facecolor('black'))(axis.get_legend().legendHandles[1:])

    plt.savefig(config.images.traffic_density_future_detail_stacked, bbox_inches='tight', transparent=True)

    total_load = 0
    total_load_in_window = 0
    for type_name in loads:
        type = loads[type_name]
        for index, timestep in enumerate(type):
            for edge_name in timestep:
                total_load += timestep[edge_name]
                if index >= WINDOW_START and index <= WINDOW_END:
                    total_load_in_window += timestep[edge_name]


    edge_count = len(edges)
    average_lenght_non_empty_edges, average_lane_count_non_empty_edges = histogram.get_non_empty_edges_statistic(loads["ALL"])
    average_density_in_time_window_non_empty_edges = np.average(average_density_list_total_future)
    average_density_in_time_window = np.sum(average_density_list_total_future) / len(edges)
    max_density_in_time_window = np.max(average_density_list_total_future)
    congested_edges_before = histogram.get_number_of_congested_edges(hist, CONGESTION_INDEX)
    congested_edges_now = histogram.get_number_of_congested_edges(hist_total, CONGESTION_INDEX)
    congestion_increase = (congested_edges_now - congested_edges_before) / congested_edges_before
    empty_edges_now = edge_count - len(average_density_list_total_current)
    empty_edges_future = edge_count - len(average_density_list_total_future)

    del loads

    print("Total edges: {0}".format(edge_count))
    print("Average length non-empty edges: {0}".format(average_lenght_non_empty_edges))
    print("Average lane count non-empty edges: {0}".format(average_lane_count_non_empty_edges))
    print("Total edges: {0}".format(edge_count))
    print("Total load on all edges: {0}".format(total_load))
    print("Total load on all edges in time window: {0}".format(total_load_in_window))
    print("Average density in time window: {0}".format(average_density_in_time_window))
    print("Average density in time window - non-empty edges: {0}".format(average_density_in_time_window_non_empty_edges))
    print("Max density in time window: {0}".format(max_density_in_time_window))
    print("Congested edges before: {0}".format(congested_edges_before))
    print("Empty edges before: {0} - {1} of total edges".format(empty_edges_now, to_percetnt(empty_edges_now / edge_count)))
    print("Congested edges now: {0}".format(congested_edges_now))
    print("Empty edges now: {0} - {1} of total edges".format(empty_edges_future, to_percetnt(empty_edges_future / edge_count)))
    print("Congestion increase: {0}".format(to_percetnt(congestion_increase)))
    print("Share per trip type:")

    output_table = np.empty((len(VehicleState), 3), dtype=object)

    for phase in VehicleState:
        sum_congested = histogram.get_number_of_congested_edges(hist_per_phase[phase.index], CONGESTION_INDEX)
        share_congested = sum_congested / congested_edges_now
        output_table[phase.index][0] = phase.name
        output_table[phase.index][1] = sum_congested
        output_table[phase.index][2] = share_congested

    output_table[:,2] = col_to_percent(output_table[:,2])
    print_table(output_table)

    del edges

    plt.show()









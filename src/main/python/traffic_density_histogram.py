from __future__ import print_function, division

import json
import matplotlib.pyplot as plt
import numpy as np

from scripts.config_loader import cfg as config
from scripts.printer import print_info, print_table
from traffic_load import WINDOW_START, WINDOW_END, WINDOW_LENGTH, VehiclePhase
from utils import to_percetnt, col_to_percent
import traffic_load


HISTOGRAM_SAMPLES = 16
LOW_THRESHOLD = traffic_load.CRITICAL_DENSITY * 0.01
HIGH_THRESHOLD = traffic_load.CRITICAL_DENSITY * 2
CONGESTION_INDEX = int(traffic_load.CRITICAL_DENSITY / (HIGH_THRESHOLD / HISTOGRAM_SAMPLES))

class TrafficDensityHistogram:

    def __init__(self):
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

    def plot_phases_share_histogram(self, loads, axis, average_density_list_total, hist_step, bins, centers):
        average_density_by_phase = self.get_average_density_by_phase(loads)
        average_density_share_by_phase = average_density_by_phase / average_density_by_phase.sum(axis=0)

        hist_total, bins_total = self.get_histogram(average_density_list_total, bins, hist_step)

        hist_per_phase = []

        for phase in VehiclePhase:
            hist_per_phase.append(np.zeros(len(hist_total)))

        for index, density in enumerate(average_density_list_total):
            target_bin_index = 0
            for bin_index, bin_min_density in enumerate(bins_total):
                if bin_min_density > density:
                    target_bin_index = bin_index - 1
                    break

            for phase in VehiclePhase:
                hist_per_phase[phase.index][target_bin_index] += average_density_share_by_phase[phase.index][index]

        bottom = np.zeros(len(hist_total))
        for phase in VehiclePhase:
            axis.bar(centers, hist_per_phase[phase.index], 0.01, color=phase.color, bottom=bottom)
            bottom += hist_per_phase[phase.index]

        return hist_per_phase

    def get_average_density_list(self, load):
        edge_id_set = self.get_all_edge_ids_for_window(load, WINDOW_START, WINDOW_END)
        average_density_list = np.zeros(len(edge_id_set))

        print_info("counting average load")

        i = 0
        for edge_id in edge_id_set:
            w = WINDOW_START
            sum = 0
            edge = edges[edge_id]
            while w <= WINDOW_END:
                if edge_id in load[w]:
                    sum += traffic_load.get_normalized_load(load[w][edge_id], edge["length"], edge["laneCount"])
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
            for phase in VehiclePhase:
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
        for phase in VehiclePhase:
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


edges = traffic_load.load_edges_mapped_by_id()
loads = traffic_load.load_all_edges_load_history()

fig, axis = \
    plt.subplots(1, 1, sharex=True, sharey=True, subplot_kw={"adjustable": 'datalim'}, figsize=(25, 12))

histogram = TrafficDensityHistogram()

# for the sum of right outliers
hist_step = HIGH_THRESHOLD / HISTOGRAM_SAMPLES
bins = np.arange(0, HIGH_THRESHOLD + hist_step, hist_step)
centers = bins[0:HISTOGRAM_SAMPLES]
colors = np.vectorize(traffic_load.get_color_from_normalized_load)(np.copy(bins))


# histogram.plot_phases_histogram(loads, axis[1])


# curent histogram
average_density_list_total = histogram.get_average_density_list(loads[VehiclePhase.DRIVING_TO_TARGET_LOCATION.name]);
hist = histogram.plot_state(axis, average_density_list_total, hist_step, bins, centers, colors)
plt.savefig(config.images.traffic_density_current, bbox_inches='tight', transparent=True)


# DETAILED HISTOGRAMS

#  current histogram
plt.axis([0.04, 0.16, 0, 2000])
plt.savefig(config.images.traffic_density_current_detail, bbox_inches='tight', transparent=True)

# future histogram
average_density_list_total = histogram.get_average_density_list(loads["ALL"]);
hist_total = histogram.plot_state(axis, average_density_list_total, hist_step, bins, centers, colors)
plt.savefig(config.images.traffic_density_future_detail, bbox_inches='tight', transparent=True)

# stacked histogram
hist_per_phase = histogram.plot_phases_share_histogram(loads, axis, average_density_list_total, hist_step, bins, centers)
plt.savefig(config.images.traffic_density_future_detail_stacked, bbox_inches='tight', transparent=True)

del edges
del loads



congested_edges_before = histogram.get_number_of_congested_edges(hist, CONGESTION_INDEX)
congested_edges_now = histogram.get_number_of_congested_edges(hist_total, CONGESTION_INDEX)
congestion_increase = (congested_edges_now - congested_edges_before) / congested_edges_before

print("Congested edges before: {0}".format(congested_edges_before))
print("Congested edges now: {0}".format(congested_edges_now))
print("Congestion increase: {0}".format(to_percetnt(congestion_increase)))
print("Share per trip type:")

output_table = np.empty((len(VehiclePhase), 3), dtype=object)

for phase in VehiclePhase:
    sum_congested = histogram.get_number_of_congested_edges(hist_per_phase[phase.index], CONGESTION_INDEX)
    share_congested = sum_congested / congested_edges_now
    output_table[phase.index][0] = phase.name
    output_table[phase.index][1] = sum_congested
    output_table[phase.index][2] = share_congested

output_table[:,2] = col_to_percent(output_table[:,2])
print_table(output_table)


plt.show()









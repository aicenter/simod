from __future__ import print_function, division

import json
import matplotlib.pyplot as plt
import numpy as np

from scripts.config_loader import cfg as config
from scripts.printer import print_info
from traffic_load import WINDOW_START, WINDOW_END, WINDOW_LENGTH, VehiclePhase
import traffic_load


HISTOGRAM_SAMPLES = 16
LOW_THRESHOLD = traffic_load.CRITICAL_DENSITY * 0.01
HIGH_THRESHOLD = traffic_load.CRITICAL_DENSITY * 2


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

    def plot_state(self, axis, average_density_list):

        print_info("plotting")

        hist_step = HIGH_THRESHOLD / HISTOGRAM_SAMPLES
        bins = np.arange(0, HIGH_THRESHOLD + hist_step, hist_step)
        counts, bins, patches = axis.hist(np.clip(average_density_list, bins[0], bins[-1] + hist_step/2), normed=False, bins=bins)

        # labels
        # self.set_histogram_labels(axis, bins)

        for value, patch in zip(bins, patches):
            plt.setp(patch, 'facecolor', traffic_load.get_color_from_normalized_load(value))

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

    def plot_phases_share_histogram(self, loads, axis, average_density_list_total):

        # for the sum of right outliers
        hist_step = HIGH_THRESHOLD / HISTOGRAM_SAMPLES
        bins = np.arange(0, HIGH_THRESHOLD, hist_step)

        colors = []
        for phase in VehiclePhase:
            colors.append(phase.color)

        average_density_by_phase = self.get_average_density_by_phase(loads)
        average_density_share_by_phase = average_density_by_phase / average_density_by_phase.sum(axis=0)

        hist_total, bins_total = np.histogram(average_density_list_total, bins=bins)

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
        centers = (bins_total[:-1] + bins_total[1:]) / 2
        for phase in VehiclePhase:
            p1 = axis.bar(centers, hist_per_phase[phase.index], 0.01, color=phase.color, bottom=bottom)
            bottom += hist_per_phase[phase.index]
            # p2 = plt.bar(ind, womenMeans, width,
            #              bottom=menMeans, yerr=womenStd)






        # stack = []
        # edge_id_list = self.get_all_edge_ids_for_window(loads["ALL"], WINDOW_START, WINDOW_END)
        # for id in edge_id_list:
        #     for phase in VehiclePhase:



edges = traffic_load.load_edges_mapped_by_id()
loads = traffic_load.load_all_edges_load_history()

fig, axis = \
    plt.subplots(1, 1, sharex=True, sharey=True, subplot_kw={"adjustable": 'datalim'}, figsize=(25, 12))

histogram = TrafficDensityHistogram()

average_density_list_total = histogram.get_average_density_list(loads[VehiclePhase.DRIVING_TO_TARGET_LOCATION.name]);

histogram.plot_state(axis, average_density_list_total)
# histogram.plot_phases_histogram(loads, axis[1])
# histogram.plot_phases_share_histogram(loads, axis[1], average_density_list_total)


plt.savefig(config.images.traffic_density_current, bbox_inches='tight', transparent=True)

plt.axis([0.04, 0.16, 0, 2000])
plt.savefig(config.images.traffic_density_current_detail, bbox_inches='tight', transparent=True)

average_density_list_total = histogram.get_average_density_list(loads["ALL"]);
histogram.plot_state(axis, average_density_list_total)
plt.savefig(config.images.traffic_density_future_detail, bbox_inches='tight', transparent=True)

del edges
del loads

plt.show()









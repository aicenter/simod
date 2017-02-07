#!/usr/bin/env python2
#encoding: UTF-8

from __future__ import print_function, division

import matplotlib.pyplot as plt
import matplotlib.ticker as ticker
import matplotlib.dates as dates
import json
import os
import itertools
import numpy as np

from scripts.config_loader import cfg as config
from scripts.printer import print_info
import traffic_load


SHIFT_DISTANCE = 30


# edges = traffic_load.load_edges()

edgePairs = traffic_load.load_edge_pairs()

loads = traffic_load.load_all_edges_load_history()

colorTypes = {}

CHOSEN_WINDOW_START = config.density_map.chosen_window_start
CHOSEN_WINDOW_END = config.density_map.chosen_window_end
CRITICAL_DENSITY = config.critical_density


def plot_edges_optimized(pairs, axis, loads=loads["ALL"], color_func=None):
    if color_func == None:
        color_func = get_color;

    for color in traffic_load.COLOR_LIST:
        colorType = {}
        colorType["xPairs"] = []
        colorType["yPairs"] = []
        colorType["width"] = 1.0 if color == traffic_load.NORMAL_COLOR else 2.0
        colorTypes[color] = colorType


    for pair in itertools.islice(pairs, 0, 100000000):
        edge1 = pair["edge1"];
        id1 = str(edge1["id"])
        color1 = color_func(loads, id=id1, length=edge1["length"], lane_count=edge1["laneCount"])

        if not pair["edge2"]:
            add_line([edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]],
                      id1, color1)
        else:
            edge2 = pair["edge2"];
            id2 = str(edge2["id"])
            color2 = color_func(loads, id=id2, length=edge2["length"], lane_count=edge2["laneCount"])
            line1 = compute_shift(
                [[edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]]],
                SHIFT_DISTANCE, 1)
            line2 = compute_shift(
                [[edge2["from"]["lonE6"], edge2["from"]["latE6"]], [edge2["to"]["lonE6"], edge2["to"]["latE6"]]],
                SHIFT_DISTANCE, 1)
            add_line(line1[0], line1[1], id1, color1)
            add_line(line2[0], line2[1], id2, color2)

    for color in traffic_load.COLOR_LIST:
        colorType = colorTypes[color]
        xList, yList = lines_to_list(colorType["xPairs"], colorType["yPairs"])
        axis.plot(xList, yList, linewidth=colorType["width"], color=color)


def add_line(a, b, id, color):
        colorTypes[color]["xPairs"].append([a[0], b[0]])
        colorTypes[color]["yPairs"].append([a[1], b[1]])


def lines_to_list(xpairs, ypairs):
    xlist = []
    ylist = []
    for xends, yends in zip(xpairs, ypairs):
        xlist.extend(xends)
        xlist.append(None)
        ylist.extend(yends)
        ylist.append(None)

    return xlist, ylist


# def get_color(edge, loads, scalarMap):
#     if str(edge["id"]) in loads[50]:
#         return scalarMap.to_rgba(loads[50][str(edge["id"])])
#     else:
#         return 'black'


def new_congestion_color(loads_all, id, length, lane_count):
    if length == 0:
        return traffic_load.NORMAL_COLOR
    loads_passanger_trip = loads["DRIVING_TO_TARGET_LOCATION"]
    load_total_passanger_trip = 0
    load_total_all = 0
    current_frame = CHOSEN_WINDOW_START
    while current_frame <= CHOSEN_WINDOW_END:
        if id in loads_passanger_trip[current_frame]:
            load_total_all += loads_all[current_frame][id]
            load_total_passanger_trip += loads_passanger_trip[current_frame][id]
        current_frame += 1

    average_load_all = load_total_all / (CHOSEN_WINDOW_END - CHOSEN_WINDOW_START)
    average_load_passanger_trip = load_total_passanger_trip / (CHOSEN_WINDOW_END - CHOSEN_WINDOW_START)
    if traffic_load.get_normalized_load(average_load_all, length, lane_count) > CRITICAL_DENSITY \
        and traffic_load.get_normalized_load(average_load_passanger_trip, length, lane_count) <= CRITICAL_DENSITY:
            return traffic_load.CONGESTED_COLOR
    else:
        return traffic_load.NORMAL_COLOR


def get_color(loads, id, length, lane_count):
    if length == 0:
        return traffic_load.NORMAL_COLOR
    load_total = 0
    current_frame = CHOSEN_WINDOW_START
    while current_frame <= CHOSEN_WINDOW_END:
        if id in loads[current_frame]:
            load_total += loads[current_frame][id]
        current_frame += 1

    average_load = load_total / (CHOSEN_WINDOW_END - CHOSEN_WINDOW_START)

    return traffic_load.get_color_from_load(load=average_load, length=length, lane_count=lane_count)


def compute_shift(line, distance, direction):
    normalVector = np.array([-(line[0][1] - line[1][1]), line[0][0] - line[1][0]])
    length = np.linalg.norm(normalVector)
    finalVector = normalVector / length * distance * direction
    return np.array([line[0] + finalVector, line[1] + finalVector])


def make_edge_pairs(edges):
    # for edge in edges:
    pairs = []
    while edges:
        edge1 = edges.pop(0)
        index = -1
        for index, edge in enumerate(edges):
            if edge["from"] == edge1["to"] and edge["to"] == edge1["from"]:
                break

        if index == -1:
            pairs.append([edge1])
        else:
            pairs.append([edge1, edges.pop(index)])

    return pairs

def location_quals(loc1, loc2):
    return loc1["lonE6"] == loc2["lonE6"] and loc1["latE6"] == loc2["latE6"]


pairs = edgePairs


# "adjustable": 'datalim', "aspect": 1.0 - naprosto nevim proc to takhle funguje - dokumentace == NULL
fig, axis = \
    plt.subplots(2, 3, sharex=True, sharey=True, subplot_kw={"adjustable": 'datalim', "aspect": 1.0},
                 figsize=(25, 12))


def set_mat_not(axis):
    # axis.fmt_xdata = ticker.ScalarFormatter(useMathText=True)
    # axis.fmt_ydata = ticker.ScalarFormatter(useMathText=True)
    # axis.fmt_xdata = dates.DateFormatter('%Y-%m-%d')
    # axis.yaxis.set_major_formatter(ticker.ScalarFormatter(useMathText=True))
    axis.set_xticklabels([])
    axis.set_yticklabels([])


np.vectorize(set_mat_not)(axis)


axis[0][0].set_xlabel("All")
axis[0][1].set_xlabel("To passenger")
axis[0][2].set_xlabel("Demanded trip")
axis[1][0].set_xlabel("To station")
axis[1][1].set_xlabel("Rebalancing")
axis[1][2].set_xlabel("New congestion")

print_info("plotting all load")
plot_edges_optimized(pairs, axis[0][0], loads["ALL"])

print_info("plotting to start location load")
plot_edges_optimized(pairs, axis[0][1], loads["DRIVING_TO_START_LOCATION"])

print_info("plotting to target location load")
plot_edges_optimized(pairs, axis[0][2], loads["DRIVING_TO_TARGET_LOCATION"])

print_info("plotting to station load")
plot_edges_optimized(pairs, axis[1][0], loads["DRIVING_TO_STATION"])

print_info("plotting rebalancing load")
plot_edges_optimized(pairs, axis[1][1], loads["REBALANCING"])

print_info("plotting new congestion")
plot_edges_optimized(pairs, axis[1][2], color_func=new_congestion_color)



# zoom
plt.axis([14308000, 14578000, 49970000, 50186000])

plt.savefig(config.images.main_map, bbox_inches='tight', transparent=True)

plt.show()




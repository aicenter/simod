#!/usr/bin/env python2
#encoding: UTF-8

from __future__ import print_function, division

import matplotlib.pyplot as plt
import json
import os
import os.path as pathi
import itertools
import matplotlib.cm as cm
import matplotlib.colors as colors
import numpy as np
from cPickle import loads

EDGES_FILE_PATH = "data/Prague/edges.json"

EDGE_PAIRS_FILE_PATH = "data/Prague/edgePairs.json"

LOADS_FILE_PATH = "data/Prague/allEdgesLoadHistory.json"

CRITICAL_DENSITY = 0.08
# CRITICAL_DENSITY = 0.08

SHIFT_DISTANCE = 30

CHOSEN_WINDOW = 989;

CHOSEN_WINDOW_START = 950;

CHOSEN_WINDOW_END = 1050;

NORMAL_COLOR = "black"

COLOR_1 = "sandybrown"

COLOR_2 = "darkorange"

COLOR_3 = "orangered"

CONGESTED_COLOR = "red"

color_list = {NORMAL_COLOR, COLOR_1, COLOR_2, COLOR_3, CONGESTED_COLOR}


os.chdir("../../../")

jsonFile = open(EDGES_FILE_PATH, 'r')
edges = json.loads(jsonFile.read())

jsonFile = open(EDGE_PAIRS_FILE_PATH, 'r')
edgePairs = json.loads(jsonFile.read())

jsonFile = open(LOADS_FILE_PATH, 'r')
loads = json.loads(jsonFile.read())

colorTypes = {}



def plot_edges_optimized(pairs, axis, loads):
    for color in color_list:
        colorType = {}
        colorType["xPairs"] = []
        colorType["yPairs"] = []
        colorTypes[color] = colorType


    for pair in itertools.islice(pairs, 0, 100000000):
        edge1 = pair["edge1"];
        id1 = str(edge1["id"])
        color1 = get_color(loads=loads, id=id1, length=edge1["length"], lane_count=edge1["laneCount"])

        if not pair["edge2"]:
            add_line([edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]],
                      id1, color1)
        else:
            edge2 = pair["edge2"];
            id2 = str(edge2["id"])
            color2 = get_color(loads=loads, id=id2, length=edge2["length"], lane_count=edge2["laneCount"])
            line1 = compute_shift(
                [[edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]]],
                SHIFT_DISTANCE, 1)
            line2 = compute_shift(
                [[edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]]],
                SHIFT_DISTANCE, -1)
            add_line(line1[0], line1[1], id1, color1)
            add_line(line2[0], line2[1], id2, color2)

    for color, colorType in colorTypes.iteritems():
        xList, yList = lines_to_list(colorType["xPairs"], colorType["yPairs"])
        axis.plot(xList, yList, linewidth=1.2, color=color)


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


def get_color(loads, id, length, lane_count):
    load_total = 0
    current_frame = CHOSEN_WINDOW_START
    while current_frame <= CHOSEN_WINDOW_END:
        if id in loads[current_frame]:
            load_total += loads[current_frame][id]
        current_frame += 1

    average_load = load_total / (CHOSEN_WINDOW_END - CHOSEN_WINDOW_START)

    return get_color_from_load(load=average_load, length=length, lane_count=lane_count)


def get_color_from_load(load, length, lane_count):
    if length == 0:
        return NORMAL_COLOR
    elif get_normalized_load(load, length, lane_count) > CRITICAL_DENSITY:
        return CONGESTED_COLOR
    elif get_normalized_load(load, length, lane_count) > CRITICAL_DENSITY * 0.75:
        return COLOR_3
    elif get_normalized_load(load, length, lane_count) > CRITICAL_DENSITY * 0.5:
        return COLOR_2
    elif get_normalized_load(load, length, lane_count) > CRITICAL_DENSITY * 0.25:
        return COLOR_1
    else:
        return NORMAL_COLOR


def get_normalized_load(load, length, lane_count):
    return load / length / lane_count


def compute_shift(line, distance, direction):
    normalVector = np.array([-abs(line[0][1] - line[1][1]), abs(line[0][0] - line[1][0])])
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

#
# line = [[1,3],[5,10]]
#
# line2 = compute_shift(line, 2, 1)

# reducedMatrix = np.delete(edgeTimeMatrix, np.nonzero(edgeTimeMatrix.sum()), axis=0)

# average load histogram
# plt.plot(np.sum(reducedMatrix, axis=0))

# for row in np.nditer(reducedMatrix):
#     if np.sum(row) == 0:


# matrix[matrix == 0] = np.nan

# plt.plot(np.sum(np.transpose(reducedMatrix), axis=0))

# pairs = make_edge_pairs(edges)

pairs = edgePairs;

# fig, axis = plt.subplots(1)

fig1 = plt.figure()
# fig2 = plt.figure()
# fig3 = plt.figure()
# fig4 = plt.figure()
# fig5 = plt.figure()

axis1 = fig1.add_subplot(321)
axis2 = fig1.add_subplot(322, sharex=axis1, sharey=axis1)
axis3 = fig1.add_subplot(323, sharex=axis1, sharey=axis1)
axis4 = fig1.add_subplot(324, sharex=axis1, sharey=axis1)
axis5 = fig1.add_subplot(325, sharex=axis1, sharey=axis1)

axis1.axis('equal')
axis2.axis('equal')
axis3.axis('equal')
axis4.axis('equal')
axis5.axis('equal')

# plot_edges(pairs, axis, loads)

plot_edges_optimized(pairs, axis1, loads["ALL"])
plot_edges_optimized(pairs, axis2, loads["DRIVING_TO_START_LOCATION"])
plot_edges_optimized(pairs, axis3, loads["DRIVING_TO_TARGET_LOCATION"])
plot_edges_optimized(pairs, axis4, loads["DRIVING_TO_STATION"])
plot_edges_optimized(pairs, axis5, loads["REBALANCING"])

plt.show()

# for pair in edgePairs:
#     if pair["edge2"] and str(pair["edge1"]["id"]) in loads[1000] and str(pair["edge2"]["id"]) in loads[1000]:
#         print(loads[1000][str(pair["edge1"]["id"])], loads[1000][str(pair["edge2"]["id"])])

# while edges:
#     edge1 = edges.pop()
#     for edge2 in edges:
#         if edge1["from"]["id"] == edge2["from"]["id"] and edge1["to"]["id"] == edge2["to"]["id"]:
#             print(1)



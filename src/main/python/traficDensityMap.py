#!/usr/bin/env python2
#encoding: UTF-8

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

SHIFT_DISTANCE = 3

CHOSEN_WINDOW = 989;

NORMAL_COLOR = "black"

CONGESTED_COLOR = "red"




os.chdir("../../../")

jsonFile = open(EDGES_FILE_PATH, 'r')
edges = json.loads(jsonFile.read())

jsonFile = open(EDGE_PAIRS_FILE_PATH, 'r')
edgePairs = json.loads(jsonFile.read())

jsonFile = open(LOADS_FILE_PATH, 'r')
loads = json.loads(jsonFile.read())

plt.gca().set_aspect('equal', adjustable='box')

fig, axis = plt.subplots(1)

xPairs = [];
yPairs = [];
xPairsCong = [];
yPairsCong = [];




def plot_edges(pairs, axis, loads):
    jet = plt.get_cmap('jet')
    cNorm = colors.Normalize(vmin=0, vmax=300)
    scalarMap = cm.ScalarMappable(norm=cNorm, cmap=jet)
    
    for pair in itertools.islice(pairs, 0, 100000000):
#        axis.plot([edge["from"]["latProjected"],edge["from"]["lonProjected"]],
#        [edge["to"]["latProjected"],edge["to"]["lonProjected"]], linewidth = 1.2, color='black')

        edge1 = pair["edge1"];
        id1 = str(edge1["id"])
        color1 = get_color(loads=loads, id=id1, length=edge1["length"], laneCount=edge1["laneCount"])

        if not pair["edge2"]:
            plot_edge([edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]],
                      id1, color1)
        else:
            edge2 = pair["edge2"];
            id2 = str(edge2["id"])
            color2 = get_color(loads=loads, id=id2, length=edge2["length"], laneCount=edge2["laneCount"])
            line1 = compute_shift(
                [[edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]]],
                SHIFT_DISTANCE, 1)
            line2 = compute_shift(
                [[edge1["from"]["lonE6"], edge1["from"]["latE6"]], [edge1["to"]["lonE6"], edge1["to"]["latE6"]]],
                SHIFT_DISTANCE, -1)
            plot_edge(line1[0], line1[1], id1, color1)
            plot_edge(line2[0], line2[1], id2, color2)

def plot_edges_optimized(pairs, axis, loads):
    for pair in itertools.islice(pairs, 0, 100000000):
        edge1 = pair["edge1"];
        id1 = str(edge1["id"])
        color1 = get_color(loads=loads, id=id1, length=edge1["length"], laneCount=edge1["laneCount"])

        if not pair["edge2"]:
            add_line([edge1["from"]["lonProjected"], edge1["from"]["latProjected"]], [edge1["to"]["lonProjected"], edge1["to"]["latProjected"]],
                      id1, color1)
        else:
            edge2 = pair["edge2"];
            id2 = str(edge2["id"])
            color2 = get_color(loads=loads, id=id2, length=edge2["length"], laneCount=edge2["laneCount"])
            line1 = compute_shift(
                [[edge1["from"]["lonProjected"], edge1["from"]["latProjected"]], [edge1["to"]["lonProjected"], edge1["to"]["latProjected"]]],
                SHIFT_DISTANCE, 1)
            line2 = compute_shift(
                [[edge1["from"]["lonProjected"], edge1["from"]["latProjected"]], [edge1["to"]["lonProjected"], edge1["to"]["latProjected"
                                                                                                                           ""]]],
                SHIFT_DISTANCE, -1)
            add_line(line1[0], line1[1], id1, color1)
            add_line(line2[0], line2[1], id2, color2)

    xList, yList = lines_to_list(xPairs, yPairs)
    xCongList, yCongList = lines_to_list(xPairsCong, yPairsCong)

    axis.plot(xList, yList, linewidth=1.2, color=NORMAL_COLOR)
    axis.plot(xCongList, yCongList, linewidth=1.2, color=CONGESTED_COLOR)


def plot_edge(a, b, id, color):
    # if id in loads[CHOSEN_WINDOW]:
        axis.plot([a[0], b[0]], [a[1], b[1]], linewidth=1.2, color=color)
        # color = get_color(edge, loads, scalarMap))
    # else:
    #     axis.plot([edge["from"]["lonE6"], edge["to"]["lonE6"]], [edge["from"]["latE6"], edge["to"]["latE6"]],
    #
    #         linewidth=1.2, color="black")


def add_line(a, b, id, color):
    if color == "black":
        xPairs.append([a[0], b[0]])
        yPairs.append([a[1], b[1]])
    else:
        xPairsCong.append([a[0], b[0]])
        yPairsCong.append([a[1], b[1]])


def lines_to_list(xpairs, ypairs):
    xlist = []
    ylist = []
    for xends, yends in zip(xpairs, ypairs):
        xlist.extend(xends)
        xlist.append(None)
        ylist.extend(yends)
        ylist.append(None)

    return xlist, ylist


def get_color(edge, loads, scalarMap):
    if str(edge["id"]) in loads[50]:
        return scalarMap.to_rgba(loads[50][str(edge["id"])])
    else:
        return 'black'


def get_color(loads, id, length, laneCount):
    if id not in loads[CHOSEN_WINDOW]:
        return "black"
    return get_color_from_load(load=loads[CHOSEN_WINDOW][id], length=length, laneCount=laneCount)


def get_color_from_load(load, length, laneCount):
    if length == 0:
        return "black"
    if load / length / laneCount > CRITICAL_DENSITY:
        return "red"
    else:
        return "black"


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

# plot_edges(pairs, axis, loads)

plot_edges_optimized(pairs, axis, loads)

plt.show()

# for pair in edgePairs:
#     if pair["edge2"] and str(pair["edge1"]["id"]) in loads[1000] and str(pair["edge2"]["id"]) in loads[1000]:
#         print(loads[1000][str(pair["edge1"]["id"])], loads[1000][str(pair["edge2"]["id"])])

# while edges:
#     edge1 = edges.pop()
#     for edge2 in edges:
#         if edge1["from"]["id"] == edge2["from"]["id"] and edge1["to"]["id"] == edge2["to"]["id"]:
#             print(1)



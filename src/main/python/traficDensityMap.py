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

edgesfilePath = "data/Prague/edges.json"

loadsfilePath = "data/Prague/allEdgesLoadHistory.json"

os.chdir("../../../")

jsonFile = open(edgesfilePath, 'r')
edges = json.loads(jsonFile.read())

jsonFile = open(loadsfilePath, 'r')
loads = json.loads(jsonFile.read())

fig, axis = plt.subplots(1)



# axis.axis()


def plot_edges(edges, axis, loads):
    jet = plt.get_cmap('jet')
    cNorm = colors.Normalize(vmin=0, vmax=300)
    scalarMap = cm.ScalarMappable(norm=cNorm, cmap=jet)
    
    for edge in itertools.islice(edges, 0, 100000):
#        axis.plot([edge["from"]["latProjected"],edge["from"]["lonProjected"]],
#        [edge["to"]["latProjected"],edge["to"]["lonProjected"]], linewidth = 1.2, color='black')
        if str(edge["id"]) in loads[50]:
            axis.plot([edge["from"]["lonE6"],edge["to"]["lonE6"]], [edge["from"]["latE6"],edge["to"]["latE6"]],
                linewidth = 1.2, color = get_color(edge, loads, scalarMap))


def get_color(edge, loads, scalarMap):
    if str(edge["id"]) in loads[50]:
        return scalarMap.to_rgba(loads[50][str(edge["id"])])
    else:
        return 'black'


def create_edge_time_matrix(edges, dictionary, loads):
    edgeArray = np.array([], np.int32);
    for edge in edges:
        edgeArray = np.append(edgeArray, edge["id"])

    np.sort(edgeArray)

    index = 0;
    for edge in np.nditer(edgeArray):
        dictionary[edge.item()] = index
        index += 1

    matrix = np.zeros((len(edgeArray), len(loads)))

    for time, timeData in enumerate(loads):
        for id, load in timeData.iteritems():
            matrix[dictionary[int(id)]][time] = load

    return matrix

edgeTimeMatrix = create_edge_time_matrix(edges, {}, loads)

reducedMatrix = np.delete(edgeTimeMatrix, np.nonzero(edgeTimeMatrix.sum()), axis=0)

# average load histogram
plt.plot(np.sum(reducedMatrix, axis=0))

# for row in np.nditer(reducedMatrix):
#     if np.sum(row) == 0:


# matrix[matrix == 0] = np.nan

# plt.plot(np.sum(np.transpose(reducedMatrix), axis=0))

# plot_edges(edges, axis, loads)


import matplotlib.pyplot as plt
import json
import os
import numpy as np
import math

edgesfilePath = "data/Prague/edges.json"

loadsfilePath = "data/Prague/allEdgesLoadHistory.json"

CRITICAL_DENSITY = 0.08

os.chdir("../../../")

jsonFile = open(edgesfilePath, 'r')
edges = json.loads(jsonFile.read())

jsonFile = open(loadsfilePath, 'r')
loads = json.loads(jsonFile.read())


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
            matrix[dictionary[id]][time] = load

    return matrix


edgeTimeMatrix = create_edge_time_matrix(edges, {}, loads)

reducedMatrix = np.delete(edgeTimeMatrix, np.nonzero(edgeTimeMatrix.sum()), axis=0)
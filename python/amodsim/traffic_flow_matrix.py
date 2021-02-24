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
from __future__ import print_function, division

import json
import os
import numpy as np

from scripts.config_loader import cfg as config
from scripts.printer import print_info


edgesfilePath = "data/Prague/edges.json"

loadsfilePath = "data/Prague/allEdgesLoadHistory.json"

CRITICAL_DENSITY = 0.08

os.chdir("../../../")

jsonFile = open(edgesfilePath, 'r')
edges = json.loads(jsonFile.read())

jsonFile = open(loadsfilePath, 'r')
loads = json.loads(jsonFile.read())

# edges mapped by edge id
em = {}
for edge in edges:
    em[edge["id"]] = edge


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


def create_edge_time_matrix_normalized(edges, dictionary, loads):
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
            length = em[id]["length"]
            laneCount = em[id]["laneCount"]
            matrix[dictionary[id]][time] = load / length / laneCount

    return matrix


# edgeTimeMatrix = create_edge_time_matrix(edges, {}, loads)

edgeTimeMatrix = create_edge_time_matrix_normalized(edges, {}, loads["ALL"])

reducedMatrix = np.delete(edgeTimeMatrix, np.nonzero(edgeTimeMatrix.sum()), axis=0)

edgeTimeMatrix = create_edge_time_matrix_normalized(edges, {}, loads["DRIVING_TO_TARGET_LOCATION"])

reducedMatrix2 = np.delete(edgeTimeMatrix, np.nonzero(edgeTimeMatrix.sum()), axis=0)
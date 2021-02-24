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

from statistics.model.traffic_load import get_total_load_sum


def print_load_history(path):
    total_load, total_load_in_window = get_total_load_sum(path)
    print("File: {0}".format(path))
    print("Total load: {0}".format(total_load))
    print("Total load in window: {0}".format(total_load_in_window))
    print("")

path1 = "/home/fido/AIC data/Shared/amodsim-data/agentpolis-experiment/Prague/experiment/default/allEdgesLoadHistory-minuteInterval.json"
path2 = "/home/fido/AIC data/Shared/amodsim-data/agentpolis-experiment/Prague/experiment/default/allEdgesLoadHistory.json"
path3 = "/home/fido/AIC data/Shared/amodsim-data/agentpolis-experiment/Prague/experiment/scaled/allEdgesLoadHistory.json"
path4 = "/home/fido/AIC data/Shared/amodsim-data/agentpolis-experiment/Prague/experiment/scaled/test/allEdgesLoadHistory.json"

print_load_history(path1)
print_load_history(path2)
print_load_history(path3)
print_load_history(path4)

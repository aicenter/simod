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
import json
import numpy as np

from amodsim.utils import col_to_percent, to_percetnt
from roadmaptools.printer import print_table


json_file = open(config.statistics.result_file_path, 'r')
result = json.loads(json_file.read())

avg_km_total = result["averageKmWithPassenger"] + result["averageKmToStartLocation"] + result["averageKmToStation"] \
               + result["averageKmRebalancing"]

demand_share = result["averageKmWithPassenger"] / avg_km_total
pickup_share = result["averageKmToStartLocation"] / avg_km_total
drop_off_share = result["averageKmToStation"] / avg_km_total
rebalancing_share = result["averageKmRebalancing"] / avg_km_total
total_vehicles = result["numberOfVehicles"]
total_demands = result["demandsCount"]
dropped_demands = result["numberOfDemandsDropped"]

output_table = np.array([["TRIP TYPE", "AVG KM PER VEHICLE", "SHARE ON TRAFFIC"],
                ["Demand trips", result["averageKmWithPassenger"], demand_share],
                ["Pickup trips", result["averageKmToStartLocation"], pickup_share],
                ["Drop off trips", result["averageKmToStation"], drop_off_share],
                ["Rebalancing trips", result["averageKmRebalancing"], rebalancing_share]])

output_table[1:,2] = col_to_percent(output_table[1:,2])


print("Total vehicles: {0}".format(total_vehicles))

print_table(output_table)

print("Total demands: {0}".format(total_demands))

print("Dropped demands: {0} - {1}".format(dropped_demands, to_percetnt(dropped_demands / total_demands, 2)))

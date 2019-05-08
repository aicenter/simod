from amodsim.init import config
import json
import numpy as np

from amodsim.utils import col_to_percent, to_percetnt
from roadmaptools.printer import print_table


json_file = open(config.amodsim.statistics.result_file_path, 'r')
result = json.loads(json_file.read())

"""avg_km_total = int(result["averageKmWithPassenger"] + result["averageKmToStartLocation"] + result["averageKmToStation"] \
               + result["averageKmRebalancing"])

demand_share = int(result["averageKmWithPassenger"]) / avg_km_total
pickup_share = int(result["averageKmToStartLocation"]) / avg_km_total
drop_off_share = int(result["averageKmToStation"]) / avg_km_total
rebalancing_share = int(result["averageKmRebalancing"]) / avg_km_total"""
total_vehicles = int(result["numberOfVehicles"])
#total_demands = int(result["demandsCount"])
#dropped_demands = int(result["numberOfDemandsDropped"])

"""output_table = np.array([["TRIP TYPE", "AVG KM PER VEHICLE", "SHARE ON TRAFFIC"],
                ["demnad trips", result["averageKmWithPassenger"], demand_share],
                ["pickup trips", result["averageKmToStartLocation"], pickup_share],
                ["drop off trips", result["averageKmToStation"], drop_off_share],
                ["rebalancing trips", result["averageKmRebalancing"], rebalancing_share]])

output_table[1:,2] = col_to_percent(output_table[1:,2])
"""

print("Total vehicles: {0}".format(total_vehicles))

#print_table(output_table)

#print("Total demands: {0}".format(total_demands))

#print("Dropped demands: {0} - {1}".format(dropped_demands, to_percetnt(dropped_demands / total_demands, 2)))

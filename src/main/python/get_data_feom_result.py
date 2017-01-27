

import json
import numpy as np

from scripts.config_loader import cfg as config


def print_table(table):
    col_width = max(len(str(word)) for row in table for word in row) + 2  # padding
    for row in table:
        print "".join(str(word).ljust(col_width) for word in row)


def to_percent(collection):
    i = 0
    for float_number in collection:
        collection[i] = "{0:.1f}%".format(float(float_number) * 100)
        i += 1
    return collection



json_file = open(config.agentpolis.statistics.result_file_path, 'r')
result = json.loads(json_file.read())

avg_km_total = result["averageKmWithPassenger"] + result["averageKmToStartLocation"] + result["averageKmToStation"] \
               + result["averageKmRebalancing"]

demand_share = result["averageKmWithPassenger"] / avg_km_total
pickup_share = result["averageKmToStartLocation"] / avg_km_total
drop_off_share = result["averageKmToStation"] / avg_km_total
rebalancing_share = result["averageKmRebalancing"] / avg_km_total

output_table = np.array([["TRIP TYPE", "AVG KM PER VEHICLE", "SHARE ON TRAFFIC"],
                ["demnad trips", result["averageKmWithPassenger"], demand_share],
                ["pickup trips", result["averageKmToStartLocation"], pickup_share],
                ["drop off trips", result["averageKmToStation"], drop_off_share],
                ["rebalancing trips", result["averageKmRebalancing"], rebalancing_share]])

output_table[1:,2] = to_percent(output_table[1:,2])

print_table(output_table)

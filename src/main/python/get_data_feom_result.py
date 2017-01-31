

import json
import numpy as np

from scripts.config_loader import cfg as config
from utils import col_to_percent
from scripts.printer import print_table


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

output_table[1:,2] = col_to_percent(output_table[1:,2])

print_table(output_table)

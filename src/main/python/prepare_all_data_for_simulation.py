from __future__ import print_function, division

from scripts.config_loader import cfg as config
from scripts.printer import print_info
from scripts.prague.prepare_trips import get_trips
from amod.common import TransposedUTM
from scripts.prague.generate_stations import generate_stations
from scripts.distance_matrix import compute_distance_matrix


print_info("PREPARATIONS STARTED")

print_info("1) - getting trips")
get_trips(config)

projection = TransposedUTM(config["tutm_projection_centre"]["latitude"],
                               config["tutm_projection_centre"]["longitude"])

print_info("2) - generating stations")
generate_stations(config, projection)

print_info("3) - computing distance matrix")
compute_distance_matrix(config)


print_info("PREPARATIONS COMPLETED")






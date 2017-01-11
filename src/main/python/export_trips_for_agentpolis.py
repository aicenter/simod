from __future__ import print_function, division

import numpy as np

from amod.common import tutm2latlon
from amod.common import load_trips


def export_data_for_amodsim(config, projection):
    trips = load_trips(config.trips_file_path, projection)

    trips_out = np.zeros((len(trips[:]), 5))

    trips_out[:,0] = trips[:,0] * 1000

    trips_out[:,1:3] = tutm2latlon(trips[:,2:4], projection)
    trips_out[:,3:5] = tutm2latlon(trips[:,4:6], projection)

    np.savetxt(config.agentpolis.trips_path, trips_out, delimiter=" ", fmt='%f')


if __name__ == "__main__":
    from amod.common import TransposedUTM
    from scripts.config_loader import cfg as config

    projection = TransposedUTM(config["tutm_projection_centre"]["latitude"],
                               config["tutm_projection_centre"]["longitude"])
    export_data_for_amodsim(config, projection)

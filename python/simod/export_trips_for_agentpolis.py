#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of the SiMoD project.
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

import numpy as np
import os

from simod.common import tutm2latlon
from simod.common import load_trips


def export_data_for_amodsim(config, projection):
    trips = load_trips(config.trips_file_path, projection)

    trips_out = np.zeros((len(trips[:]), 5))

    trips_out[:,0] = trips[:,0] * 1000

    trips_out[:,1:3] = tutm2latlon(trips[:,2:4], projection)
    trips_out[:,3:5] = tutm2latlon(trips[:,4:6], projection)

    if not os.path.exists(config.amodsim_experiment_dir):
        os.makedirs(config.amodsim_experiment_dir)

    np.savetxt(config.agentpolis.trips_path, trips_out, delimiter=" ", fmt='%f')


if __name__ == "__main__":
    from amodsim.common import TransposedUTM
    from scripts.config_loader import cfg as config

    projection = TransposedUTM(config["tutm_projection_centre"]["latitude"],
                               config["tutm_projection_centre"]["longitude"])
    export_data_for_amodsim(config, projection)

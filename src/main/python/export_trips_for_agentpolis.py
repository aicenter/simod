from __future__ import print_function, division

import numpy as np

from scripts import trip_loader
from amod.common import tutm2latlon



AGENPOLIS_DATA_PATH = '../../../data/Prague/'


projection = trip_loader.projection

trips = trip_loader.trips

trips_out = np.zeros((len(trips[:]), 5))

trips_out[:,0] = trips[:,0] * 1000

trips_out[:,1:3] = tutm2latlon(trips[:,2:4], projection)
trips_out[:,3:5] = tutm2latlon(trips[:,4:6], projection)

np.savetxt(AGENPOLIS_DATA_PATH + "car-trips-mixed-trips-completed.txt", trips_out, delimiter=" ", fmt='%f')


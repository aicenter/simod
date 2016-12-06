import numpy as np

import sys

sys.path.insert(0,'../../../../amod/python/')

from amod.common import TransposedUTM, tutm2latlon
from postgis_trips import load_trips
from settings import SCK_PATH

OUTPUT_FILE_NAME = "car-trips-new"

trips = load_trips(100000000)

print("Loaded " + str(len(trips))  + " trips")

np.savez(SCK_PATH + OUTPUT_FILE_NAME, trips)
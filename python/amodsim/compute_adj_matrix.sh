#!/bin/bash -x
ml Gurobi/8.1.1-foss-2018b-Python-3.6.6
ml Python/3.6.6-foss-2018b
python compute_station_locations.py -lc="/home/fiedlda1/Amodsim/local_config_files/RCI.cfg"
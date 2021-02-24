#!/bin/bash -x
##
## Copyright (c) 2021 Czech Technical University in Prague.
##
## This file is part of Amodsim project.
##
## This program is free software: you can redistribute it and/or modify
## it under the terms of the GNU Lesser General Public License as published by
## the Free Software Foundation, either version 3 of the License, or
## (at your option) any later version.
##
## This program is distributed in the hope that it will be useful,
## but WITHOUT ANY WARRANTY; without even the implied warranty of
## MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
## GNU Lesser General Public License for more details.
##
## You should have received a copy of the GNU Lesser General Public License
## along with this program. If not, see <http://www.gnu.org/licenses/>.
##
ml Gurobi/8.1.1-foss-2018b-Python-3.6.6
ml Python/3.6.6-foss-2018b
python compute_station_locations.py -lc="/home/fiedlda1/Amodsim/local_config_files/RCI.cfg"
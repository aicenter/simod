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
PATH=$PATH:/home/fiedlda1/apache-maven-3.6.1/bin/
ml Java
ml Gurobi
mvn exec:exec -Dexec.executable=java -Dexec.args="-classpath %classpath -Xmx25g cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation /home/fiedlda1/Amodsim/local_config_files/RCI/ih-sw.cfg" -Dfile.encoding=UTF-8

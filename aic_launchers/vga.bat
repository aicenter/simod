::
:: Copyright (c) 2021 Czech Technical University in Prague.
::
:: This file is part of the SiMoD project.
::
:: This program is free software: you can redistribute it and/or modify
:: it under the terms of the GNU Lesser General Public License as published by
:: the Free Software Foundation, either version 3 of the License, or
:: (at your option) any later version.
::
:: This program is distributed in the hope that it will be useful,
:: but WITHOUT ANY WARRANTY; without even the implied warranty of
:: MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
:: GNU Lesser General Public License for more details.
::
:: You should have received a copy of the GNU Lesser General Public License
:: along with this program. If not, see <http://www.gnu.org/licenses/>.
::
mvn exec:exec -Dexec.executable="java" -Dexec.args="-classpath %%classpath -Xmx60g cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation C:/Workspaces/AIC/amod-to-agentpolis/local_config_files/AIC/vga.cfg" -Dfile.encoding=UTF-8
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
from fconfig import configuration
from roadmaptools.config.roadmaptools_config import RoadmaptoolsConfig

# configuration.generate_config(r"C:\Workspaces\AIC\amod-to-agentpolis\src\main\resources\cz\cvut\fel\aic\amodsim\config/config.cfg")

configuration.generate_config((RoadmaptoolsConfig, "roadmaptools"))
#
# Copyright (c) 2021 Czech Technical University in Prague.
#
# This file is part of Amodsim project.
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

class Statistics:
    def __init__(self, properties: dict=None):
        self.result_file_name = properties.get("result_file_name")
        self.result_file_path = properties.get("result_file_path")
        self.all_edges_load_history_file_name = properties.get("all_edges_load_history_file_name")
        self.all_edges_load_history_file_path = properties.get("all_edges_load_history_file_path")
        self.occupancies_file_name = properties.get("occupancies_file_name")
        self.occupancies_file_path = properties.get("occupancies_file_path")
        self.service_file_name = properties.get("service_file_name")
        self.transit_file_name = properties.get("transit_file_name")
        self.ridesharing_stats_file_name = properties.get("ridesharing_stats_file_name")


        pass


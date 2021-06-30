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

class Images:

    def __init__(self, properties: dict = None):
        self.images_dir = properties.get("images_dir")
        self.occupancy_histogram = properties.get("occupancy_histogram")
        self.occupancy_histogram_window = properties.get("occupancy_histogram_window")
        self.occupancy_histogram_comparison = properties.get("occupancy_histogram_comparison")
        self.occupancy_histogram_comparison_combined = properties.get("occupancy_histogram_comparison_combined")
        self.delay_histogram_comparison = properties.get("delay_histogram_comparison")
        self.delay_histogram_comparison_combined = properties.get("delay_histogram_comparison_combined")
        self.traffic_density_map_comparison = properties.get("traffic_density_map_comparison")
        self.ridesharing_performance_comparison = properties.get("ridesharing_performance_comparison")
        self.stations = properties.get("stations")
        self.demand_heatmap = properties.get("demand_heatmap")
        self.distance_delay_tradeoff = properties.get("distance_delay_tradeoff")
        self.vga_times = properties.get("vga_times")
        self.vga_group_size = properties.get("vga_group_size")
        self.demand_trip_duration_histogram = properties.get("demand_trip_duration_histogram")
        self.demand_example = properties.get("demand_example")
        self.sensitivity_analysis = properties.get("sensitivity_analysis")
        pass

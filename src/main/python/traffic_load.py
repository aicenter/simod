from __future__ import print_function, division

import json
from enum import Enum

from scripts.config_loader import cfg as config
from scripts.printer import print_info


CRITICAL_DENSITY = config.critical_density

NORMAL_COLOR = "lightgrey"

COLOR_1 = "lemonchiffon"

COLOR_2 = "navajowhite"

COLOR_3 = "lightsalmon"

COLOR_4 = "darkorange"

COLOR_5 = "orangered"

CONGESTED_COLOR = "red"

WINDOW_LENGTH = config.density_map.chosen_window_end - config.density_map.chosen_window_start
WINDOW_START = config.density_map.chosen_window_start
WINDOW_END = config.density_map.chosen_window_end

COLOR_LIST = [NORMAL_COLOR, COLOR_1, COLOR_2, COLOR_3, COLOR_4, COLOR_5, CONGESTED_COLOR]


def load_edges():
    print_info("loading edges")
    json_file = open(config.agentpolis.edges_file_path, 'r')
    return json.loads(json_file.read())


def load_all_edges_load_history():
    print_info("loading edge load history")
    json_file = open(config.agentpolis.statistics.all_edges_load_history_file_path, 'r')
    return json.loads(json_file.read())


def load_edges_mapped_by_id():
    edges = load_edges()
    edges_mapped_by_id = {}
    for edge in edges:
        edges_mapped_by_id[edge["id"]] = edge

    return edges_mapped_by_id


def get_normalized_load(load, length, lane_count):
    return load / length / lane_count


def get_color_from_load(load, length, lane_count):
    normalized_load = get_normalized_load(load, length, lane_count)
    return get_color_from_normalized_load(normalized_load)


def get_color_from_normalized_load(load):
    if load > CRITICAL_DENSITY:
        return CONGESTED_COLOR
    elif load > CRITICAL_DENSITY * 0.75:
        return COLOR_5
    elif load > CRITICAL_DENSITY * 0.5:
        return COLOR_4
    elif load > CRITICAL_DENSITY * 0.25:
        return COLOR_3
    elif load > CRITICAL_DENSITY * 0.1:
        return COLOR_2
    elif load > CRITICAL_DENSITY * 0.05:
        return COLOR_1
    else:
        return NORMAL_COLOR


class VehiclePhase(Enum):
    DRIVING_TO_TARGET_LOCATION = ("blue", 0)
    DRIVING_TO_START_LOCATION = ("green", 1)
    DRIVING_TO_STATION = ("black", 2)
    REBALANCING = ("red", 3)

    def __init__(self, color, index):
        self.color = color
        self.index = index
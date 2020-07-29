
from statistics.model.traffic_load import get_total_load_sum


def print_load_history(path):
    total_load, total_load_in_window = get_total_load_sum(path)
    print("File: {0}".format(path))
    print("Total load: {0}".format(total_load))
    print("Total load in window: {0}".format(total_load_in_window))
    print("")

path1 = "/home/fido/AIC data/Shared/amodsim-data/agentpolis-experiment/Prague/experiment/default/allEdgesLoadHistory-minuteInterval.json"
path2 = "/home/fido/AIC data/Shared/amodsim-data/agentpolis-experiment/Prague/experiment/default/allEdgesLoadHistory.json"
path3 = "/home/fido/AIC data/Shared/amodsim-data/agentpolis-experiment/Prague/experiment/scaled/allEdgesLoadHistory.json"
path4 = "/home/fido/AIC data/Shared/amodsim-data/agentpolis-experiment/Prague/experiment/scaled/test/allEdgesLoadHistory.json"

print_load_history(path1)
print_load_history(path2)
print_load_history(path3)
print_load_history(path4)

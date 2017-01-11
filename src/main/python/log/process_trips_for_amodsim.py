
import os

def compute_distance_matrix(config):
    calculator_path = config.stations.distance_calculator_path
    call_string = """mvn -f '{calculator_path}' exec:java """.format(**vars())
    os.system(call_string)
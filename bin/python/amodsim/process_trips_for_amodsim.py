
import os

def process_trips_in_agentsim(config):
    preprocessor_path = config.agentpolis.preprocessor_path
    call_string = """mvn -f '{preprocessor_path}' exec:java -Dexec.mainClass='cz.agents.amodsim.PreprocessTrips'""".format(**vars())
    os.system(call_string)
<<<<<<< HEAD

class Agentpolis:
    def __init__(self, properties: dict=None):
        self.show_stacked_entities = properties.get("show_stacked_entities")
        self.show_visio = properties.get("show_visio")
        self.simulation_duration_in_millis = properties.get("simulation_duration_in_millis")
        self.map_nodes_filepath = properties.get("map_nodes_filepath")
        self.map_edges_filepath = properties.get("map_edges_filepath")


        pass

=======

class Agentpolis:
    def __init__(self, properties: dict=None):
        self.map_edges_filepath = properties.get("map_edges_filepath")


        pass

>>>>>>> vga_ridesharing2

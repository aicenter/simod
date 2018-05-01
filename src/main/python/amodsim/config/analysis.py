


class Analysis:

    def __init__(self, properties: dict=None):
        self.chosen_window_start = properties.get("chosen_window_start")
        self.chosen_window_end = properties.get("chosen_window_end")
        self.trips_multiplier = properties.get("trips_multiplier")
        self.ridesharing_off_experiment_path = properties.get("ridesharing_off_experiment_path")
        self.ridesharing_on_experiment_path = properties.get("ridesharing_on_experiment_path")
        self.edge_load_ridesharing_off_filepath = properties.get("edge_load_ridesharing_off_filepath")
        self.edge_load_ridesharing_on_filepath = properties.get("edge_load_ridesharing_on_filepath")
        self.results_ridesharing_off_filepath = properties.get("results_ridesharing_off_filepath")
        self.results_ridesharing_on_filepath = properties.get("results_ridesharing_on_filepath")



        pass


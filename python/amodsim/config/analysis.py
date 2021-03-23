
class Analysis:

    def __init__(self, properties: dict = None):
        self.chosen_window_start = properties.get("chosen_window_start")
        self.chosen_window_end = properties.get("chosen_window_end")
        self.trips_multiplier = properties.get("trips_multiplier")
        pass

from __future__ import print_function, division

from scripts.printer import print_info
from scripts.prague.prepare_trips import get_trips


from scripts.config_loader import config


print_info("PREPARATIONS STARTED")

get_trips(config)






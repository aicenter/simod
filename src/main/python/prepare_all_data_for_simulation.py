from __future__ import print_function, division

from datetime import datetime

def print_info(info):
    print("[{0}]: {1}".format(datetime.now().time(), info))

print_info("PREPARATIONS STARTED")

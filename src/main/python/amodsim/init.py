import os
import sys
import roadmaptools.init

import amodsim.config.amodsim_config


#config = AmodsimConfig()
config = amodsim.config.amodsim_config.config

local_config = sys.argv[1] if len(sys.argv) > 1 else None

# roadmaptools_config = roadmaptools.init.load_config(config, "roadmaptools", local_config,
#                                                    r"C:\Users\User\Desktop\thesis\amod-to-agentpolis_actual\src\main\resources\cz\cvut\fel\aic\amodsim\config\config.cfg")
#"C:\Workspaces\AIC\amod-to-agentpolis\src\main\resources\cz\cvut\fel\aic\amodsim\config/config.cfg")

config = amodsim.config.amodsim_config.config


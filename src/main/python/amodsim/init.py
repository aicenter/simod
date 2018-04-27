import os
import sys
import roadmaptools.init

from amodsim.config.amodsim_config import AmodsimConfig


config = AmodsimConfig()

local_config = sys.argv[1] if len(sys.argv) > 1 else None

roadmaptools_config = roadmaptools.init.load_config(config, "roadmaptools", local_config,
	r"C:\Workspaces\AIC\amod-to-agentpolis\src\main\resources\cz\cvut\fel\aic\amodsim\config/config.cfg")

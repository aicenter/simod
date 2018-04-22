import os
import sys
import roadmaptools.init

from amod.config.amod_config import AmodConfig


config = AmodConfig()

roadmaptools_config = roadmaptools.init.load_config(config, "roadmaptools", sys.argv[1],
	r"C:\Workspaces\AIC\amod-to-agentpolis\src\main\resources\cz\cvut\fel\aic\amodsim\config/config.cfg")

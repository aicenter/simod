#!/bin/bash
PATH=$PATH:/home/$USER/apache-maven-3.6.1/bin/
ml Java
ml Gurobi
mvn compile -U
mvn exec:exec -Dexec.executable=java -Dexec.args="-classpath %classpath -Xmx30g cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation /home/lukesma8/try/amod-to-agentpolis/local_config_files/lukes-RCI.cfg" -Dfile.encoding=UTF-8
#!/bin/bash -x
PATH=$PATH:/home/fiedlda1/apache-maven-3.6.1/bin/
ml Java
ml Gurobi
mvn exec:exec -Dexec.executable=java -Dexec.args="-classpath %classpath -Xmx30g cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation /home/fiedlda1/Amodsim/local_config_files/RCI/ih.cfg" -Dfile.encoding=UTF-8

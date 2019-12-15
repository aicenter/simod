#!/bin/bash -x
PATH=$PATH:/home/kholkolg/apache-maven-3.6.3/bin/
ml Java
ml Gurobi
mvn exec:exec -Dexec.executable=java -Dexec.args='-classpath %classpath -Xmx50g  cz.cvut.fel.aic.amodsim.Taxify /home/kholkolg/amod-to-agentpolis/local_config_files/RCI_olga/test_vga.cfg' -Dfile.encoding=UTF-8


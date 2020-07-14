

## 1. first steps
Go to your/project/folder and  clone all repositories listed below:

 - clone **agentpolis**

```commandline
git clone https://github.com/aicenter/agentpolis.git
```

 - clone **amodsim**

```commandline
git clone https://gitlab.fel.cvut.cz/fiedlda1/amod-to-agentpolis.git
```

 - clone **roadmap_processing**

```commandline
git clone https://github.com/aicenter/roadmap-processing.git
```

## **2. amodsim installation**
Simulation requires gurobi optimizer software
1. download Gurobi Optimizer tar file from [https://www.gurobi.com/downloads/](https://www.gurobi.com/downloads/)

2. untar downloaded file via tar command. location specification is optional. In this example it is /opt directory- 
```commandline
sudo tar xfvz "your downloaded gurobi tar.gz file" -C /opt
```
3. Go to your/gurobi/directory/../lib/ (directory where the **gurobi.jar** file is) and install gurobi via maven:
```commandline
mvn install:install-file -Dfile=gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi -Dversion=1.0 -Dpackaging=jar
```
4. Go to your amod-to-agentpolis directory and compile the project
```commandline
mvn compile
```

## 3. prepare map
1. go to your/project/folder and install roadmaptools:

```commandline
sudo apt-get install -y libspatialindex-dev
```
```commandline
pip3 install roadmap-processing/
```
	
	
2. go to  agentpolis/python/agentpolis directory and create **your_config.cfg** file, which

 **must** include:
``` commandline
data_dir: "/absolute/path/to/this/directory/"
```
**may** include: 
```commandline
map_envelope:
[
    your
    specific
    envelope
    values
]
```

3. run the script with your local config file
```
python3 prepare_map -lc=your_config.cfg
```
4. result is now in ../maps directory

## 4. run the simulation
1. if You face **UnsetisfiedLinkError** You may need to set path to gurobi lib as environment variable
```
LD_LIBRARY_PATH="/path/to/gurobi/lib" 
```

**..next steps yet to be added**






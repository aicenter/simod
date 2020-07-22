This guide is focused on installation on Linux. But any of these instructions can be extrapolated to any OS, just with basic knowledge of the OS.

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
- clone **demand_processing**

	```commandline
	git clone https://github.com/horychtom/demand-processing.git
	```
## **2. amodsim installation**
Simulation requires gurobi optimizer software
1. download Gurobi Optimizer tar file from [https://www.gurobi.com/downloads/](https://www.gurobi.com/downloads/)
	
- follow the instructions on gurobi site. You need to properly license the software as well.
	
2. untar downloaded file via tar command. Location specification is optional. In this example it is /opt  directory
	```
	sudo tar xfvz "your downloaded gurobi tar.gz file" -C /opt
	```
3. Go to your/gurobi/directory/../lib/ (directory where the **gurobi.jar** file is) and install gurobi via maven:
	```
	mvn install:install-file -Dfile=gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi -Dversion=1.0 -Dpackaging=jar
	```
4. Go to your amod-to-agentpolis directory and compile the project
	```
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
	
2. go to  agentpolis/python/agentpolis directory and edit **custom_config.cfg** file, which
	
	 **must** include:
	``` commandline
	data_dir: "/absolute/path/to/your/directory/"
	```
	**may** include specification of the city envelope values, i.e. New York - Manhattan:
	
	```commandline
	map_envelope:
	[
	    40.70
	    -74.06
	    40.82
	    -73.87
	]
```
	
3. run the script with your local config file
	```
	python3 prepare_map -lc=/absolute_path/custom_config.cfg
	```
	
4. result - final map .geojson data,  is now in ../maps directory


## 4. prepare demand
Depending on your project, download trips data (in .csv format) you need.
If you are not sure about the steps, please see readme in demand_processing github repository.

1. Go to demand_processing/ directory and edit config.py file according to your project.
	(in **data_dir** you specify the path to your downloaded data in .csv format)
	
2. run the script:
	 ```
		python3 trips_process.py
	```
	
3. This creates trips.txt in the directory. This file contains demand for the simulation, you need to move it to wherever you store the data for the simulation.

    *check, whether the trip times fit in your simulation time* 

## 5. run the simulation

1. Create your local config file (your_config.cfg) in directory /amod-to-agentpolis\local_config_files/. You can copy the original master config from the project, located in /src/.../amodsim/config/. Here, change amodsim_data_dir line to your directory with data for the simulation. Tweak anything you need. 

	```
	agentpolis:
	{
		#srid: 32618 #Manhattan
		show_stacked_entities: true
		
		visio:
		{
			#launches gui on true
			show_visio: false
		}
		
		map_nodes_filepath: $map_dir + 'nodes.geojson'
		map_edges_filepath: $map_dir + 'edges.geojson'
		
		# length of simulation
		simulation_duration:
		{
			days: 0
			hours: 1
			minutes: 30
			seconds: 0
		}
	
	}
	```
	
	**Important:** In order to get correct map transformations, you have to specify the srid value into the agentpolis:{} block. 
	
	srid is natively set for simulation of Prague. Find what UTM zone your target belongs to:
	[https://mangomap.com/robertyoung/maps/69585/what-utm-zone-am-i-in-#](https://mangomap.com/robertyoung/maps/69585/what-utm-zone-am-i-in-#)
	and then find appropriate srid value (EPSG).
	
2. Create **station_positions.csv**  file.  In format:
  ```
  id_of_the_node,number_of_cars_in_this_station,
  id_of_the_next_node,...
  ```
  You can run **MapVisualizer.java** (with path to the config file) to view the map and nodes so you can design your placement of the stations.

3. **Run** the amodsim **OnDemandVehiclesSimulation.java**  with path/to/your/config as an argument (you may need to set it in your IDE)

   Simulation speed can be adjusted by '+' , '-' , '*' or 'Ctrl *'



## Issues you may encounter

- if You face **UnsetisfiedLinkError** You need to set path to gurobi lib as environment variable. You can do it in your IDE or on OS level:

  Linux:

  ```
  LD_LIBRARY_PATH="/path/to/gurobi/lib"
  ```

  Windows:

  ```
  PATH="/path/to/gurobi/lib"
  ```

  

- If pip has a problem with installing rtree (or any other package), you may need to download it here https://www.lfd.uci.edu/~gohlke/pythonlibs/ and install manually using.

  ```
  pip3 install /downloaded/folder/
  ```

  *choose file according to your System and Python version, i.e. for Python 3.8 on Windows choose Rtree‑0.9.4‑cp38‑cp38‑win32.whl*
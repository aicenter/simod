# Installation

## Clone the necessary projects

Go to workspace folder and  clone all repositories listed below:

 - **Amodsim**: The main project for Mobility-on-Demand (MoD) simulations

	```commandline
	git clone https://gitlab.fel.cvut.cz/fiedlda1/amod-to-agentpolis.git
	```

- **Demand processing**: the project for generating demand from New York taxi data. You can skip it if you have another
 source of input data.

	```commandline
	git clone https://github.com/horychtom/demand-processing.git
	```
- **Agenpolis**: A simulation library, on which the Amodsim project is based. The java part of this project can be 
obtained though maven, but you need to clone it because of the python scripts, which are not yet published in PyPi.

```commandline
git clone https://github.com/aicenter/agentpolis.git
```

Then, before the next version of roadmaptools will be published on PyPi, you have to also clone Roadmaptools, 
a python library for road map processing:

```commandline
git clone https://github.com/aicenter/roadmap-processing.git
```


## Gurobi installation
Simulation requires gurobi optimizer software installed, together with the Java bindings. Follow the instructions on 
gurobi website. You need to download and install the software and register it properly with a license.
After that, try to run `gurobi` in the command line. 
If the installation was successful, you should see a prompt for entering a problem definition.
	
Then, you need to install the java binding to maven.
Go to `<your gurobi directory>/lib/`, there should be a file `gurobi.jar`.
You have to install this file manually to maven, for example by this command line call:

  ```
  mvn install:install-file -Dfile=gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi -Dversion=1.0 -Dpackaging=jar
  ```

Note that in PowerShell, you need to quote all arguments that starts with `-` and contain a dot (`.`) by `'`.

## Amodsim installation

1. Go to your /agentpolis directory and install the project:

  ```
  mvn install
  ```

2. Install agentpolis python package (located in agentpolis directory)
 
  ```
  pip3 install python/
  ``` 

3. Go to your Amodsim (amod-to-agentpolis) directory and compile the project

  ```
  mvn compile
  ```


# Map Preparation

## Manual roadmaptools installation
As long as the last version of Roadmaptools is not published, you have to install it manually from the cloned project:

1. Go to the **cloned** roadmap-processing directory:

2. Install the local (newest) version:

  ```commandline
  pip3 install .
  ```

If installation did not finish successfully, it may be due to rtree library (check the error output). In this case: 

  - On windows you need to install the **rtree** package manualy as precompiled .whl package. 
	Download it [here](https://www.lfd.uci.edu/~gohlke/pythonlibs/#rtree) and choose package version according to 
	your version of Python  *i.e. for Python 3.8 on Windows choose Rtree‑0.9.4‑cp38‑cp38‑win32.whl*.
	Then install downloaded file:

    ```
    pip3 install <path to downloaded .whl package>
    ```

  - On linux, you may also need **libspatialindex library**. Install it with:

    ```
    sudo apt-get install -y libspatialindex-dev
    ```


## Map Generation Configuration
Go to  `.../agentpolis/python/agentpolis` directory and edit (or copy) **custom_config.cfg** file, which must include 
the absolute path to directory when you want to store the maps and similar data:

    ``` commandline
    data_dir: "<absolute path to data directory>"
    ```

And also a specification of the city envelope values, i.e. New York - Manhattan:

    ```commandline
    map_envelope:
    [
        40.70
        -74.06
        40.82
        -73.87
    ]
    ```

## Map Generation
Run the script with your local config file:	

    ```
    python3 prepare_map -lc=<path to custom_config.cfg>
    ```

The `.geojson` map files are now in ../maps directory.


# Prepare demands and stations
First, prepare a **data_folder** and copy your maps in it. 
You may use any folder or the one, where maps already are.

## Create Demand
In this section, we describe how to generate demend data for the simulation from the New York City taxi data. 
For your own demand data, you will need your custom transformation script. 
To process the NYC data:

   1. Download trips data (in .csv format): the csv data of taxis in Manhattan can be downloaded from 
	 [NYC Open Data](https://data.cityofnewyork.us/browse?q=taxi). 
	 Datasets we used are [here](https://data.cityofnewyork.us/dataset/Yellow-Tripdata-2015-January-June/2yzn-sicd)
	 (2.8GB) and [here](https://data.cityofnewyork.us/Transportation/2014-Yellow-Taxi-Trip-Data/gkne-dk5s) (5.8GB).

   2. Go to the cloned Demand processing project (demand_processing/) directory and edit config.py file according to 
	your project.
	Specifically, in `data_dir` specify the path to your downloaded data in `.csv` format)

   3. Run the processing script:

      ```
      python3 trips_process.py
      ```

Finally, you should have a `trips.txt` in the specified data directory. 
This file contains demands for the simulation, you need to move it to **data_folder** from step 1.

   ​	*For additional information see readme in https://github.com/horychtom/demand-processing repository.*


## Create Stations
Stations are nodes on map where the vehicles are parked.
The simulation requires a `station_positions.csv` file that contains two columns: first column that contains the node index where 
the station is located, and the second column is the number of vehicles in the station.

For manual stations placement you can run `MapVisualizer.java` (with path to the local config file) to view the map. 
After ticking "**Show all nodes**" on the right, all nodes ID will be displayed. 


# Simulation


## Local Config file
Create your local config file (your_config.cfg), or reuse the local congig from the map generation step. 
Add amodsim_data_dir line as written below: 

     ```
     amodsim_data_dir: <absolute path to data_folder previous steps>
     ```

Then you can change anything you want in your local config, all changes will overwrite the default config file. 
You can see the original master config for the project, it is located in `/src/main/resources/.../amodsim/config/`. 
Here is an example of data you may want to ajdust.

     ```
     start_time: change to match your data (in milliseconds)
     agentpolis:
     {
     	#srid for New York
     	srid: 32618
     
     	#shows or hides gui
     	visio:
     	{
     		show_visio: false
     	}
     	
     	# length of simulation
     	simulation_duration:
     	{
     		days: 0
     		hours: 6
     		minutes: 30
     		seconds: 0
     	}
     
     }
     ```

	 * *Check, whether the trip times (first value on each row in trips.txt) fit in your simulation time, 
	change **start_time** if not*  

     * *You can see local configurations used by us in /amod-to-agentpolis/local_config_files*

     **Important:** In order to get correct map transformations, you have to specify the srid value into the 
	agentpolis:{} block. Srid is natively set for simulation of Prague. Find what UTM zone your target belongs to  
	here: [https://mangomap.com/utm](https://mangomap.com/robertyoung/maps/69585/what-utm-zone-am-i-in-#)
     and then find relevant srid value (EPSG).

     
## Run the Simulation
Run the amodsim `OnDemandVehiclesSimulation.java`  with `<path to your config>` as an argument (you may need to set 
it in your IDE). Simulation speed can be adjusted by '+' , '-' , '*' or 'Ctrl *' and paused by 'Space'

   - If you face `UnsatisfiedLinkError`, you need to set path to gurobi lib as environment variable.  
	You can do it in your IDE or on OS level:

     Linux:

     ```
     LD_LIBRARY_PATH="/path/to/gurobi/lib"
     ```

     Windows:              *Tip: rather use control panel*

     ```
     IDE: Path=<path/to/gurobi/lib>
     CMD: Path=%Path%;<path/to/gurobi/lib>
     ```

   - Error `ProjectionException:` *Latitude 90°00 N is too close to a pole)* is caused by mistake in generated maps. 
	Double check your srid.

<!--
Copyright (c) 2021 Czech Technical University in Prague.

This file is part of Amodsim project.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Lesser General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public License
along with this program. If not, see <http://www.gnu.org/licenses/>.
-->
# Quick Start Guide

## Prerequisities
- [JDK](https://www.oracle.com/cz/java/technologies/javase-downloads.html)
- [Maven](https://maven.apache.org/)
- [Gurobi](https://www.gurobi.com/products/gurobi-optimizer/) (Including the [Maven support](http://fido.ninja/manuals/add-gurobi-java-interface-maven))
- [Python 3](https://www.python.org/)



## Installation

1. clone Amodsim (this repository)
2. clone the [demand processing repository](https://github.com/aicenter/demand-processing) (for demand generation scripts).
3. Go to your Amodsim directory and compile the project: `mvn compile`
4. In the Amodism dir, install the python package by `pip install ./python`



## Configuration
This project works with configuration files. Most of the config parameters can be left as they are, but you need to configure some of them. 

1. Create a config file. It should have a `cfg` extension and can be located anywhere.
2. Configure the directory location for  maps and similar data: Add the following line to the config file: `data_dir: "<absolute path to data directory>"`



## Map Preparation

### Map Generation Configuration
Our map downloader uses a rectangular boundaries to download the map. First, you need to find coordinates for your area of interest, and then you need to use them in the config file. Below is an example for New York - Manhattan:

    ```commandline
    map_envelope:
    [
        40.70
        -74.06
        40.82
        -73.87
    ]
    ```


### Map Generation
Go to the `<AMODSIM DIR>/python` and run the following command:	

    ```
    python3 create_map_for_ap.py -lc=<path to custom_config.cfg>
    ```

The geojson map files are now in `<data_dir>/maps` directory.



## Prepare Demand and Stations

### Create Demand
In this section, we describe how to generate demend data for the simulation from the New York City taxi data. 
For your own demand data, you will need your custom transformation script. 
To process the NYC data:

   1. Download trips data (in .csv format): the csv data of taxis in Manhattan can be downloaded from 
	 [NYC Open Data](https://data.cityofnewyork.us/browse?q=taxi). 
	 Datasets we used are [here](https://data.cityofnewyork.us/dataset/Yellow-Tripdata-2015-January-June/2yzn-sicd)
	 (2.8GB) and [here](https://data.cityofnewyork.us/Transportation/2014-Yellow-Taxi-Trip-Data/gkne-dk5s) (5.8GB).

   2. Go to the cloned Demand processing directory and edit `config.py` file according to 
	your project.
	Specifically, in `data_dir` specify the path to the folder with the downloaded demand files.

   3. Run the processing script: `python3 trips_process.py`. You should see a `trips.txt` in the specified data directory. This file contains demands for the simulation.
   4. Finally, you need to move the `trips.txt` to <data_dir> specified in the Amodsim config.

   ​	*For additional information see the demand processign readme in https://github.com/horychtom/demand-processing repository.*


### Create Stations
Stations are nodes on map where the vehicles are parked.
The simulation requires a `station_positions.csv` file that contains two columns: first column that contains the node index where 
the station is located, and the second column is the number of vehicles in the station.

For manual stations placement you can run `MapVisualizer.java` (with path to the local config file) to view the map. 
After ticking "**Show all nodes**" on the right, all nodes ID will be displayed. 



## Simulation


### Simulation Configuration
Add amodsim_data_dir line to the config file: `amodsim_data_dir: $data_dir`

Then you can change anything you want in your local config, all changes will overwrite the default config file. 
You can see the original master config for the project, it is located in `/src/main/resources/cz/cvut/fel/aic/amodsim/config/`. 
Here is an example of data you may want to ajdust:

     ```
	 # 6:00 in milliseconds
     start_time:  21600000
	 
     agentpolis:
     {
     	# srid for New York
     	srid: 32618
     	
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

     
### Run the Simulation
Run the amodsim `OnDemandVehiclesSimulation.java`  with `<path to your config>` as an argument:

```
mvn exec:exec -Dexec.executable="java" -Dexec.args="-classpath %%classpath cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation C:/Workspaces/AIC/amod-to-agentpolis/local_config_files/fido-AIC.cfg" -Dfile.encoding=UTF-8
```

**Important:** If running this command from PowerShell, remeber to quote the arguments starting with `-` and containing dot: `'-Dexec.executable="java"'`


Simulation speed can be adjusted by '+' , '-' , '*' or 'Ctrl *' and paused by 'Space'



# FAQ

- When running the simulation, it crashes with `UnsatisfiedLinkError`.
**Solution**: You need to set path to gurobi lib as environment variable: Linux: `LD_LIBRARY_PATH="/path/to/gurobi/lib"`, Windows: `Path=<path/to/gurobi/lib>`

- `ProjectionException: Latitude 90°00 N is too close to a pole)` is caused by mistake in generated maps. Double check your srid.

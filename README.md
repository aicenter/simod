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

## Data
First you need to [download the test data for your first experiment](https://owncloud.cesnet.cz/index.php/s/GnwFj41o73natth). It is a experiment on Manhattan with real historical data from the taxi and limusine commision. Extract the folder anywhere in your computer. It conytains:
- `maps` directory with files `nodes.geojson` and `edges.geojson`.
- `dm.csv`: distance matrix needed for fats travel time computations.
- `station_positions.csv`: file with positions of the parking/refueling stations for the MoD operator.
- `test.cfg`: configuration file.
- `trips.txt`: the file with the demand.


## Requirements
- [JDK](https://www.oracle.com/cz/java/technologies/javase-downloads.html)
- [Python 3](https://www.python.org/)
- [Maven](https://maven.apache.org/)
- [Gurobi](https://www.gurobi.com/products/gurobi-optimizer/) (Including the [Maven support](http://fido.ninja/manuals/add-gurobi-java-interface-maven)!)


## Installation

1. check that you have all the requirements installed, especialy [Maven support for Gurobi](http://fido.ninja/manuals/add-gurobi-java-interface-maven).
2. clone Amodsim (this repository)
3. Go to your Amodsim directory and compile the project: `mvn compile`
4. Extract the downloaded data somewhere.


## Configuration
This project works with configuration files (`test.cfg` in the test data folder). Most of the config parameters can be left as they are, but you need to configure the main data directory. It is on the first line of the config file: `data_dir: "FILL ME"`.
You have to replace *FILL ME*  with the absolute path to the folder with the downloaded data. 

To see all possible options, look into the master config file. For Amodsim, the file is located in `/src/main/resources/cz/cvut/fel/aic/amodsim/config/`.
You can see local configurations used by us in `/amod-to-agentpolis/local_config_files`

     
### Run the Simulation
Run the amodsim `OnDemandVehiclesSimulation.java`  with `<path to your config>` as an argument:

```
mvn exec:java -Dexec.mainClass=cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation -Dexec.args="<path to your config>" -Dfile.encoding=UTF-8
```

**Important:** If running this command from PowerShell, remeber to quote the arguments starting with `-` and containing dot, e.g.: `'-Dexec.mainClass=cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation'`


Simulation speed can be adjusted by '+' , '-' , '*' or 'Ctrl *' and paused by 'Space'




# Usage

## Install Amodsim Python Scripts
For preprocessing data for the simulation, we are using Python scripts. To install them:
1. Upgrade 'pip'
2. In the Amodism dir, install the python package by `pip install ./python`

## Prepare Your Own Demand
In this section, we describe how to generate demend data for the simulation from the New York City taxi data. 
For other demand data, you will need your custom transformation script from to the format we use in our simulation. 

To prepare the NYC data:
1. clone the [demand processing repository](https://github.com/aicenter/demand-processing) (for demand generation scripts).
1. Download trips data (in .csv format): the csv data of taxis in Manhattan can be downloaded from 
 [NYC Open Data](https://data.cityofnewyork.us/browse?q=taxi). 
The dataset we used for the test example above is [here](https://data.cityofnewyork.us/dataset/Yellow-Tripdata-2015-January-June/2yzn-sicd) (2.8GB).
2. Go to the cloned Demand processing directory and edit `config.py` file according to 
your project. 
Specifically, in `data_dir` specify the path to the folder with the downloaded demand `csv` file.
You may also want to change the `output_dir`.
3. Run the processing script: `python trips_process.py`. You should see a `trips.txt` in the specified data directory. This file contains demands for the simulation.
4. Finally, you need to move the `trips.txt` to <data_dir> specified in the Amodsim config.

For additional information see the demand processign readme in https://github.com/horychtom/demand-processing repository.

### Demand Configuration
You should always check whether the trip times (first value on each row in trips.txt) fit in your simulation time window. Specifically:
1. `start_time` (in milliseconds) and
2. `agentpolis.simulation_duration`

Here is an example of how you may want to ajdust these values in your local config:

    
    # 6:00 in milliseconds
    start_time:  21600000
    
    agentpolis:
    {
        
        # length of simulation
        simulation_duration:
        {
            days: 0
            hours: 6
            minutes: 30
            seconds: 0
        }
    
    }


### Create Your Own Stations File
Stations are nodes on map where the vehicles are parked.
The simulation requires a `station_positions.csv` file that contains two columns: first column that contains the node index where the station is located, and the second column is the number of vehicles in the station.

For manual stations placement you can run `MapVisualizer.java` (with path to the local config file) to view the map. 
After ticking "**Show all nodes**" on the right, all nodes ID will be displayed. 

TODO: add a guide for station position generation.


## Prepare Your Own Map

### Map Generation Configuration
Our map downloader uses a rectangular boundaries to download the map. First, you need to find coordinates for your area of interest, and then you need to use them in the Amodsim config file. Below is an example for New York - Manhattan:

    
    map_envelope:
    [
        40.70
        -74.06
        40.82
        -73.87
    ]
    


### Map Generation
Go to the `<AMODSIM DIR>/python/amodsim` and run the following command:	

    
    python create_map_for_ap.py -lc=<path to custom_config.cfg>
    

The geojson map files are now in `<data_dir>/maps` directory.

### Amodism Configuration
For the new map to work in Amodsim you have to set the right SRID (EPSG) for your location, in order to get correct map transformations.
You have to specify the SRID value into the agentpolis:{} block (because it is a configuration of the agentpolis library). SRID is natively set for simulation of Prague. For Manhattan, set SRID is set in the test config to 32618. For other areas, you can find what the corresponding UTM zone here: [https://mangomap.com/utm](https://mangomap.com/robertyoung/maps/69585/what-utm-zone-am-i-in-#) and then find relevant SRID.


### Distance Matrix
TODO: guide how to generate distace matrix.


# FAQ

- When running the simulation, it crashes with `UnsatisfiedLinkError`.
**Solution**: You need to set path to gurobi lib as environment variable: Linux: `LD_LIBRARY_PATH="/path/to/gurobi/lib"`, Windows: `Path=<path/to/gurobi/lib>`

- `ProjectionException: Latitude 90°00 N is too close to a pole)` is caused by mistake in generated maps. Double check your srid.

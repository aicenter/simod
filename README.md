<!--
Copyright (c) 2021 Czech Technical University in Prague.

This file is part of the SiMoD project.

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
SiMoD is a simulation tool for Mobility-on-Demand (MoD) based on [AgentPolis](https://github.com/aicenter/agentpolis) traffic simulation framework developed by the [Smart Mobility group of AI Center, CTU, Prague](https://www.aic.fel.cvut.cz/research-areas/smart-mobility). It lets you build your own MoD system at a location of your choice. It is lightweight, highly customizable and it can run simulations with tens of thousands of vehicles and passengers with ease.

However, customizing SiMoD for your use case requires some programming skills. If you want a more universal and estblished simulation tool check for example [SUMO](https://www.eclipse.org/sumo/).

![SiMoD simulation of MoD system on Manhattan](https://github.com/aicenter/simod/blob/master/simod_showcase.gif?raw=true)

SiMoD simulation of MoD system on Manhattan (Speeded up 5 times)


# Quick Start Guide
This guide shows you how to use the simulation for simulating mobility-on-demand (MoD) with ridesharing in SiMoD. In this experiment travel requests appears and are served by MoD vehicles, so that the total travel distance is minimal.

## Data
First you need to [download the test data for your first experiment](https://owncloud.cesnet.cz/index.php/s/GnwFj41o73natth). It is an experiment on Manhattan with real historical [demand data](https://data.cityofnewyork.us/dataset/Yellow-Tripdata-2015-January-June/2yzn-sicd) from the NYC Taxi and Limusine Commision. Extract the folder anywhere in your computer. It contains:
- `maps` directory with files `nodes.geojson` and `edges.geojson`.
- `dm.csv`: distance matrix needed for fast travel time computations.
- `station_positions.csv`: file with positions of the parking/refueling stations for the MoD operator.
- `test.cfg`: SiMoD configuration file.
- `trips.txt`: the file with the demand.


## Requirements
- [JDK](https://www.oracle.com/cz/java/technologies/javase-downloads.html) (java 8 or newer)
- [Python 3](https://www.python.org/)
- [Maven](https://maven.apache.org/) (Version 3+)
- [Gurobi](https://www.gurobi.com/products/gurobi-optimizer/) (Including the [Maven support](http://fido.ninja/manuals/add-gurobi-java-interface-maven)!)


## Installation

1. check that you have all the requirements installed, especially [Maven support for Gurobi](http://fido.ninja/manuals/add-gurobi-java-interface-maven).
2. clone SiMoD (this repository)
3. Go to your SiMoD directory and compile the project: `mvn compile` (it takes some time to download the dependencies for the first time)
4. Extract the downloaded data somewhere.


## Configuration
This project works with configuration files (`test.cfg` in the test data folder). Most of the config parameters can be left as they are, but you need to configure the main data directory. It is on the first line of the config file: `data_dir: "FILL ME"`.
You have to replace *FILL ME*  with the absolute path to the folder with the downloaded data. Example:
```
data_dir: "C:/experiments/Manhattan/"
```
>**_NOTE_** Do not forgat the slash in the end!

To see all possible options, you can look into the master config file. For SiMoD, the file is located in `/src/main/resources/cz/cvut/fel/aic/SiMoD/config/`. 
But be sure to *not* modify the master config file! 
You can see local configurations used by us in `simod/local_config_files`.

     
### Run the Simulation
Run the SiMoD `OnDemandVehiclesSimulation.java`  with `<path to your config>` as an argument:

```
mvn exec:java -Dexec.mainClass=cz.cvut.fel.aic.simod.OnDemandVehiclesSimulation -Dexec.args="<path to test.cfg>" -Dfile.encoding=UTF-8
```

>**_NOTE_** If running this command from PowerShell, remember to quote the arguments starting with `-` and containing dot, e.g.: `'-Dexec.mainClass=cz.cvut.fel.aic.SiMoD.OnDemandVehiclesSimulation'`

Simulation speed can be adjusted by '+' , '-' , '*' or 'Ctrl *' and paused by 'Space'


## Results
If you let the simulation finish, you should see the reuslt files with various statistics in your experiment folder (inside `data_dir`).


# Result Files
The simulation exports the following files:
- `allEdgesLoadHistory.json`: records of the load of all edges in the road network. It represents the number of vehicles that were on the edge in each time interval.
- `darp_times.csv`: records the totla solver time for each batch. Old file, not used anymore.
- `demand_trip_lengths.csv`: lengths of each passanger trip
- `result.json`: the main result file with the aggregated statistics of the simulation
- `ridesharing.csv`: ridesharing statistics aggregated over batches (one row per batch)
- `service.csv`: request service statistic for each request. The columns are:
    - request announcmnet time
    - request id
    - vehicle id
    - pickup time
    - dropoff time
    - minimum possible service delay (if the request was served immediately by the nearest vehicle)
- `transit.csv` detailed transite statistics for each edge. Each row represent one vehicle transfering over single edge. The columns are:
    - time of the transfer
    - edge id
    - vehicle state:
        - 0: waiting (there should be no transfers with this state)
        - 1: driving to pickup
        - 2: driving to dropoff
        - 3: driving to station
        - 4: rebalancing (only possible if rebalancing is enabled)
- `vehicle_occupancy.csv`: vehicle occupancy for each vehicle in each batch. The columns are:
    - batch id
    - vehicle id
    - occupancy


# Advanced Configuration
In this section, we describe how to more complex features that are not covered in the quick start guide.

## Travel Time Computation
Travel time computation is a crucial part of SiMoD, needed for various tasks including:
- dispatching vehicles,
- rebalancing,
- and simulation of travel.

There are multiple travel time providers available in SiMoD each with a different trade-off between speed, memory usage, ease of setup, and initial loading time. Currently, there are seven travel time providers available in SiMoD:
- AStar
- Astar Cached
- Distance Matrix
- Contraction Hierarchies
- Transit Node Routing
- Transit Node Routing with Arc Flags
- Euclidean

These providers will be described in more detail in the following sections. The general properties of the providers are described in the table below:

| Provider | Precise | Preprocessing by an external tool required | Speed | Memory Requirements |
| --- | --- | --- | --- | --- |
| AStar | yes | no | very slow | low |
| AStar Cached | yes | no | slow | medium |
| Distance Matrix | yes | yes | super fast | very high |
| Contraction Hierarchies | yes | yes | medium | medium |
| Transit Node Routing | yes | yes | fast | high |
| Transit Node Routing with Arc Flags | yes | yes | fast | high |
| Euclidean | no | no | very fast | none |


### Distance Matrix Travel Time Provider
The distance matrix travel time provider is the fastest travel time provider in SiMoD and also fundamentally, there cannot be any faster exact travel time provider. It stores travel times between all pairs of road network nodes in a distance matrix. 

This distance matrix needs to be prepared in advance by an external tool. Two formats of the matrix are supported:
- Comma-separated values (`*.csv`): Text format, slow to load, works out of the box.
- [Hierarchical Data Format](https://en.wikipedia.org/wiki/Hierarchical_Data_Format) (`*.h5`): Binary format, fast to load, but it requires an external native library and a compilation with a dedicated maven profile.

At the and, the amount of memory required for the distance matrix is the same for both formats. The difference is in the loading time, for the `*.csv` format it can be several minutes, while for the `*.h5` format it is just a few seconds.

#### Use the Distance Matrix Travel Time Provider with HDF5 format
The HDF support is provided by the [`HDFQL`](https://www.hdfql.com/) library. To use the HDF5 format, you need to work through the following steps:
1. Prepare the distance matrix in the HDF5 format by an external tool.
1. Download the HDFQL library from the [official website](https://www.hdfql.com/).
2. Add a new environment variable `HDFQL_HOME` with the path to the root directory of the HDFQL library (should be `hdfql-<version>`).
3. Compile the SiMoD project with the `with-hdf5` maven profile.
4. Run the simulation with a library path pointing to the HDFQL library java wrapper: `-Djava.library.path=<HDFQL HOME>/wrapper/java/as/hdfql`.

#### HDFql installation Notes for MacOS:
1. download `HDFql-2.5.0_Darwin64.zip` (Serial) and extract it.
2. install libomp: `brew install libomp`
3. go to lipomp folder (brew info libomp) and copy the file `libomp.dylib` to `/opt/local/lib/libgcc/libgomp.1.dylib` (create folder and rename if needed)
5. open folder from step 1 in terminal and run launch script: `./bin/launch.sh`
6. If a security dialog opens select cancel and go to system settings->Privacy & Security->Security, there should be a blocked item "HDFqlCLI was blocked from use..." click on allow anyway and run the launch script again. repeat the process if other files are also blocked.
7. If it was successfull you should see a message like "HDFqlCLI version 2.5.0 (using HDFql version 2.5.0 64 bit library)" and the command line tool.

#### Note: when using distance matrix you might get an error about heap memory, make sure to increase memory by adding the argunment -Xmx16g to the run configuration or to the command in the terminal

# Usage

## Install SiMoD Python Scripts
For preprocessing data for the simulation, we are using Python scripts. To install them:
1. Upgrade 'pip'
2. In the SiMoD dir, install the python package by `pip install ./python`

## Prepare Your Own Demand
In this section, we describe how to generate demand data for the simulation from the New York City taxi data. 
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
4. Finally, you need to move the `trips.txt` to <data_dir> specified in the SiMoD config.

For additional information see the demand processing readme in https://github.com/horychtom/demand-processing repository.

### Demand Configuration
You should always check whether the trip times (first value on each row in trips.txt) fit in your simulation time window. Specifically:
1. `start_time` (in milliseconds) and
2. `agentpolis.simulation_duration`

Here is an example of how you may want to adjust these values in your local config:

    
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

*This section is incomplete and should present a guide for station position generation.*


## Prepare Your Own Map

### Map Generation Configuration
Our map downloader uses a rectangular boundaries to download the map. First, you need to find coordinates for your area of interest, and then you need to use them in the SiMoD config file. Below is an example for New York - Manhattan:

    
    map_envelope:
    [
        40.70
        -74.06
        40.82
        -73.87
    ]
    


### Map Generation
Go to the `<SiMoD DIR>/python/amodsim` and run the following command:	

    
    python create_map_for_ap.py -lc=<path to custom_config.cfg>
    

The geojson map files are now in `<data_dir>/maps` directory.

### SiMoD Configuration
For the new map to work in SiMoD you have to set the right SRID (EPSG) for your location, in order to get correct map transformations.
You have to specify the SRID value into the agentpolis:{} block (because it is a configuration of the AgentPolis library). SRID is natively set for simulation of Prague. For Manhattan, set SRID is set in the test config to 32618. For other areas, you can find what the corresponding UTM zone here: [https://mangomap.com/utm](https://mangomap.com/robertyoung/maps/69585/what-utm-zone-am-i-in-#) and then find relevant SRID.


### Distance Matrix
*This section is incomplete and should present a guide how to generate distance matrix.*


# FAQ

- When running the simulation, it crashes with `UnsatisfiedLinkError`.
**Solution**: You need to set path to gurobi lib as environment variable: Linux: `LD_LIBRARY_PATH="/path/to/gurobi/lib"`, Windows: `Path=<path/to/gurobi/lib>`

- `ProjectionException: Latitude 90°00 N is too close to a pole)` is caused by mistake in generated maps. Double check your srid.

# Publications
In case you use this software in your research, please consider citing one of our papers using SiMoD:

[1] D. Fiedler, M. Čáp, and M. Čertický, “Impact of mobility-on-demand on traffic congestion: Simulation-based study,” in *2017 IEEE 20th International Conference on Intelligent Transportation Systems (ITSC)*, Oct. 2017, pp. 1–6, doi: [10.1109/ITSC.2017.8317830](https://ieeexplore.ieee.org/document/8317830).

[2] D. Fiedler, M. Čertický, J. Alonso-Mora, and M. Čáp, “The Impact of Ridesharing in Mobility-on-Demand Systems: Simulation Case Study in Prague,” in *2018 21st International Conference on Intelligent Transportation Systems (ITSC)*, Nov. 2018, pp. 1173–1178, doi: [10.1109/ITSC.2018.8569451](https://ieeexplore.ieee.org/document/8569451).


# Project structure

## Java vs Python
The simulation is written in Java. The Python scripts are only for result processing and for input data processing. while all the simulation code should be fully functional, the same cannot be said about Python scripts, many of them are obsolete/non functional.

## Usefull executables
All executables are in the root packegge (`cz.cvut.fel.aic.simod`)

- `OnDemandVehiclesSimulation` Main Executable. It starts the Simulation.
- `MapVisualizer`: For visualizing the map without running the simulation.


## Most Important Packages
This list is not complete. Packages are presented in alphabetical order, without the rouut package prefix (`cz.cvut.fel.aic.simod`).
- `config`: auto-generated package with config classes representing the config file. Do not modify!
- `entity`: Contains agents and other simulation objects specific to SiMoD.
- `event`: Contains SiMoD specific events for the simulation engine.
- `init`: Classes for event generation.
- `rebalancing`: Everything related to rebalancing process, a process that transfer vehicles from areas with lower demand to areas with higher demand.
- `ridesharing`: Ridesharing related stuff.
- `statistics`: Classis for measuring, storing, and exporting simulation data (traveled distance, used vehicles, passenger delays...)
- `traveltimecomputation` package for travel time providers
- `visio`: Contains user interface classes specific to SiMoD.





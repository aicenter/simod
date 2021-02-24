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
# Proccessing amodsim output data



## Output data overview 

After you succesfully finish a simulation, files containing information about it's procces are created. Default folder for these is *'/your_input_data_folder/experiments/test'* but it may be changed in config before the simulation begins (fields **amodsim_experiment_dir** and **experiment_name**). 

Simulation creates following files:



##### logs

* This directory contains standard simulation output **log.txt** 

* and also logs ??? from gurobi optimizer **mip-rebalancing.log**

  

##### on_demand_vehicle_statistics	

*  **drop_off.csv** - values in format [*Simulation Time, Request ID*]  Each line stands for  simulation time [ms], when request with this ID was completed.
* **finish_rebalancing.csv** - 
* **leave_station.csv** - 
* **pickup.csv** - values in format [*Simulation Time, Request ID*] Each line stands for  simulation time [ms], when  request with this ID was started.
* **reach_nearest_station.csv** - 
* **start_rebalancing.csv**  - 



##### trip_caches

- Experiment directory contains one or several trip cache folders, some may be empy. 
- Not neccessary for future use.



##### allEdgesLoadHistory.json

- key-value records of how many times has an edge been loaded



##### darp_times.csv

- empty??



##### demand_trip_lengths.csv

* *currently in progress*



##### result.json

* file containing basic information about executed information such as *averageKmWithPassenger*, *numberOfVehicles*, *demandsCount* etc.



##### ridesharing.csv

* Structure is different according to used method of ridesharing (vga or insertion heuristic), variable names are always stated in the head of file
* **HEURISTIC:** tuple in format [*Batch,New Request Count,Fail Fast Time,Insertion Heuristic Time,Log Fail Time*] on each line
  * **Batch** = number of current iteration (data are not proccessed immediately but in periods, which's lengths are specified in config parameter *batch_size*)
  * **New Request Count** - number of requests, that appeared during the batch interval
  * **Fail Fast Time** - 
  * **Insertion Heuristic Time** - 
  * **Log Fail Time** - Duration of logging the unfeasible requests
* **VGA:** tuple in format [*Batch,New Request Count, Active Request Count, Group Generation Time, Solver Time, Solver gap*], that may be followed by multiple [*SIZE Groups count, SIZE Groups Total Time*] followed by same number of [*SIZE Feasible Groups Count, SIZE Feasible Groups Total Time*]
  * **Active Request Count** - number of waiting requests
  * **SIZE Groups count**  - number of groups of current SIZE, that were created in current batch
  * **SIZE Feasible Groups Count** - only the groups, which's tasks can be fulfilled by agents 



##### service.csv

* tuple in format [*Demand Time, Demand ID, Vehicle ID, Pickup Time, Dropoff Time, Minimal Possible Service Delay*] on each line

  * **Demand Time** - time of demand creation
  * **Minimal Possible Service Delay** - Minimal trip duration in ideal conditions

  

##### transit.csv

* Describing times, when vehicle entered 'new edge' / node
* tuple in format [*Transit Time, Static ID, Vehicle State*] on each line
  * **Transit Time** - Simulation time of entering new edge
  * **Vehicle state** - ordinal od enum OnDemandVehicleState 
    * 0 = WAITING
    * 1 = DRIVING_TO_START_LOCATION
    * 2 = DRIVING_TO_TARGET_LOCATION
    * 3 = DRIVING_TO_STATION
    * 4 = REBALANCING



##### vehicle_occupancy.csv

* tuple in format [*period, vehicle_id, seats_occupied*] on each line
  * period is 1 simulation minute





## Proccessing data using python

Statistics scripts in python needs several prerequisities. Visualization itself uses python library **matplotlib **, which additionally needs **LateX** and **Ghostscript**.

* https://ghostscript.com/download.html

* https://miktex.org/download

*Official matplotlib site regarding this topic: https://matplotlib.org/tutorials/text/usetex.html*


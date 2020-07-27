## **1. First steps**

Go to your/project/folder and  clone all repositories listed below:

 - Clone **agentpolis**

	```commandline
	git clone https://github.com/aicenter/agentpolis.git
	```

 - Clone **amodsim**

	```commandline
	git clone https://gitlab.fel.cvut.cz/fiedlda1/amod-to-agentpolis.git
	```

 - Clone **roadmap_processing**

	```commandline
	git clone https://github.com/aicenter/roadmap-processing.git
	```
- Clone **demand_processing**

	```commandline
	git clone https://github.com/horychtom/demand-processing.git
	```
## **2. Gurobi installation**
Simulation requires gurobi optimizer software
- Download Gurobi Optimizer tar file from [https://www.gurobi.com/downloads/](https://www.gurobi.com/downloads/)

- Follow the instructions on gurobi site. You need to properly license the software as well.
	
- Go to your/gurobi/directory/../lib/ (directory where the **gurobi.jar** file is) and install gurobi via maven:

  ```
  mvn install:install-file -Dfile=gurobi.jar -DgroupId=com.gurobi -DartifactId=gurobi -Dversion=1.0 -Dpackaging=jar
  ```

## **3. Amodsim installation**

1. Go to your /agentpolis directory and install the proejct

  ```
  mvn install
  ```

2. Go to your /amod-to-agentpolis directory and compile the project
  ```
  mvn compile
  ```

## **4. Prepare map**
1. Go to your project folder (directory, where repositories are cloned) and install roadmaptools from **cloned** roadmap-processing package:

  **Important:** '/' is neccessary, pip installer will otherwise use python repository version but we need this local one

  ```commandline
  pip3 install roadmap-processing/
  ```

  If installation did not finnish succesfully, this may help you. 

  - On windows is a need to install **rtree** package manualy as precompiled .whl package. Download it [here](https://www.lfd.uci.edu/~gohlke/pythonlibs/#rtree) and choose package version according to your version of Python  *i.e. for Python 3.8 on Windows choose Rtree‑0.9.4‑cp38‑cp38‑win32.whl*  Then install downloaded file

    ```
    pip3 install <path to downloaded .whl package>
    ```

  - On linux, you may also need libspatialindex library

    ```
    sudo apt-get install -y libspatialindex-dev
    ```

2. Go to  ./agentpolis/python/agentpolis directory and edit **custom_config.cfg** file, which

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

 3. Run the script with your local config file	

    ```
    python3 prepare_map -lc=/absolute/path/to/custom_config.cfg
    ```

    Result of .geojson data is now in ../maps directory


## **5. Prepare demands and stations**
1. ###### Prepare new **data_folder** and copy your maps in it. *You may use any folder or the one, where maps already are.*

2. ###### Create demands

   1. Depending on your project, download trips data (in .csv format) you need. 

      Csv data of taxis in Manhattan can be downloaded from NYC Open Data [here](https://data.cityofnewyork.us/browse?q=taxi)

      ​	*Datasets we used are [here](https://data.cityofnewyork.us/dataset/Yellow-Tripdata-2015-January-June/2yzn-sicd) (2.8GB) and [here](https://data.cityofnewyork.us/Transportation/2014-Yellow-Taxi-Trip-Data/gkne-dk5s) (5.8GB). Differently formated data may need code adjustment*

   2. Go to ./demand_processing/ directory and edit config.py file according to your project.
      (in **data_dir** specify the path to your downloaded data in .csv format)

   3. Run the script

      ```
      python3 trips_process.py
      ```

   ​	This creates **trips.txt** in the directory. This file contains demands for the simulation, you need to move it to **data_folder** from step 1.

   ​	*For additional information see readme in https://github.com/horychtom/demand-processing repository.*

3. ###### Create stations

   Stations are nodes on map containing vehicles, which can fulfill created demands.

   1. Create **station_positions.csv**  file.  In format:

      ```
      id_of_the_node,number_of_cars_in_this_station,
      id_of_the_next_node,...
      ```

      For better stations placement run **MapVisualizer.java** (with path to the config file) to view the map. After ticking "**Show all nodes**" on the right are all nodes ID displayed. 


## **6. Run the simulation**

1. Create your local config file (your_config.cfg). Add amodsim_data_dir line as stated below: 

     ```
     amodsim_data_dir: <absolute path to data_folder from step 5.Prepare demands and stations>
     ```

2. Tweak anything you need. Here is an example of data you may want to ajdust.

     **Important** You can see the original master config from the project, located in /src/main/resources/.../amodsim/config/. 

     * *Check, whether the trip times (first value on each row in trips.txt) fit in your simulation time, change **start_time** if not*  

     * *You can see local configurations used by us in /amod-to-agentpolis/local_config_files*

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

     **Important:** In order to get correct map transformations, you have to specify the srid value into the agentpolis:{} block. 

     Srid is natively set for simulation of Prague. Find what UTM zone your target belongs to  here: [https://mangomap.com/utm](https://mangomap.com/robertyoung/maps/69585/what-utm-zone-am-i-in-#)
     and then find relevant srid value (EPSG).

     

3. **Run** the amodsim **OnDemandVehiclesSimulation.java**  with path/to/your/config as an argument (you may need to set it in your IDE)

   Simulation speed can be adjusted by '+' , '-' , '*' or 'Ctrl *'

   - If you face **UnsatisfiedLinkError**, you need to set path to gurobi lib as environment variable.  You can do it in your IDE or on OS level:

     Linux:

     ```
     LD_LIBRARY_PATH="/path/to/gurobi/lib"
     ```

     Windows:              *Tip: rather use control panel*

     ```
     IDE: Path=<path/to/gurobi/lib>
     CMD: Path=%Path%;<path/to/gurobi/lib>
     ```

   - Error **ProjectionException:** *Latitude 90°00 N is too close to a pole)* is caused by mistake in generated maps. Try  rerunning process from *Prepare map* section. Also, double check your srid.

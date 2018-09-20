### Installation
Install agentpolis from github.
```commandline
cd your/project/folder
git clone https://github.com/aicenter/agentpolis.git
```
or with SSH key:
```commandline
git@github.com:aicenter/agentpolis.git
```
Install amodsim.

```commandline
cd your/project/folder
git clone https://github.com/
```

### Data for simulation
Create directory to keep data for simulation. For example:

 /path/to/folder/amod-data

and two subdirectories

/amod-data/maps

/amod-data/experiments

Parent directory should contain following files:

policy.json


To /amod-data/maps go files with road graph:

edges.geojson

nodes.geojson

Last subfolder is used by amodsim to save simulation results. 


### Cofiguration
Project uses two cofiguration files. Master config is in

/your/folder/src/main/resources/cz/cvut/fel/aic/amodsim/config/config.cfg

Local configuration files are in

/your/folder/local_config_files

Create new configuration file in local_config_files folder (like, my_config.cfg).

Add paths to directory with data for simulation, and for saving the results:

amodsim_data_dir: 'path/to/folder/amod-data/'

trips_filename: 'trips'

trips_file_path: $amodsim_data_dir + $trips_filenamev

amodsim_experiment_dir: $amodsim_data_dir + 'experiments'  + '/'

rebalancing:

{

    policy_file_path: $amodsim_data_dir +'policy.json'
}

In NetBeans open File/Project. First, open Cofigurations, add new configuration. The go to Run, choose configuration, you've just created.In Main class add
cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation
In Arguments path to your local configuration file
/your/project/folder/local_config_files/my_config.cfg
Go back to Configurations, and check if your configuration is activated.










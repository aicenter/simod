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
Create directory to keep data for simulation. For example:</br>
 /path/to/folder/amod-data
and two subdirectories</br>
/amod-data/maps</br>
/amod-data/experiments
Parent directory should contain following files:</br>
policy.json</br>
trips.txt</br>
To /amod-data/maps go files with road graph:</br>
edges.json</br>
nodes.json</br>
Last subfolder is used by amodsim to save simulation results. 


### Cofiguration
Project uses two cofiguration files. Master config is in</br>
/your/folder/src/main/resources/cz/cvut/fel/aic/amodsim/config/config.cfg

Local configuration files are in</br>
/your/folder/local_config_files

Create new configuration file in local_config_files folder (like, my_config.cfg).</br>
Add paths to directory with data for simulation, and for saving the results:</br>
amodsim_data_dir: 'path/to/folder/amod-data/'</br>
trips_filename: 'trips'</br>
trips_file_path: $amodsim_data_dir + $trips_filenamev
amodsim_experiment_dir: $amodsim_data_dir + 'experiments'  + '/'</br>
rebalancing:</br>
{</br>
    policy_file_path: $amodsim_data_dir +'policy.json'</br>
}</br>

In NetBeans open File/Project. First, open Cofigurations, add new configuration. The go to Run, choose configuration, you've just created.In Main class add</br>
cz.cvut.fel.aic.amodsim.OnDemandVehiclesSimulation</br>
In Arguments path to your local configuration file</br>
/your/project/folder/local_config_files/my_config.cfg</br>
Go back to Configurations, and check if your configuration is activated.










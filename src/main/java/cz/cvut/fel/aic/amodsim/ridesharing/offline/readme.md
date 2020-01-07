###All paths, parameters, and evaluated method
###are set in local config file.
You can change config.olga_vga_local.cfg or create your own config file. 
To run evaluation you need nodes.geojson and edges.geojson
files for graph, and .csv file with demand data. 
Final results are saved to experiment_dir, file name eval_result_{timestamp}.csv
###Name of local config file should be passed as an argument.

Main file: OfflineRidesharing.java 

###Datasets:
*nodes.geojson, edges.geojson - road graph used for evaluation

*demand trips2h.csv - 03.08.2015 from 8:00 to 10:00, around 33.000 trips
Up to 1/3 may be filtered out during preprocessing depending on pickup radius.

*demand trips2h.csv - 03.08.2015 from 00:00 to 10:00, around 300.000 trips
Up to 1/3 may be filtered out during preprocessing depending on pickup radius.



### 1) VGA
*ridesharing.offline.OfflineVGASolver 
*ridesharing.vga 

### 2) Chaining
*ridesharing.offline.Demand
*ridesharing.offline.Solution
*ridesharing.search.HopcroftKarp

### 3) Insertion Heuristic
*ridesharing.offline.IHSolver 
*ridesharing.offline.IHSolution








 
	

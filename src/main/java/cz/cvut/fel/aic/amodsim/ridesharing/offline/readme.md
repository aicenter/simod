local config: olga_vga_local.cfg
main file: OfflineRidesharing.java

demand trips2h.csv - 03.08.2015 from 8:00 to 10:00, around 33.000 trips
Up to 1/3 may be filtered out during preprocessing depending on pickup radius.


1. Original solver
ridesharing.offline.vga.OfflineVGASolver 
-uses ridesharing.vga.calculations.GroupGenerator and GurobiSolver
-call to WriteGroupsForDebugging is at the end of method generateGroups.    
 

2. Mine 
ridesharing.offline.vga.MyOfflineVGASolver 
-uses  OfflineGroupGenerator and OfflineGurobiSolver from the same package.
-call to WriteGroupsForDebugging is at the end of method generateGroups. 

To switch solvers (un)comment lines 127-128 in the MainModule.

WriteGroupsForDebugging saves 100 groups of each size from every batch 
to experiment directory name like group_generation_{groupSize}_{timestamp}.csv,

Final results are in experiment_dir, file name eval_result_{timestamp}.csv







 
	

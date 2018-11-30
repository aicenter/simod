### Taxify Competition: Team AIC's Solver

The solution method is implemented in Java 8. The dependencies are managed by Maven 3.3.9. 
To solve a integer linear program that appears as one of the subproblems of the solution approach, we use a mathematical optimization solver Gurobi 8.1.0. Gurobi is a dependency that is not in Maven and needs to be installed from http://www.gurobi.com/. 

Choose an arbitrary base directory. The base directory will be denoted $BASEDIR. 

1. Clone the project/put the source codes to the directory called $BASEDIR/taxify. 

2. The data are expected in a folder $BASEDIR/data.

3. Compile the project using Maven:

$ cd taxify
$ mvn compile

If the build succeeds, you should see:

[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time: 4.159 s
[INFO] Finished at: 2018-11-30T09:45:45+01:00
[INFO] Final Memory: 31M/410M
[INFO] ------------------------------------------------------------------------
 

4. Run the solver main class (cz.cvut.fel.aic.amodsim.Taxify) using maven: 

$ mvn exec:exec

5. The solver will read input data from $BASEDIR/data directory, computes vehicles routes to serve the requests, and stores the log to $BASEDIR/data directory. 



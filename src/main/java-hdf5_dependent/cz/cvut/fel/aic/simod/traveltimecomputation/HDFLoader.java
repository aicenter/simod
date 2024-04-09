package cz.cvut.fel.aic.simod.traveltimecomputation;

//import as.hdfql.HDFql;
import hdf.hdf5lib.*;

public class HDFLoader {

	private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(HDFLoader.class);

	public static int[][] loadDistanceMatrix(String distanceMatrixFilepath) {
//		LOGGER.info("Loading distance matrix from: {}", distanceMatrixFilepath);
//
//		// display HDFql version in use
//		System.out.println("HDFql version: " + HDFql.VERSION);
//
//		// The name of the dataset you want to read
//		String datasetName = "/dm";
//
//		// Open the HDF5 file
//		String openFileCommand = String.format("USE FILE \"%s\"", distanceMatrixFilepath);
//		LOGGER.info("Executing: {}", openFileCommand);
//		HDFql.execute(openFileCommand);
//
//		// Get the dimensions of the dataset (assuming 2D for simplicity)
//		String getDimensionsCommand = String.format("SHOW DIMENSION %s", datasetName);
//		LOGGER.info("Executing: {}", getDimensionsCommand);
//		HDFql.execute("SHOW DIMENSION " + datasetName);
//		HDFql.cursorFirst();
//		int size = HDFql.cursorGetInt(); // Get the number of rows from the cursor
////		int numberOfColumns = HDFql.cursorGetInt(); // Get the number of columns from the cursor
//
//		// Create a two-dimensional Java array based on the dimensions
//		int[][] data = new int[size][size];
//
//		// Register the Java array in HDFql
//		int variableId = HDFql.variableRegister(data);
//		LOGGER.info("Variable data registered with id: {}", variableId);
//
//		// Bind the Java array to the HDF5 dataset and read the data into it
//		LOGGER.info("Executing: SELECT FROM {} INTO MEMORY {}", datasetName, variableId);
//		int ret = HDFql.execute("SELECT FROM " + datasetName + " INTO MEMORY " + HDFql.variableTransientRegister(data));
////		int ret = HDFql.execute("SELECT FROM " + datasetName + " INTO MEMORY " + variableId);
//
//		if(ret < 0) {
//			throw new RuntimeException("Failed to read data from HDF5 file: " + ret);
//		}
//
//		// At this point, 'data' contains the dataset's data
//
//		// Close the HDF5 file
//		HDFql.execute("CLOSE FILE");
//
//		return data;

//		try (HdfFile hdfFile = new HdfFile(Paths.get(distanceMatrixFilepath))) {
//			Dataset dataset = hdfFile.getDatasetByPath("/dm");
//			// data will be a Java array with the dimensions of the HDF5 dataset
//			int[][] data = (int[][]) dataset.getData();
//			return data;
//		}
//		catch (Exception e) {
//			throw new RuntimeException(e);
//		}

		// The name of your dataset within the HDF5 file
		String datasetName = "/dm";
		long fileId = -1;
		long datasetId = -1;
		long dataspaceId = -1;

		try {
			// Open file
			fileId = H5.H5Fopen(distanceMatrixFilepath, HDF5Constants.H5F_ACC_RDONLY, HDF5Constants.H5P_DEFAULT);

			// Open dataset
			datasetId = H5.H5Dopen(fileId, datasetName, HDF5Constants.H5P_DEFAULT);

			// Open dataspace
			dataspaceId = H5.H5Dget_space(datasetId);

			// Get dimensions of the dataset
			long[] dims = new long[2];
			H5.H5Sget_simple_extent_dims(dataspaceId, dims, null);

			// Allocate array for reading the data
			int[][] data = new int[(int)dims[0]][(int)dims[1]];

			// Read the data into the Java array
			if (datasetId >= 0) {
				H5.H5Dread(datasetId, HDF5Constants.H5T_NATIVE_INT, HDF5Constants.H5S_ALL,
					HDF5Constants.H5S_ALL, HDF5Constants.H5P_DEFAULT, data[0]);
			}

			return data;

		} catch (Exception e) {
			throw new RuntimeException(e);
		} finally {
			// Close resources
			try {
				if (dataspaceId >= 0) H5.H5Sclose(dataspaceId);
				if (datasetId >= 0) H5.H5Dclose(datasetId);
				if (fileId >= 0) H5.H5Fclose(fileId);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		}
	}
}

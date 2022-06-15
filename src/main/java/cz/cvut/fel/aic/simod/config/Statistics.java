package cz.cvut.fel.aic.simod.config;

import java.lang.Integer;
import java.lang.String;
import java.util.Map;

public class Statistics {
	public String resultFilePath;

	public String occupanciesFilePath;

	public String occupanciesFileName;

	public String packagesOccupanciesFilePath;

	public String packagesOccupanciesFileName;

	public String noPeopleOccupanciesFileName;

	public String noPeopleOccupanciesFilePath;

	public String peopleOnboardOccupanciesFileName;

	public String peopleOnboardOccupanciesFilePath;

	public String ridesharingFileName;

	public String ridesharingFilePath;

	public Integer statisticIntervalMilis;

	public String groupDataFilePath;

	public String transitStatisticFilePath;

	public String allEdgesLoadHistoryFilePath;

	public OnDemandVehicleStatistic onDemandVehicleStatistic;

	public String resultFileName;

	public Integer allEdgesLoadIntervalMilis;

	public String tripDistancesFilePath;

	public String darpSolverComputationalTimesFilePath;

	public String groupDataFilename;

	public String allEdgesLoadHistoryFileName;

	public String serviceFileName;

	public String serviceFilePath;


	public Statistics(Map statistics) {
		this.resultFilePath = (String) statistics.get("result_file_path");

		this.occupanciesFilePath = (String) statistics.get("occupancies_file_path");
		this.occupanciesFileName = (String) statistics.get("occupancies_file_name");
		this.packagesOccupanciesFilePath = (String) statistics.get("packages_occupancies_file_path");
		this.packagesOccupanciesFileName = (String) statistics.get("packages_occupancies_file_name");

		this.noPeopleOccupanciesFileName = (String) statistics.get("no_people_occupancies_file_name");
		this.noPeopleOccupanciesFilePath = (String) statistics.get("no_people_occupancies_file_path");
		this.peopleOnboardOccupanciesFileName = (String) statistics.get("people_onboard_occupancies_file_name");
		this.peopleOnboardOccupanciesFilePath = (String) statistics.get("people_onboard_occupancies_file_path");

		this.ridesharingFileName = (String) statistics.get("ridesharing_file_name");
		this.ridesharingFilePath = (String) statistics.get("ridesharing_file_path");
		this.statisticIntervalMilis = (Integer) statistics.get("statistic_interval_milis");
		this.groupDataFilePath = (String) statistics.get("group_data_file_path");
		this.transitStatisticFilePath = (String) statistics.get("transit_statistic_file_path");
		this.allEdgesLoadHistoryFilePath = (String) statistics.get("all_edges_load_history_file_path");
		this.onDemandVehicleStatistic = new OnDemandVehicleStatistic((Map) statistics.get("on_demand_vehicle_statistic"));
		this.resultFileName = (String) statistics.get("result_file_name");
		this.allEdgesLoadIntervalMilis = (Integer) statistics.get("all_edges_load_interval_milis");
		this.tripDistancesFilePath = (String) statistics.get("trip_distances_file_path");
		this.darpSolverComputationalTimesFilePath = (String) statistics.get("darp_solver_computational_times_file_path");
		this.groupDataFilename = (String) statistics.get("group_data_filename");
		this.allEdgesLoadHistoryFileName = (String) statistics.get("all_edges_load_history_file_name");
		this.serviceFileName = (String) statistics.get("service_file_name");
		this.serviceFilePath = (String) statistics.get("service_file_path");
	}
}

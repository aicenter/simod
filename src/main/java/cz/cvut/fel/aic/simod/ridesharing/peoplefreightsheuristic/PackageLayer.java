package cz.cvut.fel.aic.simod.ridesharing.peoplefreightsheuristic;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.agentpolis.config.AgentpolisConfig;
import cz.cvut.fel.aic.agentpolis.simmodel.entity.vehicle.Vehicle;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.ClickableEntityLayer;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioUtils;
import cz.cvut.fel.aic.alite.vis.Vis;
import cz.cvut.fel.aic.simod.entity.DemandAgent;
import cz.cvut.fel.aic.simod.entity.DemandAgentState;
import cz.cvut.fel.aic.simod.storage.DemandStorage;

import javax.vecmath.Point2d;
import java.awt.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
public class PackageLayer extends ClickableEntityLayer<DemandPackage> {

	private static final Color DEMAND_COLOR =  new Color(56, 227, 0);

	private static final Color DROPPED_COLOR = new Color(22, 98, 0);

	private static final int SIZE = 1;

	private static final int TRANSFORMABLE_SIZE = 4;

	private static final Double TEXT_MARGIN_BOTTOM = 5.0;

	private static final Color TEXT_BACKGROUND_COLOR = Color.WHITE;


	protected final Set<DemandPackage> demandsWithPrintedInfo;


	@Inject
	public PackageLayer(DemandPackageStorage packageStorage, AgentpolisConfig agentpolisConfig) {
		super(packageStorage, agentpolisConfig);
		demandsWithPrintedInfo = new HashSet<>();
	}


	@Override
	public void init(Vis vis) {
		super.init(vis);
		vis.addMouseListener(this);
	}

	protected Point2d getDrivingPackagePosition(DemandPackage demandPackage) {
		return positionUtil.getCanvasPositionInterpolatedForVehicle(demandPackage.getTransportingEntity());
	}

	protected Point2d getDrivingPackagePositionInTime(DemandPackage demandPackage, long time) {
		return positionUtil.getCanvasPositionInterpolatedForVehicleInTime(demandPackage.getTransportingEntity(), time);
	}

	protected Point2d getWaitingPackagePosition(DemandPackage demandPackage) {
		return positionUtil.getCanvasPosition(demandPackage.getPosition());
	}

	@Override
	protected Point2d getEntityPosition(DemandPackage demandPackage) {
		if (demandPackage.getState() == DemandAgentState.DRIVING) {
			return getDrivingPackagePosition(demandPackage);
		}
		else {
			return getWaitingPackagePosition(demandPackage);
		}
	}

	@Override
	protected Point2d getEntityPositionInTime(DemandPackage demandPackage, long time) {
		if (demandPackage.getState() == DemandAgentState.DRIVING) {
			return getDrivingPackagePositionInTime(demandPackage, time);
		}
		else {
			return getWaitingPackagePosition(demandPackage);
		}
	}

	@Override
	protected Color getEntityDrawColor(DemandPackage demandPackage) {
		if (demandPackage.isDropped()) {
			return DROPPED_COLOR;
		}

		return DEMAND_COLOR;
	}

	@Override
	protected int getEntityTransformableRadius(DemandPackage demandPackage) {
		return SIZE;
	}

	@Override
	protected double getEntityStaticRadius(DemandPackage demandPackage) {
		return SIZE;
	}

	@Override
	protected void processClick(DemandPackage nearestEntity) {
//		if(demandsWithPrintedInfo.contains(nearestEntity)){
//			demandsWithPrintedInfo.remove(nearestEntity);
//		}
//		else{
//			demandsWithPrintedInfo.add(nearestEntity);
//		}
	}

	@Override
	protected void drawEntities(List<DemandPackage> demandPackages, Point2d entityPosition, Graphics2D canvas,
								Dimension dim) {
		super.drawEntities(demandPackages, entityPosition, canvas, dim);
		if (demandsWithPrintedInfo.contains(demandPackages.get(0))) {
			double radius = getRadius(demandPackages.get(0));

			int x1 = (int) (entityPosition.getX() - radius);
			int y1 = (int) (entityPosition.getY() - radius);
			int x2 = (int) (entityPosition.getX() + radius);
			int y2 = (int) (entityPosition.getY() + radius);
			if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {

				VisioUtils.printTextWithBackgroud(canvas, demandPackages.get(0).getId(),
						new Point((int) (x1 + TEXT_MARGIN_BOTTOM), y1 + (y2 - y1) / 2), Color.BLACK,
						TEXT_BACKGROUND_COLOR);
			}
		}
	}

}

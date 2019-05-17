package cz.cvut.fel.aic.amodsim.visio;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import cz.cvut.fel.aic.amodsim.entity.vehicle.OnDemandVehicle;
import cz.cvut.fel.aic.amodsim.storage.OnDemandVehicleStorage;
import cz.cvut.fel.aic.agentpolis.simulator.visualization.visio.VisioPositionUtil;
import cz.cvut.fel.aic.geographtools.Node;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import javax.vecmath.Point2d;

/**
 *
 * @author F.I.D.O.
 */
@Singleton
public class OnDemandVehiclePlanLayer extends PlanLayer<OnDemandVehicle>{
	
	private static final Color COLOR = Color.CYAN;
    
    private static final int SIZE = 3;
	
	@Inject
	public OnDemandVehiclePlanLayer(OnDemandVehicleStorage entityStorage, VisioPositionUtil positionUtil) {
		super(entityStorage, positionUtil);
	}

	@Override
	protected void drawTrip(Graphics2D canvas, Dimension dim, Rectangle2D drawingRectangle, OnDemandVehicle entity) {
		super.drawTrip(canvas, dim, drawingRectangle, entity); 
		
		Node demandTarget = entity.getDemandTarget();
        if(demandTarget != null){
            Point2d demandTargetPosition = positionUtil.getCanvasPosition(demandTarget);

            canvas.setColor(COLOR);
            int radius = SIZE;
            int width = radius * 2;

            int x1 = (int) (demandTargetPosition.getX() - radius);
            int y1 = (int) (demandTargetPosition.getY() - radius);
            int x2 = (int) (demandTargetPosition.getX() + radius);
            int y2 = (int) (demandTargetPosition.getY() + radius);
            if (x2 > 0 && x1 < dim.width && y2 > 0 && y1 < dim.height) {
                canvas.fillOval(x1, y1, width, width);
            }

            canvas.setColor(PlanLayer.TRIP_COLOR);
        }
	}
	
	
	
}

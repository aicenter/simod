/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of Amodsim project.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package cz.cvut.fel.aic.amodsim.ridesharing.vga.calculations;

import cz.cvut.fel.aic.amodsim.traveltimecomputation.TravelTimeProvider;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class MathUtils {

	private static TravelTimeProvider travelTimeProvider = null;
	
	

	public static double round(double value, int places) {
		if (places < 0) throw new IllegalArgumentException();

		BigDecimal bd = new BigDecimal(value);
		bd = bd.setScale(places, RoundingMode.HALF_UP);
		return bd.doubleValue();
	}

	private MathUtils() {}

	public static void setTravelTimeProvider(TravelTimeProvider travelTimeProvider) {
		MathUtils.travelTimeProvider = travelTimeProvider;
	}

	public static TravelTimeProvider getTravelTimeProvider() {
		return travelTimeProvider;
	}

}

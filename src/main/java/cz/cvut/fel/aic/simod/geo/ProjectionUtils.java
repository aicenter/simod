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
package cz.cvut.fel.aic.simod.geo;

/**
 *
 * @author fido
 */
//public class ProjectionUtils {
//	public static int utmToSrid(GPSLocation gpsLocation){
//		
//		int baseSrid;
//	
//		if(gpsLocation.getLatitude() < 0){
//			// south hemisphere
//			baseSrid = 32700;
//		}
//		else{
//			// north hemisphere or on equator
//			baseSrid = 32600;
//		}
//
//		baseSrid = (int) (baseSrid + Math.floor((gpsLocation.getLongitude() + 186) / 6));
//		if(gpsLocation.getLongitude() == 180){
//				 out_srid := base_srid + 60;
//		}
//
//		 --- TODO: consider special cases around norway etc.
//		return baseSrid;
//	}
//}

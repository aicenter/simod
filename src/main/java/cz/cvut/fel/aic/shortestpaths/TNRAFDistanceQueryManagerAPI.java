/*
 * Copyright (c) 2021 Czech Technical University in Prague.
 *
 * This file is part of the SiMoD project.
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

/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.8
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package cz.cvut.fel.aic.shortestpaths;

public class TNRAFDistanceQueryManagerAPI {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected TNRAFDistanceQueryManagerAPI(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TNRAFDistanceQueryManagerAPI obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        shortestPathsJNI.delete_TNRAFDistanceQueryManagerAPI(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void initializeTNRAF(String tnrafFile, String mappingFile) {
    shortestPathsJNI.TNRAFDistanceQueryManagerAPI_initializeTNRAF(swigCPtr, this, tnrafFile, mappingFile);
  }

  public long distanceQuery(java.math.BigInteger start, java.math.BigInteger goal) {
    return shortestPathsJNI.TNRAFDistanceQueryManagerAPI_distanceQuery(swigCPtr, this, start, goal);
  }

  public void clearStructures() {
    shortestPathsJNI.TNRAFDistanceQueryManagerAPI_clearStructures(swigCPtr, this);
  }

  public TNRAFDistanceQueryManagerAPI() {
    this(shortestPathsJNI.new_TNRAFDistanceQueryManagerAPI(), true);
  }

}

/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.8
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package cz.cvut.fel.aic.shortestpaths;

public class TNRDistanceQueryManagerAPI {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected TNRDistanceQueryManagerAPI(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(TNRDistanceQueryManagerAPI obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        shortestPathsJNI.delete_TNRDistanceQueryManagerAPI(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void initializeTNR(String tnrFile, String mappingFile) {
    shortestPathsJNI.TNRDistanceQueryManagerAPI_initializeTNR(swigCPtr, this, tnrFile, mappingFile);
  }

  public long distanceQuery(java.math.BigInteger source, java.math.BigInteger target) {
    return shortestPathsJNI.TNRDistanceQueryManagerAPI_distanceQuery(swigCPtr, this, source, target);
  }

  public void clearStructures() {
    shortestPathsJNI.TNRDistanceQueryManagerAPI_clearStructures(swigCPtr, this);
  }

  public TNRDistanceQueryManagerAPI() {
    this(shortestPathsJNI.new_TNRDistanceQueryManagerAPI(), true);
  }

}

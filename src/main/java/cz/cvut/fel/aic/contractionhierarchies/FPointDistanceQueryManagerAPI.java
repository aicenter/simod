/* ----------------------------------------------------------------------------
 * This file was automatically generated by SWIG (http://www.swig.org).
 * Version 3.0.8
 *
 * Do not make changes to this file unless you know what you are doing--modify
 * the SWIG interface file instead.
 * ----------------------------------------------------------------------------- */

package cz.cvut.fel.aic.contractionhierarchies;

public class FPointDistanceQueryManagerAPI {
  private transient long swigCPtr;
  protected transient boolean swigCMemOwn;

  protected FPointDistanceQueryManagerAPI(long cPtr, boolean cMemoryOwn) {
    swigCMemOwn = cMemoryOwn;
    swigCPtr = cPtr;
  }

  protected static long getCPtr(FPointDistanceQueryManagerAPI obj) {
    return (obj == null) ? 0 : obj.swigCPtr;
  }

  protected void finalize() {
    delete();
  }

  public synchronized void delete() {
    if (swigCPtr != 0) {
      if (swigCMemOwn) {
        swigCMemOwn = false;
        contractionHierarchiesJNI.delete_FPointDistanceQueryManagerAPI(swigCPtr);
      }
      swigCPtr = 0;
    }
  }

  public void initializeCH(String chFile) {
    contractionHierarchiesJNI.FPointDistanceQueryManagerAPI_initializeCH(swigCPtr, this, chFile);
  }

  public double distanceQuery(long source, long target) {
    return contractionHierarchiesJNI.FPointDistanceQueryManagerAPI_distanceQuery(swigCPtr, this, source, target);
  }

  public void clearStructures() {
    contractionHierarchiesJNI.FPointDistanceQueryManagerAPI_clearStructures(swigCPtr, this);
  }

  public FPointDistanceQueryManagerAPI() {
    this(contractionHierarchiesJNI.new_FPointDistanceQueryManagerAPI(), true);
  }

}

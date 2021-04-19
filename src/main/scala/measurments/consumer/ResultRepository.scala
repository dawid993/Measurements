package measurments.consumer

import measurments.result.MeasurementResult

import scala.collection.mutable.ArrayBuffer

trait ResultRepository {
  def save(results : ArrayBuffer[MeasurementResult])
  def getByDeviceId(deviceId : String) : List[MeasurementResult]
  def getByTarget(target : String) : List[MeasurementResult]
}

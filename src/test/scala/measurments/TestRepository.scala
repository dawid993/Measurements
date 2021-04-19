package measurments

import measurments.consumer.ResultRepository
import measurments.result.MeasurementResult

import scala.collection.mutable.ArrayBuffer

class TestRepository extends ResultRepository {
  private val inMemoryStorage = new ArrayBuffer[MeasurementResult]

  override def save(results: ArrayBuffer[MeasurementResult]): Unit = {
    this.synchronized {
      inMemoryStorage ++= results
    }
  }

  override def getByDeviceId(deviceId: String): List[MeasurementResult] = {
    this.synchronized {
      inMemoryStorage.filter(result => result.deviceId == deviceId).toList
    }
  }

  override def getByTarget(target: String): List[MeasurementResult] = {
    this.synchronized {
      inMemoryStorage.filter(result => result.target == target).toList
    }
  }
}
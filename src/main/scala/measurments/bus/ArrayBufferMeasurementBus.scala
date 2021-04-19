package measurments.bus

import measurments.result.MeasurementResult

import scala.collection.mutable.ArrayBuffer

class ArrayBufferMeasurementBus(val busSize: Int) extends MeasurementBus {
  private val measurementsContainer = new ArrayBuffer[MeasurementResult](busSize)

  private def loadInitials(measurementResults: List[MeasurementResult]): Unit = {
    measurementsContainer ++= measurementResults
  }

  override def put(result: MeasurementResult): Unit = this.synchronized {
    measurementsContainer += result
  }

  override def get(): Option[MeasurementResult] = this.synchronized {
    if (measurementsContainer.isEmpty) {
      return None
    }

    val headElement = measurementsContainer.head
    measurementsContainer.remove(0)
    Some(headElement)
  }

  def getMeasurementsNumber : Int = measurementsContainer.size

  def getMeasurements : ArrayBuffer[MeasurementResult] = measurementsContainer.clone
}

object ArrayBufferMeasurementBus {
  private val DefaultBusSize = 100

  def apply(busSize: Int): ArrayBufferMeasurementBus = new ArrayBufferMeasurementBus(busSize)

  def apply(): ArrayBufferMeasurementBus = new ArrayBufferMeasurementBus(DefaultBusSize)

  def apply(measurementResult: List[MeasurementResult]): ArrayBufferMeasurementBus = {
    val bus = this.apply(DefaultBusSize)
    bus.loadInitials(measurementResult)
    bus
  }
}

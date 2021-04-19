package measurments.bus

import measurments.result.MeasurementResult

trait MeasurementBus {
  def put(result : MeasurementResult)
  def get() : Option[MeasurementResult]
}

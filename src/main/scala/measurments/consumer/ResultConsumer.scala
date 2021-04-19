package measurments.consumer

import measurments.bus.MeasurementBus
import measurments.result.MeasurementResult

import scala.collection.mutable.ArrayBuffer

class ResultConsumer(val bus: MeasurementBus, val resultRepository: ResultRepository, val bufferSize: Int) extends Thread {
  private val resultBuffer = new ArrayBuffer[MeasurementResult](bufferSize)

  private var enabled = true
  private[this] var _operationsDone = false

  def operationsDone: Boolean = _operationsDone

  override def run() {
    while (enabled) {
      reachToEventBus
    }

    this.synchronized {
      saveQueue
    }

    _operationsDone = true
  }

  private def reachToEventBus: Unit = {
    val result = bus.get();
    if (result.isDefined) {
      this.synchronized {
        addToQueue(result.get)
        if (resultBuffer.size >= bufferSize) {
          saveQueue
        }
      }
    }
  }

  def disable(): Unit = enabled = false

  private def saveQueue: Unit = {
    if (!resultBuffer.isEmpty) {
      resultRepository.save(resultBuffer)
      resultBuffer.clearAndShrink(bufferSize)
    }
  }

  private def addToQueue(result: MeasurementResult): Unit = resultBuffer += result
}

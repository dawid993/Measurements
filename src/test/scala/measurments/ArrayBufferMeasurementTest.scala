package measurments

import measurments.bus.ArrayBufferMeasurementBus
import measurments.result.MeasurementResult
import org.awaitility.Awaitility.await
import org.scalatest.FunSuite

import java.time.Duration

class ArrayBufferMeasurementTest extends FunSuite {
  private val DeviceId1 = "0001d"
  private val DeviceId2 = "0001e"
  private val TargetId1 = "0001t"

  test("should return 0 as number of measurements")({
    val bus = ArrayBufferMeasurementBus()
    assert(bus.getMeasurementsNumber == 0)
  })

  test("should return 0 as number of measurements - v2")({
    val bus = ArrayBufferMeasurementBus(100)
    assert(bus.getMeasurementsNumber == 0)
  })

  test("should return 1 as number of measurements")({
    val measurement = MeasurementResult(DeviceId1, TargetId1, 12.0)
    val bus = ArrayBufferMeasurementBus(List[MeasurementResult](
      measurement
    ))

    assert(bus.getMeasurementsNumber == 1)
    val measurementsCopy = bus.getMeasurements
    assert(measurementsCopy.find(_.deviceId == DeviceId1) match {
      case Some(result) => result == measurement
      case None => false
    })
  })

  test("should add results to bus")({
    val measurement = MeasurementResult(DeviceId1, TargetId1, 12.0)
    val bus = ArrayBufferMeasurementBus()

    val measurementsNumber = 100
    for(i <- 1 to measurementsNumber) {
      bus.put(measurement)
    }

    assert(bus.getMeasurementsNumber == measurementsNumber)
    assert(bus.getMeasurements.filter(_.deviceId == DeviceId1).size == measurementsNumber)
  })

  test("should add results to bus - concurrent")({
    val measurement1 = MeasurementResult(DeviceId1, TargetId1, 12.0)
    val measurement2 = MeasurementResult(DeviceId2, TargetId1, 12.0)

    val bus = ArrayBufferMeasurementBus()

    val measurementsNumber = 1000

    new Thread(() => {
      for (i <- 1 to measurementsNumber) {
        bus.put(measurement1)
      }
    }).start()

    new Thread(() => {
      for (i <- 1 to measurementsNumber) {
        bus.put(measurement2)
      }
    }).start()

    await().atMost(Duration.ofSeconds(5)).until(() => {
      bus.getMeasurementsNumber == 2 * measurementsNumber
    })

    await().atMost(Duration.ofSeconds(5)).until(() => {
      bus.getMeasurements.filter(_.deviceId == DeviceId1).size == measurementsNumber
    })

    await().atMost(Duration.ofSeconds(5)).until(() => {
      bus.getMeasurements.filter(_.deviceId == DeviceId2).size == measurementsNumber
    })
  })
}

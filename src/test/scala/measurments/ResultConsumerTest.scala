package measurments

import measurments.bus.{ArrayBufferMeasurementBus, MeasurementBus}
import measurments.consumer.ResultConsumer
import measurments.result.MeasurementResult
import org.awaitility.Awaitility._
import org.scalatest.FunSuite

import java.time.Duration
import java.util.concurrent.TimeUnit

class ResultConsumerTest extends FunSuite {
  private val TestBufferSize = 2
  private val DeviceId1 = "0001d"
  private val DeviceId2 = "0001e"
  private val TargetId1 = "0001t"

  test("should save measurement result when result consumer disabled")({
    val repository: TestRepository = new TestRepository
    val resultConsumer: ResultConsumer = createConsumer(
      createEventBus(List[MeasurementResult](
        MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1)
      )),
      repository
    )

    new Thread(resultConsumer).start
    TimeUnit.MILLISECONDS.sleep(100)
    resultConsumer.disable()

    await().atMost(Duration.ofSeconds(5)).until(() => resultConsumer.operationsDone)
    assert(repository.getByDeviceId(DeviceId1).size == 1)
  })

  test("should save measurement result when buffer size exceeded")({
    val repository: TestRepository = new TestRepository
    val resultConsumer: ResultConsumer = createConsumer(
      createEventBus(List[MeasurementResult](
        MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1),
        MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1)
      )),
      repository
    )

    new Thread(resultConsumer).start
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId1).size == 2)
  })

  test("should save measurements result when consecutive measurement added")({
    val repository: TestRepository = new TestRepository
    val eventBus = createEventBus(List[MeasurementResult](
      MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1),
      MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1)
    ))

    val resultConsumer: ResultConsumer = createConsumer(eventBus, repository, 3)

    new Thread(resultConsumer).start
    eventBus.put(MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1))
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId1).size == 3)
  })

  test("should save measurements result when consecutive measurement added - v2")({
    val repository: TestRepository = new TestRepository
    val eventBus = createEventBus(List[MeasurementResult](
      MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1),
      MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1)
    ))

    val resultConsumer: ResultConsumer = createConsumer(eventBus, repository, 3)

    new Thread(resultConsumer).start
    eventBus.put(MeasurementResult(degrees = 12.0, deviceId = DeviceId2, target = TargetId1))
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId1).size == 2)
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId2).size == 1)
  })

  test("should save measurements results when concurrent producents are running")({
    val repository: TestRepository = new TestRepository
    val eventBus = createEventBus()

    val resultConsumer: ResultConsumer = createConsumer(eventBus, repository, 10)

    new Thread(resultConsumer).start
    val measurementsNumber = 100

    runProducent(measurementsNumber, eventBus, MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1)).start()
    runProducent(measurementsNumber, eventBus, MeasurementResult(degrees = 12.0, deviceId = DeviceId2, target = TargetId1)).start()

    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId1).size == measurementsNumber)
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId2).size == measurementsNumber)
  })

  test("should save measurements results when concurrent producents are running - v2")({
    val repository: TestRepository = new TestRepository
    val eventBus = createEventBus()
    val resultConsumer: ResultConsumer = createConsumer(eventBus, repository, 10)

    new Thread(resultConsumer).start
    val measurementsNumber = 100
    val repeatNumber = 100;

    for (i <- 1 to repeatNumber) {
      runProducent(measurementsNumber, eventBus, MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1)).start()
      runProducent(measurementsNumber, eventBus, MeasurementResult(degrees = 12.0, deviceId = DeviceId2, target = TargetId1)).start()
    }

    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId1).size == repeatNumber * measurementsNumber)
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId2).size == repeatNumber * measurementsNumber)
  })

  test("should save measurements results when concurrent consumers are running - v2")({
    val repository: TestRepository = new TestRepository
    val eventBus = createEventBus()
    val resultConsumer: ResultConsumer = createConsumer(eventBus, repository, 10)
    val consumersNumber = 20
    val measurementsNumber = 1000
    val repeatNumber = 100;

    for (i <- 1 to repeatNumber) {
      runProducent(measurementsNumber, eventBus, MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1)).start()
      runProducent(measurementsNumber, eventBus, MeasurementResult(degrees = 12.0, deviceId = DeviceId2, target = TargetId1)).start()
    }

    for (i <- 1 to consumersNumber) {
      new Thread(resultConsumer).start
    }

    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId1).size == repeatNumber * measurementsNumber)
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId2).size == repeatNumber * measurementsNumber)
  })

  test("should save measurements results when last free buffer slot is inserted")({
    val repository: TestRepository = new TestRepository
    val eventBus = createEventBus(List[MeasurementResult](MeasurementResult(degrees = 12.0, deviceId = DeviceId1, target = TargetId1)))
    val resultConsumer: ResultConsumer = createConsumer(eventBus, repository, 2)

    new Thread(resultConsumer).start
    eventBus.put(MeasurementResult(degrees = 12.0, deviceId = DeviceId2, target = TargetId1))
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId1).size == 1)
    await().atMost(Duration.ofSeconds(5)).until(() => repository.getByDeviceId(DeviceId1).size == 1)

  })

  private def runProducent(measurmentsNumber: Int, eventBus: MeasurementBus, result: MeasurementResult) = {
    new Thread(() => {
      for (i <- 1 to measurmentsNumber) {
        eventBus.put(result)
      }
    })
  }

  private def createConsumer(eventBus: MeasurementBus, repository: TestRepository, bufferSize: Int = TestBufferSize) = {
    val resultConsumer: ResultConsumer = new ResultConsumer(
      bus = eventBus,
      resultRepository = repository,
      bufferSize
    )
    resultConsumer
  }

  private def createEventBus(measurments: List[MeasurementResult] = List[MeasurementResult]()): ArrayBufferMeasurementBus = {
    ArrayBufferMeasurementBus(measurments)
  }
}

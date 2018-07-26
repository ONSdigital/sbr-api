package tracing

import com.typesafe.scalalogging.LazyLogging
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.time.{ Millis, Seconds }
import org.scalatest.{ FreeSpec, Matchers }
import org.slf4j.MDC

import scala.concurrent.{ ExecutionContextExecutor, Future }

/*
 * This spec:
 * - uses the test thread to put a value in the mapped diagnostic context
 * - kicks off a computation that involves evaluating a number of futures.  These futures will be executed by the
 *   ForkJoinPool - and we assert that the parent context from the test thread is migrated into each thread from
 *   the pool that is used to execute a computation.
 *
 * As a by-product the computation calculates a factorial, but that is a by-product and is not the purpose of the
 * test.
 *
 * Note that there is no guarantee that different threads from the pool will be used to complete the calculation.
 * We therefore execute the test many times in the hope that this situation will occur - ensuring that a parent
 * context was migrated into multiple pool threads.  This is just a hope however - the very nature of concurrency
 * means that this is non-deterministic.
 *
 * If the executionContext used by the fixture is swapped out for the default Scala or Play execution contexts,
 * this test should fail.
 */
class TracingExecutionContextSpec extends FreeSpec with Matchers with ScalaFutures with LazyLogging {

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(org.scalatest.time.Span(5, Seconds)),
    interval = scaled(org.scalatest.time.Span(100, Millis))
  )

  private trait Fixture {
    implicit val executionContext: ExecutionContextExecutor = new TracingExecutionContext(scala.concurrent.ExecutionContext.Implicits.global)

    val ContextKey = "SomeKey"
    private val StartChainValue = 1
    private val EndChainValue = 40320 // factorial of 8 (8!)

    def testContextMigration(): Unit = {
      val contextValue = newContextValue
      logger.trace(s"Setting MDC value to [$contextValue] in thread [${Thread.currentThread().getName}]")
      MDC.put(ContextKey, contextValue)

      try {
        val startChain = Future.successful(StartChainValue)
        whenReady(executeChain(startChain, contextValue)) { n =>
          n shouldBe EndChainValue
        }
        ()
      } finally {
        logger.trace(s"Clearing MDC in thread [${Thread.currentThread().getName}]")
        MDC.clear()
      }
    }

    private def newContextValue: String =
      (Math.random() * 100).toInt.toString

    private def executeChain(startChain: Future[Int], expectedContextValue: String): Future[Int] =
      startChain.map {
        (assertContextHasValue(expectedContextValue) _).andThen(multiplyBy(2))
      }.map {
        (assertContextHasValue(expectedContextValue) _).andThen(multiplyBy(3))
      }.map {
        (assertContextHasValue(expectedContextValue) _).andThen(multiplyBy(4))
      }.map {
        (assertContextHasValue(expectedContextValue) _).andThen(multiplyBy(5))
      }.map {
        (assertContextHasValue(expectedContextValue) _).andThen(multiplyBy(6))
      }.map {
        (assertContextHasValue(expectedContextValue) _).andThen(multiplyBy(7))
      }.map {
        (assertContextHasValue(expectedContextValue) _).andThen(multiplyBy(8))
      }

    /*
     * This should be invoked on a different thread to that which is running the test.
     * If everything is working correctly, it should have access to the initial context value, as the top-level
     * context should have been migrated into any task threads.
     */
    private def assertContextHasValue(expectedValue: String)(n: Int): Int = {
      val actualValue = MDC.get(ContextKey)
      logger.trace(s"In task thread [${Thread.currentThread().getName}] context has actual value [$actualValue] - expected [$expectedValue]")
      actualValue shouldBe expectedValue
      n
    }

    private def multiplyBy(multiplier: Int)(n: Int): Int =
      multiplier * n
  }

  "A TracingExecutionContext" - {
    "migrates the mapped diagnostic context across tasks" in new Fixture {
      (1 to 200).foreach(_ => testContextMigration())
    }
  }
}

package tracing

import java.util

import com.typesafe.scalalogging.LazyLogging
import org.slf4j.MDC

import scala.concurrent.{ ExecutionContext, ExecutionContextExecutor }

/*
 * We are using the logback Mapped Diagnostic Context (MDC) to automatically associate a request trace context
 * with any log statements.  The implementation of MDC however uses a ThreadLocal, assuming a 'one thread per
 * request' model.  This is not suitable for Play's responsive asynchronous architecture.
 * This execution context is responsible for migrating the context of a calling thread into any spawned task,
 * so that a context moves across the threads in the pool as tasks are executed.
 */
class TracingExecutionContext(delegateContext: ExecutionContext) extends ExecutionContextExecutor with LazyLogging {
  override def execute(toRun: Runnable): Unit = {
    val callingThreadContext = MDC.getCopyOfContextMap
    logger.debug(s"execute called in thread [${Thread.currentThread().getName}] with a callingThreadContext of [$callingThreadContext]")
    delegateContext.execute(runnableWithContext(callingThreadContext)(toRun))
  }

  private def runnableWithContext(callerThreadContext: util.Map[String, String])(toRun: Runnable): Runnable =
    new Runnable {
      override def run(): Unit = {
        val originalThreadContext = MDC.getCopyOfContextMap
        logger.trace(s"Original context of thread [${Thread.currentThread().getName}] was [$originalThreadContext]")
        try {
          setThreadContextTo(callerThreadContext)
          logger.trace("running task ...")
          toRun.run()
        } finally {
          setThreadContextTo(originalThreadContext)
        }
      }
    }

  private def setThreadContextTo(context: util.Map[String, String]): Unit = {
    logger.debug(s"Setting context of thread [${Thread.currentThread().getName}] to [$context]")
    if (context == null) {
      MDC.clear()
    } else {
      MDC.setContextMap(context)
    }
  }

  override def reportFailure(cause: Throwable): Unit =
    delegateContext.reportFailure(cause)
}

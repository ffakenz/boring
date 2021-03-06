package arch.infra.router

import arch.common.Program.{Context, MError, ProgramError}
import arch.infra.logging.LoggingLibrary

import scala.reflect.ClassTag

class RouterF[F[_]: MError](
 onSuccess: String => Unit = _ => { println("success") },
 onFailure: ProgramError => Unit = _ => { println("failure") },
 recordLatencyInMillis: (String, Long, Long) => Unit = (_, _, _) => { println("recording latency") },
 handlers: Map[Class[_], Action => F[Any]] = Map.empty[Class[_], Action => F[Any]]
)(implicit logger: LoggingLibrary[F]) extends Router[F] {
  private val context: Context = Context("router")
  private val actionNotFoundErrorCode = 1

  override def publish[A <: Action](action: A): F[A#ReturnType] =
    handlers
      .get(action.getClass) match {
      case Some(handler) => handleAction(action, handler)
      case None => MError[F].raiseError(
        ProgramError("action not found", s"action ${action.getClass.getSimpleName} not found", context, actionNotFoundErrorCode)
      )
    }

  override def subscribe[A <: Action : ClassTag](handler: ActionHandler[F, A]): RouterF[F] = {
    val classTag = implicitly[ClassTag[A]]
    if (handlers.contains(classTag.runtimeClass)) {
      logger.logWarn("handler already subscribed")(context.copy(
        metadata = context.metadata + ("handler_name" -> handler.getClass.getSimpleName)
      ))
      new RouterF(onSuccess, onFailure, recordLatencyInMillis, handlers)
    } else {
      val transformed: Action => F[Any] = (t: Action) => MError[F].map(handler.handle(t.asInstanceOf[A]))(_.asInstanceOf[Any])
      new RouterF(onSuccess, onFailure, recordLatencyInMillis, handlers + (classTag.runtimeClass -> transformed))
    }
  }

  private def handleAction[A <: Action](action: A, handler: A => F[Any]): F[A#ReturnType] = {
    val before = System.currentTimeMillis()
    val maybeResponse: F[A#ReturnType] = MError[F].map(handler(action))(_.asInstanceOf[A#ReturnType])
    val recoverable = MError[F].recoverWith(maybeResponse) {
      case error: ProgramError =>
        onFailure(error)
        recordLatencyInMillis(action.getClass.getSimpleName, before, System.currentTimeMillis())
        maybeResponse
    }
    MError[F].map(recoverable) { result =>
      onSuccess(action.getClass.getSimpleName)
      recordLatencyInMillis(action.getClass.getSimpleName, before, System.currentTimeMillis())
      result
    }
  }
}

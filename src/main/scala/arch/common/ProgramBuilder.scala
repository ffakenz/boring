package arch.common

import arch.common.ProgramLive.{App, Test}
import arch.domain.modules.user.UserConfig.{UserConfigApp, UserConfigF, UserConfigTest}
import arch.domain.modules.user.UserRepoF
import arch.domain.modules.user.UserRepoLive.{UserRepoApp, UserRepoTest}
import arch.domain.modules.user.service.UserAction.UserActionHandler
import arch.domain.modules.user.service.UserServiceLive.{UserServiceApp, UserServiceTest}
import arch.domain.modules.user.service.{UserAction, UserServiceF}
import arch.infra.json.JsonLibraryF
import arch.infra.json.JsonLibraryLive.{JsonLibraryApp, JsonLibraryTest}
import arch.infra.logging.LoggingLibrary
import arch.infra.logging.LoggingLive.{LoggingApp, LoggingTest}
import arch.infra.monitoring.MonitoringLibrary
import arch.infra.monitoring.MonitoringLive.{MonitoringApp, MonitoringTest}
import arch.infra.router.RouterF
import arch.infra.router.RouterLive.{RouterApp, RouterTest}
import com.typesafe.config.Config

object ProgramBuilder {
  trait ProgramBuilder[F[_]] {
    def buildApp(config: Config): RouterF[F]
  }

  implicit lazy val production: ProgramBuilder[App] = (config: Config) => {
    import ProgramLive.App
    implicit lazy val userRepoLive: UserRepoF[App] = UserRepoApp
    implicit lazy val jsonLibraryLive: JsonLibraryF[App] = JsonLibraryApp
    implicit lazy val monitoring: MonitoringLibrary[App] = MonitoringApp
    implicit lazy val userConfigF: UserConfigF[App] = UserConfigApp
    implicit lazy val runService: UserServiceF[App] = new UserServiceApp(config.getConfig("user"))
    implicit lazy val logger: LoggingLibrary[App] = LoggingApp
    implicit lazy val router: RouterF[App] = new RouterApp()
      .subscribe[UserAction](new UserActionHandler[App]())
      .subscribe[UserAction](new UserActionHandler[App]()) // this is only added for debugging purposes
    router
  }

  implicit lazy val testing: ProgramBuilder[Test] = (config: Config) => {
    import ProgramLive.Test
    implicit lazy val userRepoLive: UserRepoF[Test] = UserRepoTest
    implicit lazy val jsonLibraryLive: JsonLibraryF[Test] = JsonLibraryTest
    implicit lazy val monitoring: MonitoringLibrary[Test] = MonitoringTest
    implicit lazy val userConfigF: UserConfigF[Test] = UserConfigTest
    implicit lazy val runService: UserServiceF[Test] = new UserServiceTest(config.getConfig("user"))
    implicit lazy val logger: LoggingLibrary[Test] = LoggingTest
    implicit lazy val router: RouterF[Test] = new RouterTest()
      .subscribe[UserAction](new UserActionHandler[Test]())
      .subscribe[UserAction](new UserActionHandler[Test]()) // this is only added for debugging purposes
    router
  }
}

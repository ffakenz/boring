package arch.domain.modules.user

import arch.common.Program
import arch.common.Program.{Context, MError}
import arch.domain.Repo
import arch.domain.modules.user.model.UserModel
import arch.domain.modules.user.model.UserModel.User

// IMPL class on top Map
class UserRepoF[F[_]: MError] extends Repo[F] {

  type M = UserModel.type

  private val module = Context("user")
  private val userRepoErrorCode = 1

  private var users: Map[M#Id, User] = Map.empty[M#Id, User]

  override def set(a: User)(implicit id: M#Identifiable[User]): F[Unit] =
    Program.App[F, Unit](module, userRepoErrorCode) {
      users = users.updated(id.id(a), a)
    }

  override def get(id: M#Id): F[Option[User]] =
    Program.App[F, Option[User]](module, userRepoErrorCode) {
      users.get(id)
    }
}

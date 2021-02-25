package io.flow.delta.www.lib

import scala.annotation.nowarn
import io.flow.delta.v0.models.{Organization, Project}
import io.flow.common.v0.models.User

object Permissions {

  object Organization {

    @nowarn
    def edit(organization: Organization, user: Option[User]): Boolean = !user.isEmpty
    def delete(organization: Organization, user: Option[User]): Boolean = edit(organization, user)

  }

  object Project {

    @nowarn
    def edit(project: Project, user: Option[User]): Boolean = !user.isEmpty
    def delete(project: Project, user: Option[User]): Boolean = edit(project, user)

  }

}

package db

import javax.inject.{Inject, Singleton}

import anorm._
import io.flow.delta.v0.models.DashboardBuild
import io.flow.postgresql.{Authorization, Query}
import play.api.db._

@Singleton
class DashboardBuildsDao @Inject()(
  db: Database
){

  private[this] val BaseQuery = Query(s"""
    select builds.id,
           builds.name,
           projects.id as project_id,
           projects.name as project_name,
           projects.uri as project_uri,
           projects.organization_id as project_organization_id,
           build_last_states.timestamp as last_timestamp,
           build_last_states.versions as last_versions,
           build_desired_states.timestamp as desired_timestamp,
           build_desired_states.versions as desired_versions
      from builds
      join projects on builds.project_id = projects.id
      join build_last_states on build_last_states.build_id = builds.id
      join build_desired_states on build_desired_states.build_id = builds.id
  """)

  def findAll(
    auth: Authorization,
    limit: Long = 25,
    offset: Long = 0
  ): Seq[DashboardBuild] = {

    db.withConnection { implicit c =>
      BaseQuery.
        and(Filters(auth).organizations("projects.organization_id").sql).
        limit(limit).
        offset(offset).
        orderBy("case when build_desired_states.versions::varchar = build_last_states.versions::varchar then 1 else 0 end, build_desired_states.timestamp desc").
        as(
          parser().*
        )
    }
  }

  private[this] def parser(): RowParser[DashboardBuild] = {
    io.flow.delta.v0.anorm.parsers.ProjectSummary.parserWithPrefix("project") ~
    SqlParser.str("name") ~
    io.flow.delta.v0.anorm.parsers.State.parserWithPrefix("last") ~
    io.flow.delta.v0.anorm.parsers.State.parserWithPrefix("desired") map {
      case projectSummary ~ name ~ lastState ~ desiredState => {
        DashboardBuild(
          project = projectSummary,
          name = name,
          desired = desiredState,
          last = lastState
        )
      }
    }
  }
}

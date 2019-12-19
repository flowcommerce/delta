package db

import javax.inject.{Inject, Singleton}
import anorm._
import io.flow.delta.v0.models.{DashboardBuild, State}
import io.flow.postgresql.{Authorization, Query}
import lib.ProjectConfigUtil
import org.joda.time.DateTime
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
           build_desired_states.versions as desired_versions,
           configs.data::text as config_data
      from builds
      join projects on builds.project_id = projects.id
      left join build_last_states on build_last_states.build_id = builds.id
      left join build_desired_states on build_desired_states.build_id = builds.id
      left join configs on configs.project_id = projects.id
  """)

  def findAll(
    auth: Authorization,
    limit: Option[Long],
    offset: Long = 0
  ): Seq[DashboardBuild] = {

    db.withConnection { implicit c =>
      BaseQuery.
        and(Filters(auth).organizations("projects.organization_id").sql).
        optionalLimit(limit).
        offset(offset).
        orderBy("case when coalesce(build_desired_states.versions::varchar, 'desired') = coalesce(build_last_states.versions::varchar, 'last') then 1 else 0 end, build_desired_states.timestamp desc").
        as(
          parser.*
        )
    }
  }

  private[this] val parser: RowParser[DashboardBuild] = {
    SqlParser.str("id") ~
    io.flow.delta.v0.anorm.parsers.ProjectSummary.parserWithPrefix("project") ~
    SqlParser.str("name") ~
    io.flow.delta.v0.anorm.parsers.State.parserWithPrefix("last").? ~
    io.flow.delta.v0.anorm.parsers.State.parserWithPrefix("desired").? ~
    SqlParser.str("config_data").? map {
      case id ~ projectSummary ~ name ~ lastState ~ desiredState ~ configData => {
        lazy val defaultState = State(
          timestamp = DateTime.now,
          versions = Nil,
        )
        DashboardBuild(
          id = id,
          project = projectSummary,
          name = name,
          cluster = configData.flatMap { c =>
            ProjectConfigUtil.cluster(c, name)
          }.getOrElse(ProjectConfigUtil.Unknown),
          desired = desiredState.getOrElse(defaultState),
          last = lastState.getOrElse(defaultState),
        )
      }
    }
  }

}

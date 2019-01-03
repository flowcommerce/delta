package io.flow.delta.lib

import io.flow.delta.v0.models.{OrganizationSummary, ProjectSummary}
import io.flow.util.{IdGenerator, Random}

trait Factories {

  val idGenerator = IdGenerator("tst")
  val random = Random()

  def makeName(): String = {
    s"Z Test ${random.alpha(20)}"
  }

  def makeUri(): String = {
    s"http://otto.com"
  }

  def makeKey(): String = {
    s"z-test-${random.alphaNumeric(20)}"
  }

  def makeProjectSummary(
    id: String = idGenerator.randomId(),
    name: String = makeName()
  ) = {
    val org = makeOrganizationSummary()
    ProjectSummary(
      id = id,
      organization = org,
      name = name,
      uri = "https://github.com/%s/%s".format(org.id, name)
    )
  }

  def makeOrganizationSummary(
    id: String = makeKey()
  ) = OrganizationSummary(
    id = id
  )

}

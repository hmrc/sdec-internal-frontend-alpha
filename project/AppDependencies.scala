import sbt.*

object AppDependencies {

  private val bootstrapVersion  = "10.7.0"
  private val hmrcMongoVersion  = "2.12.0"
  private val hmrcPlayFEVersion = "12.32.1"
  private val scalaCheckVersion = "3.2.17.0"

  val compile = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "play-frontend-hmrc-play-30" % hmrcPlayFEVersion,
    "uk.gov.hmrc"       %% "bootstrap-frontend-play-30" % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"         % hmrcMongoVersion
  )

  val test = Seq(
    "uk.gov.hmrc"       %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-30" % hmrcMongoVersion,
    "org.scalatestplus" %% "scalacheck-1-17"         % scalaCheckVersion
  ).map(_ % Test)

  def apply(): Seq[ModuleID] = compile ++ test
}

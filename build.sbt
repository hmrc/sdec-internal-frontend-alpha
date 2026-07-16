import play.sbt.routes.RoutesKeys
import sbt.Def
import uk.gov.hmrc.versioning.SbtGitVersioning.autoImport.majorVersion

lazy val appName: String = "sdec-internal-frontend-alpha"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "3.3.7"

lazy val commonSettings = Seq(
  scalacOptions += "-Wconf:src=routes/.*:s",
  scalacOptions += "-Wconf:msg=unused import&src=html/.*:s",
  scalacOptions += "-Wconf:msg=Flag.*repeatedly:s"
)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .disablePlugins(
    JUnitXmlReportPlugin
  ) // Required to prevent https://github.com/scalatest/scalatest/issues/1427
  .settings(inConfig(Test)(testSettings): _*)
  .settings(ThisBuild / useSuperShell := false)
  .settings(
    name := appName,
    RoutesKeys.routesImport ++= Seq(
      "models._",
      "uk.gov.hmrc.play.bootstrap.binders.RedirectUrl"
    ),
    TwirlKeys.templateImports ++= Seq(
      "play.twirl.api.HtmlFormat",
      "play.twirl.api.HtmlFormat._",
      "uk.gov.hmrc.govukfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.components._",
      "uk.gov.hmrc.hmrcfrontend.views.html.helpers._",
      "uk.gov.hmrc.hmrcfrontend.views.config._",
      "views.ViewUtils._",
      "models.Mode",
      "controllers.routes._",
      "viewmodels.govuk.all._"
    ),
    PlayKeys.playDefaultPort := 4500,
    scalacOptions ++= Seq(
      "-feature",
      "-Wconf:cat=deprecation:ws,cat=feature:ws,cat=optimizer:ws,src=target/.*:s"
    ),
    libraryDependencies ++= AppDependencies(),
    retrieveManaged             := true,
    pipelineStages              := Seq(digest),
    Assets / pipelineStages     := Seq(concat),
    Compile / scalafmtOnCompile := true,
    Test / scalafmtOnCompile    := true,
    Compile / unmanagedResourceDirectories += baseDirectory.value / "resources",
    Test / unmanagedSourceDirectories := (Test / baseDirectory)(base =>
      Seq(base / "test", base / "test-common")
    ).value,
    Test / unmanagedResourceDirectories := Seq(
      baseDirectory.value / "test-resources"
    ),
    Test / unmanagedSourceDirectories += baseDirectory.value / "test-utils",

    commonSettings
  )
  .settings(CodeCoverageSettings.settings: _*)

lazy val testSettings: Seq[Def.Setting[_]] = Seq(
  fork := true,
  unmanagedSourceDirectories += baseDirectory.value / "test-utils"
)

lazy val it =
  (project in file("it"))
    .enablePlugins(PlayScala)
    .dependsOn(microservice % "test->test")

inThisBuild(
  List(
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision
  )
)

addCommandAlias(
  "prePrChecks",
  "; scalafmtCheckAll; scalafmtSbtCheck; scalafixAll --check"
)
addCommandAlias(
  "checkCodeCoverage",
  "; clean; coverage; test; it/test; coverageReport"
)
addCommandAlias("lint", "; scalafmtAll; scalafmtSbt; scalafixAll")
addCommandAlias("prePush", "; reload; clean; compile; test; it/test; lint;")

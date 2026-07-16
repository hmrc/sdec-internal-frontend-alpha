import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    ".*.Module",
    ".*.model.*",
    ".*.config.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    ".*handlers.*",
    ".*components.*",
    ".*viewmodels.govuk.*",
    "controllers.LanguageSwitchController",
    "models.UserAnswers",
    "pages.*",
    "queries.*",
    "repositories.*",
    "views.ViewUtils",
    "views.html.ErrorTemplate",
    "models.Enumerable",
    "models.Mode",
    "forms.mappings.Formatters",
    "views.html.IndexView",
    "views.html.CheckYourAnswersView"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum    := true,
    ScoverageKeys.coverageHighlighting     := true
  )
}

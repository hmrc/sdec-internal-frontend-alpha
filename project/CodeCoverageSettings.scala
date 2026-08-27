import sbt.Setting
import scoverage.ScoverageKeys

object CodeCoverageSettings {
  private val excludedPackages: Seq[String] = Seq(
    "<empty>",
    "Reverse.*",
    ".*.Module",
    ".*.model.*",
    ".*.config.*",
    "config.FrontendAppConfig",
    "config.CurrencyFormatter",
    "config.Service",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    ".*Routes.*",
    "testOnly.*",
    "testOnlyDoNotUseInAppConf.*",
    ".*handlers.*",
    ".*components.*",
    ".*viewmodels.govuk.*",
    "connectors.ThreadSummaryConnector",
    "controllers.LanguageSwitchController",
    "controllers.DevelopmentInProgressController",
    "controllers.actions.AuthenticatedIdentifierAction",
    "models.UserAnswers",
    "models.ThreadDetails",
    "pages.*",
    "queries.*",
    "repositories.*",
    "views.html.DashboardView",
    "views.html.ViewUtils",
    "views.html.ErrorTemplate",
    "models.Enumerable",
    "models.Mode",
    "forms.mappings.Formatters",
    "forms.mappings.MonthFormatter",
    "views.html.IndexView",
    "views.html.CheckYourAnswersView",
    "views.html.DevelopmentInProgressView",
    "views.html.createthread.ThreadDetailsView",
    "views.html.createthread.RecipientDetailsView"
  )

  val settings: Seq[Setting[_]] = Seq(
    ScoverageKeys.coverageExcludedPackages := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true
  )
}

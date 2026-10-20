package models.audit

import play.api.libs.json.JsObject
import uk.gov.hmrc.play.audit.model.ExtendedDataEvent

trait AuditEvent {
  def auditType: String
  def detail:    JsObject

  def extendedDataEvent(auditSource: String): ExtendedDataEvent =
    ExtendedDataEvent(
      auditSource = auditSource,
      auditType = auditType,
      detail = detail
    )
}

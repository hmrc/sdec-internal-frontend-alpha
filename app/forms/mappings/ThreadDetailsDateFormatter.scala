/*
 * Copyright 2026 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package forms.mappings

import forms.mappings.Formatters
import play.api.data.FormError
import play.api.data.format.Formatter
import play.api.i18n.Messages

import java.time.{LocalDate, Month}
import scala.util.{Failure, Success, Try}

class ThreadDetailsDateFormatter(
  invalidKey:     String,
  allRequiredKey: String,
  twoRequiredKey: String,
  requiredKey:    String
)(implicit messages: Messages)
    extends Formatter[LocalDate]
    with Formatters {

  private val fieldKeys: List[String] = List("day", "month", "year")

  private def toDate(
    key:   String,
    day:   Int,
    month: Int,
    year:  Int
  ): Either[Seq[FormError], LocalDate] =

    if day < 1 || day > 31 then {
      Left(
        Seq(
          FormError(
            s"$key.day",
            invalidKey
          )
        )
      )
    } else if month < 1 || month > 12 then {
      Left(
        Seq(
          FormError(
            s"$key.month",
            invalidKey
          )
        )
      )
    } else {
      Try(LocalDate.of(year, month, day)) match {
        case Success(date) =>
          Right(date)

        case Failure(_) =>
          Left(
            Seq(
              FormError(
                s"$key.day",
                invalidKey
              )
            )
          )
      }
    }

  private def formatDate(
    key:  String,
    data: Map[String, String]
  ): Either[Seq[FormError], LocalDate] = {

    val int = intFormatter(
      requiredKey = invalidKey,
      wholeNumberKey = invalidKey,
      nonNumericKey = invalidKey
    )

    val monthFormatter = new ThreadDetailsMonthFormatter(invalidKey)

    for {
      day   <- int.bind(s"$key.day", data)
      month <- monthFormatter.bind(s"$key.month", data)
      year  <- int.bind(s"$key.year", data)

      date <- {
        val yearString =
          data.getOrElse(s"$key.year", "")

        if yearString.length != 4 then {
          Left(
            Seq(
              FormError(
                s"$key.year",
                "threadDetails.responseDate.error.year.length"
              )
            )
          )
        } else {
          toDate(key, day, month, year)
        }
      }
    } yield date
  }

  override def bind(
    key:  String,
    data: Map[String, String]
  ): Either[Seq[FormError], LocalDate] = {

    val fields = fieldKeys.map { field =>
      field -> data.get(s"$key.$field").filter(_.nonEmpty)
    }.toMap

    fields.count(_._2.isDefined) match {

      case 3 =>
        formatDate(key, data)

      case 2 =>
        val missingField =
          fields.collectFirst { case (field, None) =>
            field
          }.get

        Left(
          List(
            FormError(
              s"$key.$missingField",
              s"threadDetails.responseDate.error.$missingField.required"
            )
          )
        )

      case 1 =>
        Left(
          List(
            FormError(
              key,
              twoRequiredKey
            )
          )
        )

      case _ =>
        Left(
          List(
            FormError(
              key,
              allRequiredKey
            )
          )
        )
    }
  }

  override def unbind(
    key:   String,
    value: LocalDate
  ): Map[String, String] =
    Map(
      s"$key.day"   -> value.getDayOfMonth.toString,
      s"$key.month" -> value.getMonthValue.toString,
      s"$key.year"  -> value.getYear.toString
    )
}

private class ThreadDetailsMonthFormatter(
  invalidKey: String
) extends Formatter[Int]
    with Formatters {

  private val baseFormatter =
    stringFormatter(invalidKey)

  override def bind(
    key:  String,
    data: Map[String, String]
  ): Either[Seq[FormError], Int] = {

    val months = Month.values.toList

    baseFormatter
      .bind(key, data)
      .flatMap { str =>
        months
          .find(m =>
            m.getValue.toString == str.replaceAll("^0+", "") ||
              m.toString == str.toUpperCase ||
              m.toString.take(3) == str.toUpperCase
          )
          .map(m => Right(m.getValue))
          .getOrElse(
            Left(
              List(
                FormError(
                  key,
                  invalidKey
                )
              )
            )
          )
      }
  }

  override def unbind(
    key:   String,
    value: Int
  ): Map[String, String] =
    Map(key -> value.toString)
}

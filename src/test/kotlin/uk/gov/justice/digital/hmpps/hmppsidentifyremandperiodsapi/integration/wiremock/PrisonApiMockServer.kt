package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the prison-api.
 */
class PrisonApiExtension : BeforeAllCallback, AfterAllCallback, BeforeEachCallback {
  companion object {
    @JvmField
    val prisonApi = PrisonApiMockServer()
    const val IMPRISONED_PRISONER = "IMP"
    const val BAIL_PRISONER = "BAIL"
    const val INTERSECTING_PRISONER = "INTERSE"
    const val CRD_VALIDATION_PRISONER = "CRDVAL"
    const val RELATED_PRISONER = "RELATED"
    const val MULTIPLE_OFFENCES_PRISONER = "MULTI"
    const val NO_OFFENCE_DATES = "OFF_DATE"
  }
  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
    prisonApi.stubImprisonedCourtCaseResults()
    prisonApi.stubBailPrisoner()
    prisonApi.stubRelatedOffencesPrisoner()
    prisonApi.stubMultipleOffences()
    prisonApi.stubIntersectingSentence()
    prisonApi.stubCrdValidation()
    prisonApi.stubActiveBookingHasNoOffenceDates()
    prisonApi.stubGetPrison()
    prisonApi.stubHistoricCalculations()
    prisonApi.stubSentencesAndOffences()
  }

  override fun beforeEach(context: ExtensionContext) {
    prisonApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    prisonApi.stop()
  }
}

class PrisonApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 8332
  }

  fun stubSentencesAndOffences() {
    stubFor(
      get("/prison-api/api/offender-sentences/booking/1/sentences-and-offences")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                [
                  {
                    "bookingId": 123,
                    "sentenceSequence": 1,
                    "caseSequence": 9191,
                    "sentenceStatus": "A",
                    "sentenceCategory": "2003",
                    "sentenceCalculationType": "ADIMP_ORA",
                    "sentenceTypeDescription": "Standard Determinate",
                    "sentenceDate": "2024-01-01",
                    "terms": [{
                      "years": 0,
                      "months": 20,
                      "weeks": 0,
                      "days": 0
                    }],
                    "offences": [
                      {
                        "offenderChargeId": 1,
                        "offenceStartDate": "2015-03-17",
                        "offenceCode": "GBH",
                        "offenceDescription": "Grievous bodily harm"
                      }
                    ]
                  }
                ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
    stubFor(
      get("/prison-api/api/offender-sentences/booking/2/sentences-and-offences")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                [
                  {
                    "bookingId": 123,
                    "sentenceSequence": 1,
                    "caseSequence": 9191,
                    "sentenceStatus": "A",
                    "sentenceCategory": "2003",
                    "sentenceCalculationType": "ADIMP_ORA",
                    "sentenceTypeDescription": "Standard Determinate",
                    "sentenceDate": "2024-01-01",
                    "terms": [{
                      "years": 0,
                      "months": 20,
                      "weeks": 0,
                      "days": 0
                    }],
                    "offences": [
                      {
                        "offenderChargeId": 9991,
                        "offenceStartDate": "2015-03-17",
                        "offenceCode": "GBH",
                        "offenceDescription": "Grievous bodily harm"
                      }
                    ]
                  }
                ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubHistoricCalculations() {
    stubFor(
      get(WireMock.urlPathMatching("/prison-api/api/offender-dates/calculations/.*"))
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              [{
                "bookingId": 1,
                "calculationDate": "2021-03-30T09:46:55Z",
                "offenderSentCalculationId": 1
              }]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )

    stubFor(
      get("/prison-api/api/offender-dates/sentence-calculation/1")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
              {
                "conditionalReleaseDate": "2021-04-01"
              }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubImprisonedCourtCaseResults() {
    stubFor(
      get("/prison-api/api/court-date-results/by-charge/${PrisonApiExtension.IMPRISONED_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                [
                   {
                      "chargeId":1,
                      "offenceCode":"SX03163A",
                      "offenceStatue":"SX03",
                      "offenceDate":"2021-05-05",
                      "offenceDescription":"An offence",
                      "guilty":false,
                      "courtCaseId":1,
                      "sentenceSequence":1,
                      "sentenceDate":"2022-12-13",
                      "bookingId":1,
                      "bookNumber":"ABC123",
                      "outcomes":[
                         {
                            "id":1,
                            "date":"2022-10-13",
                            "resultCode":"4531",
                            "resultDescription":"Remand in Custody (Bail Refused)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":2,
                            "date":"2022-10-16",
                            "resultCode":"G",
                            "resultDescription":"Guilty",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":3,
                            "date":"2022-12-13",
                            "resultCode":"1002",
                            "resultDescription":"Imprisonment",
                            "resultDispositionCode":"F"
                         },
                         {
                            "id":4,
                            "date":"2015-12-13",
                            "resultCode":null,
                            "resultDescription":null,
                            "resultDispositionCode":null
                         },
                         {
                            "id":5,
                            "date":"2022-12-13",
                            "resultCode":null,
                            "resultDescription":null,
                            "resultDispositionCode":null
                         }
                      ]
                   }
                ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
    stubFor(
      get("/prison-api/api/offenders/${PrisonApiExtension.IMPRISONED_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "offenderNo": "${PrisonApiExtension.IMPRISONED_PRISONER}",
                  "agencyId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubBailPrisoner() {
    stubFor(
      get("/prison-api/api/court-date-results/by-charge/${PrisonApiExtension.BAIL_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                [
                   {
                      "chargeId":1,
                      "offenceCode":"SX03163A",
                      "offenceStatue":"SX03",
                      "offenceDate":"2010-05-05",
                      "offenceDescription":"An offence",
                      "guilty":false,
                      "courtCaseId":1,
                      "sentenceSequence":1,
                      "sentenceDate":"2015-09-18",
                      "bookingId":1,
                      "bookNumber":"ABC123",
                      "outcomes":[
                         {
                            "id":1,
                            "date":"2015-03-25",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":2,
                            "date":"2015-04-08",
                            "resultCode":"4530",
                            "resultDescription":"Remand on Conditional Bail",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":3,
                            "date":"2015-09-18",
                            "resultCode":"1002",
                            "resultDescription":"Imprisonment",
                            "resultDispositionCode":"F"
                         }
                      ]
                   }
                ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )

    stubFor(
      get("/prison-api/api/offenders/${PrisonApiExtension.BAIL_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "offenderNo": "${PrisonApiExtension.BAIL_PRISONER}",
                  "agencyId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubIntersectingSentence() {
    stubFor(
      get("/prison-api/api/court-date-results/by-charge/${PrisonApiExtension.INTERSECTING_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                 [
                    {
                       "chargeId":3933931,
                       "offenceCode":"WR91001",
                       "offenceStatue":"WR91",
                       "offenceDescription":"Abstract water without a licence",
                       "offenceDate":"2019-01-01",
                       "guilty":false,
                       "courtCaseId":1564627,
                       "courtCaseRef":"C1",
                       "courtLocation":"Birmingham Crown Court",
                       "sentenceSequence":3,
                       "sentenceDate":"2022-01-01",
                       "resultDescription":"Imprisonment",
                       "bookingId":1,
                       "bookNumber":"ABC123",
                       "outcomes":[
                          {
                             "id":499876018,
                             "date":"2020-01-01",
                             "resultCode":"4531",
                             "resultDescription":"Remand in Custody (Bail Refused)",
                             "resultDispositionCode":"I"
                          },
                          {
                             "id":499876019,
                             "date":"2022-01-01",
                             "resultCode":"1002",
                             "resultDescription":"Imprisonment",
                             "resultDispositionCode":"F"
                          }
                       ]
                    },
                    {
                       "chargeId":3933932,
                       "offenceCode":"CS00011",
                       "offenceStatue":"CS00",
                       "offenceDescription":"Act as child minder while disqualified from registration as a child minder",
                       "offenceDate":"2019-01-01",
                       "guilty":false,
                       "courtCaseId":1564628,
                       "courtCaseRef":"C2",
                       "courtLocation":"Birmingham Crown Court",
                       "sentenceSequence":4,
                       "sentenceDate":"2021-02-01",
                       "resultDescription":"Imprisonment",
                       "bookingId":1,
                       "bookNumber":"ABC123",
                       "outcomes":[
                          {
                             "id":499876020,
                             "date":"2021-01-01",
                             "resultCode":"4531",
                             "resultDescription":"Remand in Custody (Bail Refused)",
                             "resultDispositionCode":"I"
                          },
                          {
                             "id":499876021,
                             "date":"2021-02-01",
                             "resultCode":"1002",
                             "resultDescription":"Imprisonment",
                             "resultDispositionCode":"F"
                          }
                       ]
                    }
                 ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )

    stubFor(
      get("/prison-api/api/offenders/${PrisonApiExtension.INTERSECTING_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "offenderNo": "${PrisonApiExtension.INTERSECTING_PRISONER}",
                  "agencyId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubCrdValidation() {
    stubFor(
      get("/prison-api/api/court-date-results/by-charge/${PrisonApiExtension.CRD_VALIDATION_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
             [
                {
                   "chargeId":3933931,
                   "offenceCode":"WR91001",
                   "offenceStatue":"WR91",
                   "offenceDescription":"Abstract water without a licence",
                   "offenceDate":"2019-01-01",
                   "guilty":false,
                   "courtCaseId":1564627,
                   "courtCaseRef":"C1",
                   "courtLocation":"Birmingham Crown Court",
                   "sentenceSequence":3,
                   "sentenceDate":"2022-01-01",
                   "resultDescription":"Imprisonment",
                   "bookingId":1,
                   "bookNumber":"ABC123",
                   "outcomes":[
                      {
                         "id":499876018,
                         "date":"2020-01-01",
                         "resultCode":"4531",
                         "resultDescription":"Remand in Custody (Bail Refused)",
                         "resultDispositionCode":"I"
                      },
                      {
                         "id":499876019,
                         "date":"2022-01-01",
                         "resultCode":"1002",
                         "resultDescription":"Imprisonment",
                         "resultDispositionCode":"F"
                      }
                   ]
                },
                {
                   "chargeId":3933932,
                   "offenceCode":"CS00011",
                   "offenceStatue":"CS00",
                   "offenceDescription":"Act as child minder while disqualified from registration as a child minder",
                   "offenceDate":"2019-01-01",
                   "guilty":false,
                   "courtCaseId":1564628,
                   "courtCaseRef":"C2",
                   "courtLocation":"Birmingham Crown Court",
                   "sentenceSequence":4,
                   "sentenceDate":"2021-02-01",
                   "resultDescription":"Imprisonment",
                   "bookingId":1,
                   "bookNumber":"ABC123",
                   "outcomes":[
                      {
                         "id":499876020,
                         "date":"2021-01-01",
                         "resultCode":"4531",
                         "resultDescription":"Remand in Custody (Bail Refused)",
                         "resultDispositionCode":"I"
                      },
                      {
                         "id":499876021,
                         "date":"2021-02-01",
                         "resultCode":"1002",
                         "resultDescription":"Imprisonment",
                         "resultDispositionCode":"F"
                      }
                   ]
                }
             ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )

    stubFor(
      get("/prison-api/api/offenders/${PrisonApiExtension.CRD_VALIDATION_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "offenderNo": "${PrisonApiExtension.CRD_VALIDATION_PRISONER}",
                  "agencyId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubRelatedOffencesPrisoner() {
    stubFor(
      get("/prison-api/api/court-date-results/by-charge/${PrisonApiExtension.RELATED_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
             [
   {
      "chargeId":1,
      "offenceCode":"SX03163A",
      "offenceStatue":"SX03",
      "offenceDate":"2010-05-05",
      "offenceDescription":"An offence",
      "guilty":false,
      "courtCaseId":1,
      "sentenceSequence":1,
      "sentenceDate":"2015-09-25",
      "bookingId":1,
      "bookNumber":"ABC123",
      "outcomes":[
         {
            "id":1,
            "date":"2015-03-20",
            "resultCode":"4565",
            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
            "resultDispositionCode":"I"
         }
      ]
   },
   {
      "chargeId":2,
      "offenceCode":"SX03163A",
      "offenceStatue":"SX03",
      "offenceDate":"2010-05-05",
      "offenceDescription":"An offence",
      "guilty":false,
      "courtCaseId":1,
      "sentenceSequence":1,
      "sentenceDate":"2015-09-25",
      "bookingId":2,
      "bookNumber":"ABC321",
      "outcomes":[
         {
            "id":2,
            "date":"2015-04-10",
            "resultCode":"4530",
            "resultDescription":"Remand on Conditional Bail",
            "resultDispositionCode":"I"
         }
      ]
   },
   {
      "chargeId":3,
      "offenceCode":"SX03163A",
      "offenceStatue":"SX03",
      "offenceDate":"2010-05-05",
      "offenceDescription":"An offence",
      "guilty":false,
      "courtCaseId":1,
      "sentenceSequence":1,
      "sentenceDate":"2015-09-25",
      "bookingId":3,
      "bookNumber":"ABC456",
      "outcomes":[
         {
            "id":3,
            "date":"2015-09-25",
            "resultCode":"1002",
            "resultDescription":"Imprisonment",
            "resultDispositionCode":"F"
         }
      ]
   }
]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )

    stubFor(
      get("/prison-api/api/offenders/${PrisonApiExtension.RELATED_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "offenderNo": "${PrisonApiExtension.BAIL_PRISONER}",
                  "agencyId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubMultipleOffences() {
    stubFor(
      get("/prison-api/api/court-date-results/by-charge/${PrisonApiExtension.MULTIPLE_OFFENCES_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                [
                   {
                      "chargeId":1,
                      "offenceCode":"MD71130C",
                      "offenceStatue":"MD71",
                      "offenceDescription":"An offence",
                      "offenceDate":"2019-05-01",
                      "offenceEndDate":"2019-05-21",
                      "guilty":false,
                      "courtCaseId":1,
                      "bookingId":1,
                      "bookNumber":"ABC123",
                      "outcomes":[
                         {
                            "id":1,
                            "date":"2019-07-06",
                            "resultCode":"4560",
                            "resultDescription":"Commit/Transfer/Send to Crown Court for Trial in Custody",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":3,
                            "date":"2019-08-05",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":4,
                            "date":"2019-08-13",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":5,
                            "date":"2019-08-27",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":6,
                            "date":"2019-10-28",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":7,
                            "date":"2020-01-14",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":8,
                            "date":"2020-02-06",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":9,
                            "date":"2020-02-10",
                            "resultCode":"4004",
                            "resultDescription":"Sentence Postponed",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":10,
                            "date":"2020-02-20",
                            "resultCode":"4004",
                            "resultDescription":"Sentence Postponed",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":11,
                            "date":"2020-09-11",
                            "resultCode":"4004",
                            "resultDescription":"Sentence Postponed",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":12,
                            "date":"2020-09-24",
                            "resultCode":"4015",
                            "resultDescription":"Commit to Crown Court for Sentence Conditional Bail",
                            "resultDispositionCode":"I"
                         }
                      ]
                   },
                   {
                      "chargeId":2,
                      "offenceCode":"MD71145C",
                      "offenceStatue":"MD71",
                      "offenceDate":"2019-05-01",
                      "offenceEndDate":"2019-07-05",
                      "offenceDescription":"An other offence",
                      "guilty":false,
                      "courtCaseId":1,
                      "bookingId":1,
                      "bookNumber":"ABC123",
                      "outcomes":[
                         {
                            "id":1,
                            "date":"2019-07-06",
                            "resultCode":"4560",
                            "resultDescription":"Commit/Transfer/Send to Crown Court for Trial in Custody",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":3,
                            "date":"2019-08-05",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":4,
                            "date":"2019-08-13",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":5,
                            "date":"2019-08-27",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":6,
                            "date":"2019-10-28",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":7,
                            "date":"2020-01-14",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":8,
                            "date":"2020-02-06",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":9,
                            "date":"2020-02-10",
                            "resultCode":"4004",
                            "resultDescription":"Sentence Postponed",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":10,
                            "date":"2020-02-20",
                            "resultCode":"4004",
                            "resultDescription":"Sentence Postponed",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":11,
                            "date":"2020-09-11",
                            "resultCode":"4004",
                            "resultDescription":"Sentence Postponed",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":12,
                            "date":"2020-09-24",
                            "resultCode":"4015",
                            "resultDescription":"Commit to Crown Court for Sentence Conditional Bail",
                            "resultDispositionCode":"I"
                         }
                      ]
                   },
                   {
                      "chargeId":3,
                      "offenceCode":"MD71130C",
                      "offenceStatue":"MD71",
                      "offenceDate":"2019-05-01",
                      "offenceEndDate":"2019-05-21",
                      "offenceDescription":"An offence",
                      "guilty":false,
                      "courtCaseId":2,
                      "sentenceSequence":1,
                      "sentenceDate":"2021-06-15",
                      "bookingId":2,
                      "bookNumber":"ABC321",
                      "outcomes":[
                         {
                            "id":2,
                            "date":"2019-07-06",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":13,
                            "date":"2020-09-24",
                            "resultCode":"4015",
                            "resultDescription":"Commit to Crown Court for Sentence Conditional Bail",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":14,
                            "date":"2021-06-14",
                            "resultCode":"4004",
                            "resultDescription":"Sentence Postponed",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":15,
                            "date":"2021-06-15",
                            "resultCode":"1002",
                            "resultDescription":"Imprisonment",
                            "resultDispositionCode":"F"
                         }
                      ]
                   },
                   {
                      "chargeId":4,
                      "offenceCode":"MD71145C",
                      "offenceStatue":"MD71",
                      "offenceDescription":"An other offence",
                      "offenceDate":"2019-05-01",
                      "offenceEndDate":"2019-05-21",
                      "guilty":false,
                      "courtCaseId":2,
                      "sentenceSequence":2,
                      "sentenceDate":"2021-06-15",
                      "bookingId":2,
                      "bookNumber":"ABC321",
                      "outcomes":[
                         {
                            "id":2,
                            "date":"2019-07-06",
                            "resultCode":"4565",
                            "resultDescription":"Commit to Crown Court for Trial (Summary / Either Way Offences)",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":13,
                            "date":"2020-09-24",
                            "resultCode":"4015",
                            "resultDescription":"Commit to Crown Court for Sentence Conditional Bail",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":14,
                            "date":"2021-06-14",
                            "resultCode":"4004",
                            "resultDescription":"Sentence Postponed",
                            "resultDispositionCode":"I"
                         },
                         {
                            "id":15,
                            "date":"2021-06-15",
                            "resultCode":"1002",
                            "resultDescription":"Imprisonment",
                            "resultDispositionCode":"F"
                         }
                      ]
                   }
                ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )

    stubFor(
      get("/prison-api/api/offenders/${PrisonApiExtension.MULTIPLE_OFFENCES_PRISONER}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 2,
                  "offenderNo": "${PrisonApiExtension.MULTIPLE_OFFENCES_PRISONER}",
                  "agencyId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubActiveBookingHasNoOffenceDates() {
    stubFor(
      get("/prison-api/api/court-date-results/by-charge/${PrisonApiExtension.NO_OFFENCE_DATES}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
            [
   {
      "chargeId":1,
      "offenceCode":"SX03163A",
      "offenceStatue":"SX03",
      "offenceDate":null,
      "offenceDescription":"An offence",
      "guilty":false,
      "courtCaseId":1,
      "sentenceSequence":1,
      "sentenceDate":"2015-12-13",
      "bookingId":1,
      "bookNumber":"ABC123",
      "outcomes":[
         {
            "id":1,
            "date":"2022-10-13",
            "resultCode":"4531",
            "resultDescription":"Remand in Custody (Bail Refused)",
            "resultDispositionCode":"I"
         }
      ]
   }
]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
    stubFor(
      get("/prison-api/api/offenders/${PrisonApiExtension.NO_OFFENCE_DATES}")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "bookingId": 1,
                  "offenderNo": "${PrisonApiExtension.NO_OFFENCE_DATES}",
                  "agencyId": "BMI"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubGetPrison() {
    stubFor(
      get("/prison-api/api/agencies/BMI?activeOnly=false")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                {
                  "agencyId": "BMI",
                  "description": "Birmingham Prison"
                }
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
}

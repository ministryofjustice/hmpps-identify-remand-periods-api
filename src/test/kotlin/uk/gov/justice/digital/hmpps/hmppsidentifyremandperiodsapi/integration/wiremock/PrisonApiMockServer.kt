package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi.integration.wiremock

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo
import com.github.tomakehurst.wiremock.stubbing.StubMapping
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext

/*
    This class mocks the prison-api.
 */
class PrisonApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {
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
    const val INTERSECTING_MOVEMENTS = """
      [
          {
              "offenderNo": "$INTERSECTING_PRISONER",
              "createDateTime": "2021-03-15T09:04:19.272501",
              "fromAgency": "HMI",
              "fromAgencyDescription": "Humber (HMP)",
              "toAgency": "OUT",
              "toAgencyDescription": "Outside",
              "fromCity": "",
              "toCity": "",
              "movementType": "REL",
              "movementTypeDescription": "Release",
              "directionCode": "OUT",
              "movementDate": "2021-03-15",
              "movementTime": "09:03:14",
              "movementReason": "HDC release",
              "movementReasonCode": "HDC"
          },
          {
              "offenderNo": "$INTERSECTING_PRISONER",
              "createDateTime": "2021-03-25T09:04:19.272501",
              "fromAgency": "HLI",
              "fromAgencyDescription": "Hull (HMP)",
              "toAgency": "HMI",
              "toAgencyDescription": "Humber (HMP)",
              "fromCity": "",
              "toCity": "",
              "movementType": "ADM",
              "movementTypeDescription": "Admission",
              "directionCode": "IN",
              "movementDate": "2021-03-25",
              "movementTime": "12:20:38",
              "movementReason": "Imprisonment",
              "movementReasonCode": "IMP"
          }
      ]
    """
  }
  override fun beforeAll(context: ExtensionContext) {
    prisonApi.start()
    prisonApi.stubImprisonedCourtCaseResults()
    prisonApi.stubImprisonmentStatus(IMPRISONED_PRISONER)
    prisonApi.stubExternalMovements(IMPRISONED_PRISONER, "[]")
    prisonApi.stubBailPrisoner()
    prisonApi.stubEmptyImprisonmentStatus(BAIL_PRISONER)
    prisonApi.stubExternalMovements(BAIL_PRISONER, "[]")
    prisonApi.stubRelatedOffencesPrisoner()
    prisonApi.stubEmptyImprisonmentStatus(RELATED_PRISONER)
    prisonApi.stubExternalMovements(RELATED_PRISONER, "[]")
    prisonApi.stubMultipleOffences()
    prisonApi.stubEmptyImprisonmentStatus(MULTIPLE_OFFENCES_PRISONER)
    prisonApi.stubExternalMovements(MULTIPLE_OFFENCES_PRISONER, "[]")
    prisonApi.stubIntersectingSentence()
    prisonApi.stubEmptyImprisonmentStatus(INTERSECTING_PRISONER)
    prisonApi.stubExternalMovements(INTERSECTING_PRISONER, INTERSECTING_MOVEMENTS)
    prisonApi.stubCrdValidation()
    prisonApi.stubEmptyImprisonmentStatus(CRD_VALIDATION_PRISONER)
    prisonApi.stubExternalMovements(CRD_VALIDATION_PRISONER, "[]")
    prisonApi.stubActiveBookingHasNoOffenceDates()
    prisonApi.stubEmptyImprisonmentStatus(NO_OFFENCE_DATES)
    prisonApi.stubExternalMovements(NO_OFFENCE_DATES, "[]")
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
                    "bookingId": 1,
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
                    "bookingId": 2,
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
                        "offenderChargeId": 3,
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
                   "offenceDate":"2020-01-01",
                   "guilty":false,
                   "courtCaseId":1564627,
                   "courtCaseRef":"C1",
                   "courtLocation":"Birmingham Crown Court",
                   "sentenceSequence":3,
                   "sentenceDate":"2023-01-01",
                   "resultDescription":"Imprisonment",
                   "bookingId":1,
                   "bookNumber":"ABC123",
                   "outcomes":[
                      {
                         "id":499876018,
                         "date":"2021-01-01",
                         "resultCode":"4531",
                         "resultDescription":"Remand in Custody (Bail Refused)",
                         "resultDispositionCode":"I"
                      },
                      {
                         "id":499876019,
                         "date":"2023-01-01",
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
                   "offenceDate":"2020-01-01",
                   "guilty":false,
                   "courtCaseId":1564628,
                   "courtCaseRef":"C2",
                   "courtLocation":"Birmingham Crown Court",
                   "sentenceSequence":4,
                   "sentenceDate":"2022-02-01",
                   "resultDescription":"Imprisonment",
                   "bookingId":1,
                   "bookNumber":"ABC123",
                   "outcomes":[
                      {
                         "id":499876020,
                         "date":"2022-01-01",
                         "resultCode":"4531",
                         "resultDescription":"Remand in Custody (Bail Refused)",
                         "resultDispositionCode":"I"
                      },
                      {
                         "id":499876021,
                         "date":"2022-02-01",
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
      "bookingId":99,
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

  fun stubEmptyImprisonmentStatus(offenderNo: String) {
    stubFor(
      get("/prison-api/api/imprisonment-status-history/$offenderNo")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                []
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }

  fun stubImprisonmentStatus(offenderNo: String) {
    stubFor(
      get("/prison-api/api/imprisonment-status-history/$offenderNo")
        .willReturn(
          aResponse()
            .withHeader("Content-Type", "application/json")
            .withBody(
              """
                [
                  {
                    "status": "LR",
                    "effectiveDate": "2022-10-15"
                  }
                ]
              """.trimIndent(),
            )
            .withStatus(200),
        ),
    )
  }
  fun stubExternalMovements(prisonerId: String, json: String): StubMapping = stubFor(
    get(urlPathEqualTo("/prison-api/api/movements/offender/$prisonerId"))
      .willReturn(
        aResponse()
          .withHeader("Content-Type", "application/json")
          .withBody(json)
          .withStatus(200),
      ),
  )
}

package uk.gov.justice.digital.hmpps.hmppsidentifyremandperiodsapi

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.springframework.core.io.ClassPathResource
import java.io.File
import java.io.FileNotFoundException
import java.text.SimpleDateFormat

class TestUtil private constructor() {
  companion object {
    fun objectMapper(): ObjectMapper = ObjectMapper()
      .registerModule(JavaTimeModule())
      .setDateFormat(SimpleDateFormat("yyyy-MM-dd"))
      .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      .registerKotlinModule()
      .setDefaultPropertyInclusion(JsonInclude.Include.NON_DEFAULT)

    fun doAllInDir(fileName: String, fileType: String = "json", comsumer: (File) -> Unit) {
      val dir = ClassPathResource(fileName).file
      if (dir.isDirectory) {
        dir.walk().forEach {
          if (it.extension == fileType) {
            comsumer(it)
          }
        }
      } else {
        throw FileNotFoundException("File $fileName was not a directory")
      }
    }
  }
}

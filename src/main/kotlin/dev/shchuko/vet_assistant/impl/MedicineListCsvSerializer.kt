package dev.shchuko.vet_assistant.impl

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvParser
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import dev.shchuko.vet_assistant.api.MedicineListSerializer
import dev.shchuko.vet_assistant.service.model.MedicineWithDescription

class MedicineListCsvSerializer : MedicineListSerializer {
    data class CsvEntry(
        @field:JsonProperty("Name") val name: String,
        @field:JsonProperty("Analogues") val analogues: String,
        @field:JsonProperty("Active Ingredients") val ingredients: String,
        @field:JsonProperty("Description") val description: String,
    ) {
        @Suppress("unused") // Required for fasterxml
        constructor() : this("", "", "", "")
    }

    private val csvMapper = CsvMapper().apply {
        enable(CsvParser.Feature.TRIM_SPACES)
        enable(CsvParser.Feature.SKIP_EMPTY_LINES)
    }

    private val schema = CsvSchema.builder()
        .addColumn("Name")
        .addColumn("Analogues")
        .addColumn("Active Ingredients")
        .addColumn("Description")
        .build()

    override fun serialize(input: List<MedicineWithDescription>): String {
        val csvEntries = input.map {
            CsvEntry(it.name, it.analogues.joinToString(","), it.activeIngredients.joinToString(","), it.description)
        }
        return csvMapper.writer().with(schema.withHeader()).writeValueAsString(csvEntries)
    }

    override fun deserialize(input: String): List<MedicineWithDescription> {
        val parsed = csvMapper.readerFor(CsvEntry::class.java)
            .with(schema.withSkipFirstDataRow(true))
            .readValues<CsvEntry>(input)
            .readAll()

        return parsed.map { entry ->
            MedicineWithDescription(
                name = entry.name.trim(),
                description = entry.description.trim(),
                activeIngredients = entry.ingredients.split(",").map(String::trim).distinct(),
                analogues = entry.analogues.split(",").map(String::trim).distinct(),
            )
        }.distinctBy { it.name }
    }
}
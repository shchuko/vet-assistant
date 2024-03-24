package dev.shchuko.vet_assistant.impl

import dev.shchuko.vet_assistant.api.IBot
import dev.shchuko.vet_assistant.api.MedicineListSerializer
import dev.shchuko.vet_assistant.api.VetMedicineService
import dev.shchuko.vet_assistant.service.model.MedicineWithDescription
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.text.MessageFormat
import java.util.*

abstract class VetAssistantBot : IBot, KoinComponent {
    protected object Commands {
        const val GET_ALL = "getAll"
    }

    private val messagesBundle = ResourceBundle.getBundle("vet_bot_messages")

    private val medicineService by inject<VetMedicineService>()
    private val medicineListSerializer by inject<MedicineListSerializer>()

    private fun getString(key: String, vararg args: Any?): String = if (messagesBundle.containsKey(key)) {
        MessageFormat(
            messagesBundle.getString(key),
            messagesBundle.locale
        ).format(args)
    } else {
        key
    }

    protected fun handleSearchMedicineRequest(name: String): String? {
        val medicineName = name.trim().takeIf(String::isNotEmpty) ?: return null

        val result = medicineService.search(medicineName)
        return when {
            result.medicine != null -> buildString {
                appendLine(result.medicine.name)
                if (result.medicine.analogues.isNotEmpty()) {
                    appendLine()

                    val analogues = result.medicine.analogues.joinToString()
                    if (result.medicine.analogues.size == 1) {
                        appendLine(getString("message.medicine.search.analogue.single", analogues))
                    } else {
                        appendLine(getString("message.medicine.search.analogies", analogues))
                    }
                }
                if (result.medicine.activeIngredients.isNotEmpty()) {
                    appendLine()
                    val ingredients = result.medicine.activeIngredients.joinToString()
                    if (result.medicine.activeIngredients.size == 1) {
                        appendLine(getString("message.medicine.search.active.ingredient.single", ingredients))
                    } else {
                        appendLine(getString("message.medicine.search.active.ingredients", ingredients))
                    }
                }

                if (result.medicine.description.isNotBlank()) {
                    appendLine()
                    appendLine(result.medicine.description)
                }
            }

            result.misspellMatches.isEmpty() -> getString("message.medicine.search.not.found", medicineName)
            else -> """
                ${getString("message.medicine.search.not.found", medicineName)}
                
                ${getString("message.medicine.search.maybe.you.meant", result.misspellMatches.joinToString())}
            """.trimIndent()
        }
    }

    protected fun handleGetAllMedicineCsv(): String {
        val previousEntries = medicineService.getAll()
        return medicineListSerializer.serialize(previousEntries)
    }

    protected fun handleSetAllMedicineCsv(content: String) {
        val newEntries: List<MedicineWithDescription> = medicineListSerializer.deserialize(content)
        medicineService.setAll(newEntries)
    }

    protected fun getFileTooBigMessage() = getString("message.file.too.big")
}
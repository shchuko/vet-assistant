package dev.shchuko.vet_assistant.api

import dev.shchuko.vet_assistant.service.model.MedicineWithDescription

interface MedicineListSerializer {
    fun serialize(input: List<MedicineWithDescription>): String

    fun deserialize(input: String): List<MedicineWithDescription>
}
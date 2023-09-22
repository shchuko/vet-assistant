package dev.shchuko.vet_assistant.medicine.api

import java.util.*

interface MedicineService {
    @Throws(MedicineValidationException::class)
    suspend fun createMedicine(name: String, description: String = ""): UUID

    @Throws(MedicineValidationException::class)
    suspend fun editMedicineName(medicineId: UUID, name: String): String

    @Throws(MedicineValidationException::class)
    suspend fun editMedicineDescription(medicineId: UUID, description: String): String

    @Throws
    suspend fun findMedicine(name: String): Optional<MedicineDto>

    @Throws
    suspend fun searchMedicine(medicineOrSubstanceName: String): MedicineSearchResult

    @Throws(MedicineIdNotFoundException::class)
    suspend fun deleteMedicine(medicineId: UUID, safeDelete: Boolean = true): MedicineDeleteResult

    @Throws(MedicineValidationException::class)
    suspend fun addMedicineAnalogues(medicineId: UUID, analogueNames: List<String>): List<String>

    suspend fun deleteMedicineAnalogues(analogueNames: List<String>): Int

    @Throws(MedicineIdNotFoundException::class, MedicineAnalogueNotFoundException::class)
    suspend fun makeMedicineAnalogueNamePrimary(medicineId: UUID, analogueName: String): String

    @Throws(MedicineSubstanceValidationException::class)
    suspend fun createMedicineSubstance(name: String): UUID

    @Throws(MedicineSubstanceValidationException::class)
    suspend fun editMedicineSubstanceName(medicineSubstanceId: UUID, name: String): String

    @Throws(MedicineSubstanceIdNotFoundException::class)
    suspend fun deleteMedicineSubstance(
        medicineSubstanceId: UUID,
        safeDelete: Boolean = true
    ): MedicineSubstanceDeleteResult

    @Throws(MedicineIdNotFoundException::class, MedicineSubstanceIdNotFoundException::class)
    suspend fun linkSubstanceToMedicine(medicineId: UUID, medicineSubstanceId: UUID): Boolean

    @Throws(MedicineIdNotFoundException::class, MedicineSubstanceIdNotFoundException::class)
    suspend fun unlinkSubstanceFromMedicine(medicineId: UUID, medicineSubstanceId: UUID): Boolean
}
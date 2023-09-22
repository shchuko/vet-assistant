package dev.shchuko.vet_assistant.medicine.api

import java.util.*

open class MedicineServiceException : Exception()

class MedicineIdNotFoundException(val id: UUID) : MedicineServiceException()
class MedicineAnalogueNotFoundException(val primaryName: String, val analogueName: String) : MedicineServiceException()

class MedicineSubstanceIdNotFoundException(val id: UUID) : MedicineServiceException()

sealed class MedicineSubstanceValidationException() : MedicineServiceException() {
    class MedicineSubstanceNameCollisionException(name: String) : MedicineSubstanceValidationException()
    class MedicineSubstanceNameIsBlankException : MedicineSubstanceValidationException()
    class MedicineSubstanceNameTooLongException(val providedLen: Int, val maxLen: Int) :
        MedicineSubstanceValidationException()
}

sealed class MedicineValidationException(cause: Throwable? = null) : MedicineServiceException() {
    class MedicineNameCollisionException(val name: String) : MedicineValidationException()
    class MedicineNameIsBlankException : MedicineValidationException()
    class MedicineNameTooLongException(val providedLen: Int, val maxLen: Int) : MedicineValidationException()
    class MedicineDescriptionTooLongException(val providedLen: Int, val maxLen: Int) : MedicineValidationException()
}
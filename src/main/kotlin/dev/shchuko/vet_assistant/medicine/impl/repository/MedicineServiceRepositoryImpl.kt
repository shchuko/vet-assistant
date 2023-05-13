package dev.shchuko.vet_assistant.medicine.impl.repository

class MedicineServiceRepositoryImpl : MedicineServiceRepository {
    // TODO use database
    private val stub = mapOf(
        "Габапентин" to """
            Ваще от эпилепсии но так можно кота успокоить или себя нервного.
            
            Сутулым собакам не рекомендуется.
        """.trimIndent(),

        "Серения" to """
            Серения чтобы была норм серения у вашего питомца.
        """.trimIndent(),

        "Серения1" to "описание Серении1",
        "Серения2" to "описание Серении2",
        "Серения3" to "описание Серении3",
        "Серения4" to "описание Серении4",

        "Triglukonat" to "description Triglukonat",
    )

    override fun getAllMedicineNames(): List<String> = stub.keys.toList()

    override fun getMedicineDescriptionByName(name: String): String? = stub[name]
}
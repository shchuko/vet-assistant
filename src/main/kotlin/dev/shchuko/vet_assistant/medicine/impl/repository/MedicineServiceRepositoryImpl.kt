package dev.shchuko.vet_assistant.medicine.impl.repository

class MedicineServiceRepositoryImpl : MedicineServiceRepository {
    // TODO use database
    private val stub = mapOf(
        "габапентин" to "описание габапентина",
        "серения" to "описание серении"
    )

    override fun getAllMedicineNames(): List<String> = stub.keys.toList()

    override fun getMedicineDescriptionByName(name: String): String? = stub[name]
}
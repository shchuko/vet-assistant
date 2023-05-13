import ru.homyakin.iuliia.Schemas
import ru.homyakin.iuliia.Translator

// TODO support bidirectional transliteration
private var translator = Translator(Schemas.ICAO_DOC_9303)
fun String.transliterate(): String? = translator.translate(this)
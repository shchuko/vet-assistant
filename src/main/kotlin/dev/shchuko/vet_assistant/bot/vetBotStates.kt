package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.api.keyboard.buildKeyboard
import dev.shchuko.vet_assistant.bot.base.api.model.BotContext
import dev.shchuko.vet_assistant.bot.base.statemachine.State
import dev.shchuko.vet_assistant.medicine.api.model.MedicineSearchResult
import dev.shchuko.vet_assistant.medicine.api.service.MedicineService
import org.koin.java.KoinJavaComponent.getKoin

private typealias VetBotState = State<BotContext<VetBotContext>>

class VetBotContext {
    var command: String? = null

    fun reset() {
        command = null
    }
}

object SelectCommandState : VetBotState {
    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) {
        val text = context.update.message.text

        context.botSubContext.command = null

        if (!text.startsWith("/")) return
        val maybeCommand = text.split("\\s+")[0]
            .substringAfter("/")
            .substringBefore("@")
        if (maybeCommand.isBlank()) return
        context.botSubContext.command = maybeCommand
    }
}

object CommandNotFoundPrinterState : VetBotState {
    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) = context.bot.sendMessage(
        context.update,
        VetBotStringsBundle.getString(
            "message.no.such.command",
            context.update.user.locale,
            context.botSubContext.command
        )
    )
}

object StartHelpPrinterState : VetBotState {
    fun messageMatches(context: BotContext<VetBotContext>): Boolean {
        val text = context.update.message.text
        return text.startsWith("/start") || text.startsWith("/help")
    }

    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) = context.bot.sendMessage(
        context.update,
        VetBotStringsBundle.getString("message.greeting", context.update.user.locale)
    )
}

object BasicUserKeyboardShowState : VetBotState {
    private val keyboardMarkup = buildKeyboard {
        row {
            button { VetBotStringsBundle.getString("button.medicine.search", it) }
        }
    }

    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) =
        context.bot.sendMessage(context.update, keyboard = keyboardMarkup)
}

object BasicUserKeyboardSelectionParseState : VetBotState {
    override fun isPauseBefore(context: BotContext<VetBotContext>) = true

    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) {
        // only pause with [isPauseBefore], state will be selected in STM choice
    }

    fun medicineSearchSelected(context: BotContext<VetBotContext>) =
        context.update.message.text == VetBotStringsBundle.getString(
            "button.medicine.search",
            context.update.user.locale
        )
}

object MedicineSearchAskNameState : VetBotState {
    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) = context.bot.sendMessage(
        context.update,
        VetBotStringsBundle.getString("message.medicine.search.ask.name", context.update.user.locale)
    )
}

object MedicineSearchPerformState : VetBotState {
    private val medicineService by lazy { getKoin().get<MedicineService>() }

    override fun isPauseBefore(context: BotContext<VetBotContext>) = true

    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) {
        val searchResult = medicineService.findMedicine(context.update.message.text)

        when (searchResult.type) {
            MedicineSearchResult.ResultType.EXACT_MATCH -> context.bot.sendMessage(
                context.update,
                VetBotStringsBundle.getString(
                    "message.medicine.search.result.exact.match",
                    context.update.user.locale,
                    searchResult.result[0].name, searchResult.result[0].description
                )
            )


            MedicineSearchResult.ResultType.MISSPELL_SINGLE_MATCH -> context.bot.sendMessage(
                context.update,
                VetBotStringsBundle.getString(
                    "message.medicine.search.result.misspell.single.match",
                    context.update.user.locale,
                    searchResult.result[0].name, searchResult.result[0].description
                )
            )

            // TODO allow proposed names selection using /search <name>
            MedicineSearchResult.ResultType.MISSPELL_MULTIPLE_MATCH -> context.bot.sendMessage(
                context.update,
                VetBotStringsBundle.getString(
                    "message.medicine.search.result.misspell.multiple.match",
                    context.update.user.locale,
                    searchResult.result.map { it.name }.joinToString("\n")
                )
            )


            MedicineSearchResult.ResultType.NONE_MATCH -> context.bot.sendMessage(
                context.update,
                VetBotStringsBundle.getString(
                    "message.medicine.search.result.none.match",
                    context.update.user.locale
                )
            )
        }
    }
}

object AdminCommandState : VetBotState {
    fun messageMatches(context: BotContext<VetBotContext>) = context.update.message.text.startsWith("/admin")

    // TODO implement admin logic
    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) = Unit
}

object GlobalErrorHandlerState : VetBotState {
    override fun enter(context: BotContext<VetBotContext>, error: Throwable?) {
        context.botSubContext.reset()
    }

    override fun isPauseAfter(context: BotContext<VetBotContext>) = true
}


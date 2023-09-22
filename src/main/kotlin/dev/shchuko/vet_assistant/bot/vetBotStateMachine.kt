package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.VetBotContext.MainMenuCommand
import dev.shchuko.vet_assistant.bot.base.api.keyboard.buildKeyboard
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import dev.shchuko.vet_assistant.bot.base.statemachine.state
import dev.shchuko.vet_assistant.bot.base.statemachine.subStateMachine
import dev.shchuko.vet_assistant.medicine.api.MedicineSearchResult
import dev.shchuko.vet_assistant.medicine.api.MedicineService
import org.koin.java.KoinJavaComponent
import org.slf4j.LoggerFactory


object VetBotStateMachine : StateMachine<VetBotContext>("VetBotStateMachine2", {
    val logger = LoggerFactory.getLogger(VetBotStateMachine::class.java)

    initialStateId = "main_menu"
    globalErrorHandlerId = "global_error_handler"


    subStateMachine("main_menu", MainMenuStateMachine) {
        onSuccess = plain("main_menu")
    }

    state("global_error_handler") {
        onEnter = { context, error ->
            logger.error("Global error handler has caught an error", error)

            try {
                context.bot.sendMessage(
                    context.update,
                    VetBotUiBundle.getString("message.unexpected.error.happened", context.update.user.locale)
                )
            } catch (e: Exception) {
                logger.error("Global error hangler failed to report error to user", e)
            }
        }

        transitions = {
            onSuccess = plain("main_menu")
        }
    }
})


object MainMenuStateMachine : StateMachine<VetBotContext>("MainMenuStateMachine", {
    initialStateId = "show_keyboard"

    state("show_keyboard") {
        onEnter = { context, _ ->
            val keyboard = buildKeyboard {
                row {
                    button(VetBotUiBundle.getString("button.main.medicine.search", context.update.user.locale))
                }

                row {
                    button(VetBotUiBundle.getString("button.main.admin.menu", context.update.user.locale))
                }
            }

            context.bot.sendMessage(
                context.update,
                VetBotUiBundle.getString("message.please.use.keyboard", context.update.user.locale),
                keyboard
            )
        }

        isPauseAfter = { true }

        transitions = {
            onSuccess = plain("parse_command")
        }
    }

    state("parse_command") {
        onEnter = { context, _ ->
            val message = context.update.message.text.trim()
            context.mainMenuCommand = when (message) {
                in VetBotUiBundle.getStringInAllLocales("button.main.medicine.search") -> MainMenuCommand.MEDICINE_SEARCH
                in VetBotUiBundle.getStringInAllLocales("button.main.admin.menu") -> MainMenuCommand.ADMIN_MENU
                else -> null
            }
        }

        transitions = {
            onSuccess = choice {
                case("medicine_search") { it.mainMenuCommand == MainMenuCommand.MEDICINE_SEARCH }
                case("admin_menu") { it.mainMenuCommand == MainMenuCommand.ADMIN_MENU }
                default("command_unknown")
            }
        }
    }

    subStateMachine("medicine_search", MedicineSearchStateMachine) {
        onSuccess = plain("show_keyboard")
    }

    subStateMachine("admin_menu", AdminMenuStateMachine) {
        onSuccess = plain("show_keyboard")
    }

    state("command_unknown") {
        transitions = {
            onSuccess = plain("show_keyboard")
        }
    }
})

object MedicineSearchStateMachine : StateMachine<VetBotContext>("MedicineSearchStateMachine", {
    initialStateId = "request_medicine_name"

    state("request_medicine_name") {
        onEnter = { context, _ ->
            val keyboard = buildKeyboard {
                inline = true
                row {
                    button(VetBotUiBundle.getString("button.cancel", context.update.user.locale))
                }
            }

            context.askMedicineSearchMessageId = context.bot.sendMessage(
                context.update,
                VetBotUiBundle.getString("message.medicine.search.ask.name", context.update.user.locale),
                keyboard = keyboard
            ).messageId
        }

        isPauseAfter = { true }

        transitions = {
            onSuccess = plain("parse_medicine_name")
        }
    }

    state("parse_medicine_name") {
        onEnter = { context, _ ->
            val message = context.update.message.text.trim()
            context.medicineSearchCancel = message in VetBotUiBundle.getStringInAllLocales("button.cancel")
            context.medicineSearchName = if (context.medicineSearchCancel) null else message

            if (context.medicineSearchCancel) {
                context.bot.editMessage(
                    requireNotNull(context.askMedicineSearchMessageId),
                    VetBotUiBundle.getString("message.medicine.search.cancelled")
                )
            } else {
                context.bot.editMessage(
                    requireNotNull(context.askMedicineSearchMessageId),
                    VetBotUiBundle.getString("message.medicine.search.ask.name")
                ) // remove keyboard
            }
        }

        transitions = {
            onSuccess = choice {
                case("cancel_search") { it.medicineSearchCancel }
                default("do_search")
            }
        }
    }

    state("cancel_search") {
        transitions = {
            onSuccess = noTransition()
        }
    }

    state("do_search") {
        onEnter = { context, _ ->
            // TODO performance check
            val medicineService = KoinJavaComponent.getKoin().get<MedicineService>()
            val requestedName = requireNotNull(context.medicineSearchName)
            context.medicineSearchName = null
            when (val searchResult = medicineService.searchMedicine(requestedName)) {
                is MedicineSearchResult.SingleMatchByName -> context.bot.sendMessage(
                    context.update, VetBotUiBundle.getString(
                        if (searchResult.misspell) "message.medicine.search.result.misspell.single.match" else "message.medicine.search.result.exact.match",
                        context.update.user.locale,
                        searchResult.value.name,
                        searchResult.value.description
                    )
                )

                is MedicineSearchResult.MultipleMatch -> context.bot.sendMessage(
                    context.update, VetBotUiBundle.getString("message.medicine.search.result.misspell.multiple.match",
                        context.update.user.locale,
                        requestedName,
                        (searchResult.medicineNames + searchResult.substanceNames).joinToString("\n") { "- $it" })
                )

                is MedicineSearchResult.NoneMatch -> context.bot.sendMessage(
                    context.update, VetBotUiBundle.getString(
                        "message.medicine.search.result.none.match", context.update.user.locale, requestedName
                    )
                )
            }
        }

        transitions = {
            onSuccess = plain("request_medicine_name")
        }
    }
})

object AdminMenuStateMachine : StateMachine<VetBotContext>("AdminMenuStateMachine", {
    initialStateId = "todo"

    state("todo") {
        transitions = {
            onSuccess = noTransition()
        }
    }
})


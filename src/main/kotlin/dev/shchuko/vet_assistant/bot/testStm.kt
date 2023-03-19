package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.api.model.BotContext
import dev.shchuko.vet_assistant.bot.base.statemachine.State
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import dev.shchuko.vet_assistant.bot.base.statemachine.plainChain


class UserContext

object BotMainStateMachine : StateMachine<BotContext<UserContext>>({
    initialState = SelectCommandState

    state(SelectCommandState) {
        onSuccess = choice {
            case(StartHelpPrinterState::messageMatches, StartHelpPrinterState)
            case(AdminCommandState::messageMatches, AdminCommandState)
            default(CommandNotFoundPrinterState)
        }
    }

    state(CommandNotFoundPrinterState) {
        onSuccess = noTransition()
    }

    plainChain(StartHelpPrinterState, BasicUserKeyboardEnableState) {
        onSuccess = plain(BasicUserKeyboardSelectionParseState)
    }

    state(BasicUserKeyboardSelectionParseState) {
        onSuccess = choice {
            case(BasicUserKeyboardSelectionParseState::medicineSearchSelected, MedicineSearchAskNameState)
            default(SelectCommandState) // if none matched from keyboard, assume user entered some command like /start
        }
    }

    state(AdminCommandState) {
        onSuccess = plain(BasicUserKeyboardSelectionParseState)
    }

    plainChain(MedicineSearchAskNameState, MedicineSearchPerformState) {
        onSuccess = plain(BasicUserKeyboardEnableState)
    }
})

object SelectCommandState : State<BotContext<UserContext>> {
    override fun enter(context: BotContext<UserContext>, error: Throwable?) {
        // command selection is performed in state machine
    }
}

object CommandNotFoundPrinterState : State<BotContext<UserContext>> {
    override fun enter(context: BotContext<UserContext>, error: Throwable?) {
        context.bot.reply(context.update) // No such command
    }

}

object StartHelpPrinterState : State<BotContext<UserContext>> {
    fun messageMatches(context: BotContext<UserContext>): Boolean {
        val text = context.update.message.text
        return text.startsWith("/start") || text.startsWith("/help")
    }

    override fun enter(context: BotContext<UserContext>, error: Throwable?) {
        context.bot.reply(context.update) // print help
    }
}

object BasicUserKeyboardEnableState : State<BotContext<UserContext>> {
    override fun enter(context: BotContext<UserContext>, error: Throwable?) {
        context.bot.reply(context.update) // switch on basic user keyboard
    }
}

object BasicUserKeyboardSelectionParseState : State<BotContext<UserContext>> {
    override fun enter(context: BotContext<UserContext>, error: Throwable?) {
        context.bot.reply(context.update) // read response and save into context
    }

    fun medicineSearchSelected(context: BotContext<UserContext>) = true
}

object MedicineSearchAskNameState : State<BotContext<UserContext>> {
    override fun enter(context: BotContext<UserContext>, error: Throwable?) {
        context.bot.reply(context.update) // switch off keyboard and ask for medicine name
    }
}

object MedicineSearchPerformState : State<BotContext<UserContext>> {
    override fun enter(context: BotContext<UserContext>, error: Throwable?) {
        context.bot.reply(context.update) // read a message and search for medicine here
    }
}

object AdminCommandState : State<BotContext<UserContext>> {
    fun messageMatches(context: BotContext<UserContext>) = context.update.message.text.startsWith("/admin")

    override fun enter(context: BotContext<UserContext>, error: Throwable?) =
        BasicUserKeyboardEnableState.enter(context, error)
}


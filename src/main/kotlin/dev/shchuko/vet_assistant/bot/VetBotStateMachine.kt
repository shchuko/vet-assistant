package dev.shchuko.vet_assistant.bot

import dev.shchuko.vet_assistant.bot.base.api.model.BotContext
import dev.shchuko.vet_assistant.bot.base.statemachine.StateMachine
import dev.shchuko.vet_assistant.bot.base.statemachine.plainChain

object VetBotStateMachine : StateMachine<BotContext<VetBotContext>>({
    initialState = SelectCommandState
    globalErrorHandler = GlobalErrorHandlerState

    state(GlobalErrorHandlerState) {
        onSuccess = plain(SelectCommandState)
    }

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

    plainChain(StartHelpPrinterState, BasicUserKeyboardShowState) {
        onSuccess = plain(BasicUserKeyboardSelectionParseState)
    }

    state(BasicUserKeyboardSelectionParseState) {
        onSuccess = choice {
            case(BasicUserKeyboardSelectionParseState::medicineSearchSelected, MedicineSearchAskNameState)
            default(SelectCommandState) // if none matched from keyboard, assume user entered some command like /start
        }
    }

    plainChain(MedicineSearchAskNameState, MedicineSearchPerformState) {
        onSuccess = plain(BasicUserKeyboardShowState)
    }


    // TODO implement admin logic
    state(AdminCommandState) {
        onSuccess = plain(BasicUserKeyboardShowState)
    }
})
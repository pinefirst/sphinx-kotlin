package chat.sphinx.chat_common.ui.viewstate.messageholder

import chat.sphinx.chat_common.ui.viewstate.InitialHolderViewState
import chat.sphinx.wrapper_message.Message
import chat.sphinx.wrapper_message.isDirectPayment
import chat.sphinx.wrapper_message.isMessage

sealed class MessageHolderViewState {

    abstract val message: Message
    abstract val background: HolderBackground
    abstract val initialHolder: InitialHolderViewState

    abstract val directPayment: LayoutState.DirectPayment?

    val messageTypeMessageContent: LayoutState.MessageTypeMessageContent? by lazy(LazyThreadSafetyMode.NONE) {
        message.messageContentDecrypted?.let {
            message.giphyData?.let { giphyData ->
                // TODO: show only the giphyData.text when rendering logic is implemented
//                giphyData.text?.let { text ->
//                    LayoutState.MessageTypeMessageContent(text)
//                }
                LayoutState.MessageTypeMessageContent(giphyData.toString())
            } ?: /*if (message.podBoost != null) {*/ // TODO: Uncomment once boost layout logic is implemented
                LayoutState.MessageTypeMessageContent(it.value)
//            } else {
//                null
//            }
        }
    }


    class InComing(
        override val message: Message,
        override val background: HolderBackground.In,
        override val initialHolder: InitialHolderViewState,
    ) : MessageHolderViewState() {

        override val directPayment: LayoutState.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
            if (message.type.isDirectPayment()) {
                LayoutState.DirectPayment(showSent = false, amount = message.amount)
            } else {
                null
            }
        }
    }

    class OutGoing(
        override val message: Message,
        override val background: HolderBackground.Out,
    ) : MessageHolderViewState() {
        override val initialHolder: InitialHolderViewState
            get() = InitialHolderViewState.None

        override val directPayment: LayoutState.DirectPayment? by lazy(LazyThreadSafetyMode.NONE) {
            if (message.type.isDirectPayment()) {
                LayoutState.DirectPayment(showSent = true, amount = message.amount)
            } else {
                null
            }
        }
    }
}

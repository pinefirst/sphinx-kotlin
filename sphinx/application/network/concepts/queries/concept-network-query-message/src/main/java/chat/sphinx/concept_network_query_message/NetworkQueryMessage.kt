package chat.sphinx.concept_network_query_message

import chat.sphinx.concept_network_query_message.model.*
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_common.dashboard.ContactId
import chat.sphinx.wrapper_common.lightning.LightningPaymentRequest
import chat.sphinx.wrapper_common.lightning.Sat
import chat.sphinx.wrapper_common.message.MessageId
import chat.sphinx.wrapper_common.message.MessagePagination
import chat.sphinx.wrapper_common.message.MessageUUID
import chat.sphinx.wrapper_message.MessageType
import chat.sphinx.wrapper_message_media.MediaToken
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryMessage {

    abstract fun getMessages(
        messagePagination: MessagePagination?,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<GetMessagesResponse, ResponseError>>

    abstract fun getPayments(
        offset: Int = 0,
        limit: Int = 50,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<List<TransactionDto>, ResponseError>>

    abstract fun sendMessage(
        postMessageDto: PostMessageDto,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<MessageDto, ResponseError>>

    abstract fun boostMessage(
        chatId: ChatId,
        pricePerMessage: Sat,
        escrowAmount: Sat,
        tipAmount: Sat,
        messageUUID: MessageUUID,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<MessageDto, ResponseError>>

    abstract fun sendPayment(
        postPaymentDto: PostPaymentDto,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<MessageDto, ResponseError>>

    abstract fun sendPaymentRequest(
        postPaymentRequestDto: PostPaymentRequestDto,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<MessageDto, ResponseError>>

    abstract fun payPaymentRequest(
        putPaymentRequestDto: PutPaymentRequestDto,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<MessageDto, ResponseError>>

    abstract fun sendKeySendPayment(
        postPaymentDto: PostPaymentDto,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<KeySendPaymentDto, ResponseError>>

    abstract fun payAttachment(
        chatId: ChatId,
        contactId: ContactId?,
        amount: Sat,
        mediaToken: MediaToken,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null
    ): Flow<LoadResponse<MessageDto, ResponseError>>

    abstract fun readMessages(
        chatId: ChatId,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<Any?, ResponseError>>

//    app.post('/messages/clear', messages.clearMessages)

    //////////////
    /// DELETE ///
    //////////////
    /**
     * Delete message with the associated [MessageId]
     *
     * DELETE /message/$messageId
     */
    abstract fun deleteMessage(
        messageId: MessageId,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<MessageDto, ResponseError>>

    abstract fun processMemberRequest(
        contactId: ContactId,
        messageId: MessageId,
        type: MessageType,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null
    ): Flow<LoadResponse<PutMemberResponseDto, ResponseError>>
}

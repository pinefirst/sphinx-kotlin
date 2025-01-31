package chat.sphinx.concept_network_query_verify_external

import chat.sphinx.concept_network_query_verify_external.model.PersonInfoDto
import chat.sphinx.concept_network_query_verify_external.model.SignBase64Dto
import chat.sphinx.concept_network_query_verify_external.model.VerifyExternalDto
import chat.sphinx.concept_network_query_verify_external.model.VerifyExternalInfoDto
import chat.sphinx.kotlin_response.LoadResponse
import chat.sphinx.kotlin_response.ResponseError
import chat.sphinx.wrapper_relay.AuthorizationToken
import chat.sphinx.wrapper_relay.RelayUrl
import chat.sphinx.wrapper_relay.TransportToken
import kotlinx.coroutines.flow.Flow

abstract class NetworkQueryAuthorizeExternal {

    abstract fun verifyExternal(
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<VerifyExternalDto, ResponseError>>

    abstract fun signBase64(
        base64: String,
        relayData: Triple<AuthorizationToken, TransportToken?, RelayUrl>? = null,
    ): Flow<LoadResponse<SignBase64Dto, ResponseError>>

    abstract fun authorizeExternal(
        host: String,
        challenge: String,
        token: String,
        info: VerifyExternalInfoDto,
    ): Flow<LoadResponse<Any, ResponseError>>

    abstract fun getPersonInfo(
        host: String,
        publicKey: String
    ): Flow<LoadResponse<PersonInfoDto, ResponseError>>
}
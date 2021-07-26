package chat.sphinx.tribe_detail.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import chat.sphinx.camera_view_model_coordinator.request.CameraRequest
import chat.sphinx.camera_view_model_coordinator.response.CameraResponse
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.concept_repository_chat.ChatRepository
import chat.sphinx.concept_repository_contact.ContactRepository
import chat.sphinx.concept_service_media.MediaPlayerServiceController
import chat.sphinx.concept_service_media.UserAction
import chat.sphinx.concept_view_model_coordinator.ViewModelCoordinator
import chat.sphinx.kotlin_response.Response
import chat.sphinx.logger.SphinxLogger
import chat.sphinx.logger.e
import chat.sphinx.menu_bottom.ui.MenuBottomViewState
import chat.sphinx.menu_bottom_profile_pic.PictureMenuHandler
import chat.sphinx.menu_bottom_profile_pic.PictureMenuViewModel
import chat.sphinx.menu_bottom_profile_pic.UpdatingImageViewState
import chat.sphinx.podcast_player.objects.toPodcast
import chat.sphinx.tribe.TribeMenuHandler
import chat.sphinx.tribe.TribeMenuViewModel
import chat.sphinx.tribe_detail.R
import chat.sphinx.tribe_detail.navigation.TribeDetailNavigator
import chat.sphinx.wrapper_chat.Chat
import chat.sphinx.wrapper_chat.ChatAlias
import chat.sphinx.wrapper_chat.isTribeOwnedByAccount
import chat.sphinx.wrapper_common.dashboard.ChatId
import chat.sphinx.wrapper_contact.Contact
import chat.sphinx.wrapper_io_utils.InputStreamProvider
import chat.sphinx.wrapper_meme_server.PublicAttachmentInfo
import chat.sphinx.wrapper_message_media.MediaType
import chat.sphinx.wrapper_message_media.toMediaType
import dagger.hilt.android.lifecycle.HiltViewModel
import io.matthewnelson.android_feature_navigation.util.navArgs
import io.matthewnelson.android_feature_viewmodel.SideEffectViewModel
import io.matthewnelson.android_feature_viewmodel.submitSideEffect
import io.matthewnelson.android_feature_viewmodel.updateViewState
import io.matthewnelson.concept_coroutines.CoroutineDispatchers
import io.matthewnelson.concept_views.viewstate.ViewStateContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.InputStream
import javax.annotation.meta.Exhaustive
import javax.inject.Inject

internal inline val TribeDetailFragmentArgs.chatId: ChatId
    get() = ChatId(argChatId)

@HiltViewModel
internal class TribeDetailViewModel @Inject constructor(
    private val app: Application,
    dispatchers: CoroutineDispatchers,
    savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val cameraCoordinator: ViewModelCoordinator<CameraRequest, CameraResponse>,
    private val contactRepository: ContactRepository,
    private val mediaPlayerServiceController: MediaPlayerServiceController,
    val navigator: TribeDetailNavigator,
    val LOG: SphinxLogger,
): SideEffectViewModel<
        Context,
        TribeDetailSideEffect,
        TribeDetailViewState>(dispatchers, TribeDetailViewState.Idle),
    TribeMenuViewModel,
    PictureMenuViewModel
{
    companion object {
        const val TAG = "TribeDetailViewModel"
    }

    private val args: TribeDetailFragmentArgs by savedStateHandle.navArgs()

    val chatId = args.chatId
    val podcast = args.argPodcast?.toPodcast()

    val updatingImageViewStateContainer: ViewStateContainer<UpdatingImageViewState> by lazy {
        ViewStateContainer(UpdatingImageViewState.Idle)
    }

    private val chatSharedFlow: SharedFlow<Chat?> = flow {
        emitAll(chatRepository.getChatById(chatId))
    }.distinctUntilChanged().shareIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(2_000),
        replay = 1,
    )

    private suspend fun getOwner(): Contact {
        return contactRepository.accountOwner.value.let { contact ->
            if (contact != null) {
                contact
            } else {
                var resolvedOwner: Contact? = null
                try {
                    contactRepository.accountOwner.collect { ownerContact ->
                        if (ownerContact != null) {
                            resolvedOwner = ownerContact
                            throw Exception()
                        }
                    }
                } catch (e: Exception) {}
                delay(25L)

                resolvedOwner!!
            }
        }
    }

    private suspend fun updateChatViewStat() {
        chatRepository.getChatById(chatId).collect { chat ->
            chat?.let {
                updateViewState(
                    TribeDetailViewState.TribeProfile(
                        it,
                        getOwner(),
                        podcast
                    )
                )
            }
        }
    }
    private suspend fun getChat(): Chat {
        chatSharedFlow.replayCache.firstOrNull()?.let { chat ->
            return chat
        }

        chatSharedFlow.firstOrNull()?.let { chat ->
            return chat
        }

        var chat: Chat? = null

        try {
            chatSharedFlow.collect {
                if (it != null) {
                    chat = it
                    throw Exception()
                }
            }
        } catch (e: Exception) {}
        delay(25L)

        return chat!!
    }

    fun load() {
        viewModelScope.launch(mainImmediate) {
            updateViewState(TribeDetailViewState.TribeProfile(getChat(), getOwner(), podcast))
        }
    }

    val imageLoaderDefaults by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    override val tribeMenuHandler: TribeMenuHandler by lazy {
        TribeMenuHandler()
    }

    override val pictureMenuHandler: PictureMenuHandler by lazy {
        PictureMenuHandler()
    }

    fun updateSatsPerMinute(sats: Long) {
        podcast?.let { podcast ->
            podcast.satsPerMinute = sats

            viewModelScope.launch(mainImmediate) {
                mediaPlayerServiceController.submitAction(
                    UserAction.AdjustSatsPerMinute(
                        args.chatId,
                        podcast.getMetaData()
                    )
                )
            }
        }
    }

    fun updateProfileAlias(alias: String?) {
        viewModelScope.launch(mainImmediate) {
            val response = chatRepository.updateChatProfileInfo(
                getChat().id,
                alias?.let { ChatAlias(it) }
            )

            when (response) {
                is Response.Success -> {}
                is Response.Error -> {
                    submitSideEffect(TribeDetailSideEffect.FailedToUpdateProfileAlias)
                }
            }
        }
    }

    private var cameraJob: Job? = null

    override fun updatePictureFromCamera() {
        if (cameraJob?.isActive == true) {
            return
        }

        cameraJob = viewModelScope.launch(dispatchers.mainImmediate) {
            val response = cameraCoordinator.submitRequest(CameraRequest)

            @Exhaustive
            when (response) {
                is Response.Error -> {}
                is Response.Success -> {

                    @Exhaustive
                    when (response.value) {
                        is CameraResponse.Image -> {
                            pictureMenuHandler.viewStateContainer.updateViewState(
                                MenuBottomViewState.Closed
                            )

                            updatingImageViewStateContainer.updateViewState(
                                UpdatingImageViewState.UpdatingImage
                            )

                            val mediaType = MediaType.Image(
                                "${MediaType.IMAGE}/${response.value.value.extension}"
                            )

                            try {
                                val attachmentInfo = PublicAttachmentInfo(
                                    stream = object : InputStreamProvider() {
                                        override fun newInputStream(): InputStream {
                                            return response.value.value.inputStream()
                                        }
                                    },
                                    mediaType = mediaType,
                                    fileName = response.value.value.name,
                                    contentLength = response.value.value.length(),
                                )

                                val repoResponse = chatRepository.updateChatProfileInfo(
                                    getChat().id,
                                    null,
                                    attachmentInfo
                                )

                                @Exhaustive
                                when (repoResponse) {
                                    is Response.Error -> {
                                        LOG.e(TAG, "Error update chat Profile Picture: ", repoResponse.cause.exception)

                                        updatingImageViewStateContainer.updateViewState(
                                            UpdatingImageViewState.UpdatingImageFailed
                                        )

                                        submitSideEffect(TribeDetailSideEffect.FailedToUpdateProfilePic)
                                    }
                                    is Response.Success -> {
                                        updatingImageViewStateContainer.updateViewState(
                                            UpdatingImageViewState.UpdatingImageSucceed
                                        )

                                        updateChatViewStat()
                                    }
                                }
                            } catch (e: Exception) {
                                updatingImageViewStateContainer.updateViewState(
                                    UpdatingImageViewState.UpdatingImageFailed
                                )

                                submitSideEffect(TribeDetailSideEffect.FailedToUpdateProfilePic)

                                response.value.value.delete()
                            }
                        }
                        else -> {}
                    }
                }
                else -> {}
            }
        }
    }


    override fun handleActivityResultUri(uri: Uri?) {
        if (uri == null) {
            return
        }

        val cr = app.contentResolver

        cr.getType(uri)?.let { crType ->

            MimeTypeMap.getSingleton().getExtensionFromMimeType(crType)?.let { ext ->

                crType.toMediaType().let { mType ->

                    @Exhaustive
                    when (mType) {
                        is MediaType.Image -> {
                            val stream: InputStream? = try {
                                cr.openInputStream(uri)
                            } catch (e: Exception) {
                                // TODO: Handle Error
                                null
                            }

                            if (stream != null) {
                                pictureMenuHandler.viewStateContainer.updateViewState(
                                    MenuBottomViewState.Closed
                                )

                                updatingImageViewStateContainer.updateViewState(
                                    UpdatingImageViewState.UpdatingImage
                                )

                                viewModelScope.launch(dispatchers.mainImmediate) {
                                    val attachmentInfo = PublicAttachmentInfo(
                                        stream = object : InputStreamProvider() {
                                            var initialStreamUsed: Boolean = false
                                            override fun newInputStream(): InputStream {
                                                return if (!initialStreamUsed) {
                                                    initialStreamUsed = true
                                                    stream
                                                } else {
                                                    cr.openInputStream(uri)!!
                                                }
                                            }
                                        },
                                        mediaType = mType,
                                        fileName = "image.$ext",
                                        contentLength = null,
                                    )

                                    val repoResponse = chatRepository.updateChatProfileInfo(
                                        getChat().id,
                                        null,
                                        attachmentInfo
                                    )

                                    @Exhaustive
                                    when (repoResponse) {
                                        is Response.Error -> {
                                                LOG.e(
                                                    TAG,
                                                    "Error update chat Profile Picture: ",
                                                    repoResponse.cause.exception
                                                )

                                                updatingImageViewStateContainer.updateViewState(
                                                    UpdatingImageViewState.UpdatingImageFailed
                                                )

                                                submitSideEffect(TribeDetailSideEffect.FailedToUpdateProfilePic)
                                            }
                                            is Response.Success -> {
                                                updatingImageViewStateContainer.updateViewState(
                                                    UpdatingImageViewState.UpdatingImageSucceed
                                                )

                                                updateChatViewStat()
                                            }
                                        }
                                }
                            }
                        }
                        is MediaType.Audio,
                        is MediaType.Pdf,
                        is MediaType.Text,
                        is MediaType.Unknown,
                        is MediaType.Video -> {
                            // do nothing
                        }
                    }
                }
            }
        }
    }

    /***
     * Tribe Menu Implementation
     */
    override fun deleteTribe() {
        viewModelScope.launch(mainImmediate) {
            val chat = getChat()

            submitSideEffect(
                TribeDetailSideEffect.AlertConfirmDeleteTribe(chat) {
                    viewModelScope.launch(mainImmediate) {
                        when (chatRepository.exitAndDeleteTribe(chat)) {
                            is Response.Success -> {
                                navigator.goBackToDashboard()
                            }
                            is Response.Error -> {
                                submitSideEffect(TribeDetailSideEffect.FailedToDeleteTribe)
                            }
                        }
                    }
                }
            )
        }
        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
    }

    override fun shareTribe() {
        viewModelScope.launch(mainImmediate) {
            val chat = getChat()
            if (chat.isTribeOwnedByAccount(getOwner().nodePubKey)) {
                val shareTribeURL = "sphinx.chat://?action=tribe&uuid=${chat.uuid.value}&host=${chat.host?.value}"
                navigator.toShareTribeScreen(shareTribeURL, app.getString(R.string.qr_code_title))
            }
        }

        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
    }

    override fun exitTribe() {
        viewModelScope.launch(mainImmediate) {
            val chat = getChat()

            submitSideEffect(
                TribeDetailSideEffect.AlertConfirmExitTribe(chat) {
                    viewModelScope.launch(mainImmediate) {
                        when (chatRepository.exitAndDeleteTribe(chat)) {
                            is Response.Success -> {
                                navigator.goBackToDashboard()
                            }
                            is Response.Error -> {
                                submitSideEffect(TribeDetailSideEffect.FailedToExitTribe)
                            }
                        }
                    }
                }
            )
        }
        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
    }

    override fun editTribe() {
        tribeMenuHandler.viewStateContainer.updateViewState(MenuBottomViewState.Closed)
//        TODO("Implement Edit Tribe Functionality on TribeMenuHandler")
    }
}

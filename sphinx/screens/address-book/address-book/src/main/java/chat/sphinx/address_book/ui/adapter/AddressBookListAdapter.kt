package chat.sphinx.address_book.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.recyclerview.widget.RecyclerView
import chat.sphinx.address_book.databinding.LayoutAddressBookContactHolderBinding
import chat.sphinx.address_book.ui.AddressBookViewModel
import chat.sphinx.concept_image_loader.Disposable
import chat.sphinx.concept_image_loader.ImageLoader
import chat.sphinx.concept_image_loader.ImageLoaderOptions
import chat.sphinx.concept_image_loader.Transformation
import chat.sphinx.resources.R
import chat.sphinx.resources.setBackgroundRandomColor
import chat.sphinx.resources.setTextColorExt
import chat.sphinx.wrapper_contact.Contact
import io.matthewnelson.android_feature_screens.util.goneIfFalse
import io.matthewnelson.android_feature_viewmodel.collectViewState
import io.matthewnelson.android_feature_viewmodel.currentViewState
import io.matthewnelson.android_feature_viewmodel.util.OnStopSupervisorScope
import kotlinx.coroutines.launch

internal class AddressBookListAdapter(
    private val imageLoader: ImageLoader<ImageView>,
    private val lifecycleOwner: LifecycleOwner,
    private val viewModel: AddressBookViewModel,
): RecyclerView.Adapter<AddressBookListAdapter.AddressBookViewHolder>(), DefaultLifecycleObserver {

    private val addressBookContacts = ArrayList<Contact>(viewModel.currentViewState.list)
    private val supervisor = OnStopSupervisorScope(lifecycleOwner)

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
            viewModel.collectViewState { viewState ->
                addressBookContacts.addAll(viewState.list)
                this@AddressBookListAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun getItemCount(): Int {
        return addressBookContacts.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddressBookListAdapter.AddressBookViewHolder {
        val binding = LayoutAddressBookContactHolderBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return AddressBookViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddressBookListAdapter.AddressBookViewHolder, position: Int) {
        holder.bind(position)
    }

    private val imageLoaderOptions: ImageLoaderOptions by lazy {
        ImageLoaderOptions.Builder()
            .placeholderResId(R.drawable.ic_profile_avatar_circle)
            .transformation(Transformation.CircleCrop)
            .build()
    }

    inner class AddressBookViewHolder(
        private val binding: LayoutAddressBookContactHolderBinding
    ): RecyclerView.ViewHolder(binding.root) {

        private var disposable: Disposable? = null
        private var dContact: Contact? = null

//        init {
//            binding.layoutConstraintChatHolder.setOnClickListener {
//                dChat?.let { dashboardChat ->
//                    @Exhaustive
//                    when (dashboardChat) {
//                        is DashboardChat.Active.Conversation -> {
//                            supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
//                                viewModel.dashboardNavigator.toChatContact(
//                                    dashboardChat.chat,
//                                    dashboardChat.contact
//                                )
//                            }
//                        }
//                        is DashboardChat.Active.GroupOrTribe -> {
//                            supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
//
//                                if (dashboardChat.chat.type.isGroup()) {
//                                    viewModel.dashboardNavigator.toChatGroup(dashboardChat.chat)
//                                } else if (dashboardChat.chat.type.isTribe()) {
//                                    viewModel.dashboardNavigator.toChatTribe(dashboardChat.chat)
//                                }
//
//                            }
//                        }
//                        is DashboardChat.Inactive.Conversation -> {
//                            supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
//                                viewModel.dashboardNavigator.toChatContact(
//                                    null,
//                                    dashboardChat.contact
//                                )
//                            }
//                        }
//                    }
//                }
//            }
//        }

        fun bind(position: Int) {
            binding.apply {
                val addressBookContact: Contact = addressBookContacts.getOrNull(position) ?: let {
                    dContact = null
                    return
                }
                dContact = addressBookContact
                disposable?.dispose()

                // Set Defaults
                textViewAddressBookHolderName.setTextColorExt(android.R.color.white)

                // Image
                addressBookContact.photoUrl.let { url ->

                    layoutAddressBookInitialHolder.imageViewChatPicture.goneIfFalse(url != null)
                    layoutAddressBookInitialHolder.textViewInitials.goneIfFalse(url == null)

                    if (url != null) {
                        supervisor.scope().launch(viewModel.dispatchers.mainImmediate) {
                            imageLoader.load(
                                layoutAddressBookInitialHolder.imageViewChatPicture,
                                url.value,
                                imageLoaderOptions
                            )
                        }
                    } else {
                        layoutAddressBookInitialHolder.textViewInitials.text =
                            addressBookContact.alias?.value ?: ""
                        layoutAddressBookInitialHolder.textViewInitials
                            .setBackgroundRandomColor(R.drawable.chat_initials_circle)
                    }

                }

                // Name
                textViewAddressBookHolderName.text = if (addressBookContact.alias != null) {
                    addressBookContact.alias?.value
                } else {
                    // Should never make it here, but just in case...
                    textViewAddressBookHolderName.setTextColorExt(R.color.primaryRed)
                    "ERROR: NULL NAME"
                }
            }
        }
    }

    init {
        lifecycleOwner.lifecycle.addObserver(this)
    }
}
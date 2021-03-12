package chat.sphinx.dto_chat.model

@Suppress("NOTHING_TO_INLINE")
inline fun ChatPrivate.isTrue(): Boolean =
    this is ChatPrivate.True

/**
 * Converts the integer value returned over the wire to an object.
 *
 * @throws [IllegalArgumentException] if the integer is not supported
 * */
@Suppress("NOTHING_TO_INLINE")
@Throws(IllegalArgumentException::class)
inline fun Int?.toChatPrivate(): ChatPrivate =
    when (this) {
        null,
        ChatPrivate.NOT_PRIVATE -> {
            ChatPrivate.False
        }
        ChatPrivate.PRIVATE -> {
            ChatPrivate.True
        }
        else -> {
            throw IllegalArgumentException(
                "ChatPrivate for integer '$this' not supported"
            )
        }
    }

/**
 * Comes off the wire as:
 *  - null (Not Private)
 *  - 0 (Not Private)
 *  - 1 (Private)
 * */
sealed class ChatPrivate {

    companion object {
        const val PRIVATE = 1
        const val NOT_PRIVATE = 0
    }

    abstract val value: Int

    object True: ChatPrivate() {
        override val value: Int
            get() = PRIVATE
    }

    object False: ChatPrivate() {
        override val value: Int
            get() = NOT_PRIVATE
    }
}

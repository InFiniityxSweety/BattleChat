package com.ebicep.chatplus.features

import com.ebicep.chatplus.ChatPlus
import com.ebicep.chatplus.config.configDirectoryPath
import com.ebicep.chatplus.config.json
import com.ebicep.chatplus.events.EventBus
import com.ebicep.chatplus.features.textbarelements.AddTextBarElementEvent
import com.ebicep.chatplus.features.textbarelements.EmojiPickerTextBarElement
import kotlinx.serialization.Serializable
import java.io.File

/** BattleChat emoji catalog plus persisted recent/favorite state. */
object EmojiPicker {

    enum class Category(val displayName: String, val icon: String) {
        RECENT("Recent", "🕘"),
        FAVORITES("Favorites", "★"),
        FACES("Faces", "😀"),
        PEOPLE("People", "👋"),
        NATURE("Nature", "🌿"),
        FOOD("Food", "🍕"),
        ACTIVITY("Activity", "⚽"),
        MORE("More", "💡"),
    }

    data class Emoji(val value: String, val keywords: String)

    @Serializable
    private data class PersistedState(
        val recent: List<String> = emptyList(),
        val favorites: List<String> = emptyList(),
    )

    private val stateFile: File
        get() = File(configDirectoryPath, "emoji-picker.json")

    private val recent = mutableListOf<String>()
    private val favorites = linkedSetOf<String>()

    private val catalog: Map<Category, List<Emoji>> = mapOf(
        Category.FACES to listOf(
            Emoji("😀", "grin happy smile"), Emoji("😃", "happy smile"), Emoji("😄", "happy laugh"), Emoji("😁", "grin teeth"),
            Emoji("😆", "laugh xd"), Emoji("😅", "sweat laugh"), Emoji("🤣", "rofl laugh"), Emoji("😂", "tears joy laugh"),
            Emoji("🙂", "smile"), Emoji("🙃", "upside down"), Emoji("😉", "wink"), Emoji("😊", "blush smile"),
            Emoji("😇", "angel innocent"), Emoji("🥰", "love hearts"), Emoji("😍", "heart eyes love"), Emoji("🤩", "star eyes"),
            Emoji("😘", "kiss"), Emoji("😋", "yummy tongue"), Emoji("😜", "wink tongue"), Emoji("🤪", "crazy goofy"),
            Emoji("😎", "cool sunglasses"), Emoji("🤓", "nerd glasses"), Emoji("🧐", "monocle"), Emoji("🤔", "thinking hmm"),
            Emoji("🫡", "salute"), Emoji("🤨", "raised eyebrow"), Emoji("😐", "neutral"), Emoji("😑", "expressionless"),
            Emoji("😏", "smirk"), Emoji("😒", "unamused"), Emoji("🙄", "eyeroll"), Emoji("😬", "grimace"),
            Emoji("🥺", "pleading"), Emoji("😭", "cry sob"), Emoji("😡", "angry mad"), Emoji("🤬", "swearing rage"),
            Emoji("😈", "devil"), Emoji("💀", "skull dead lol"), Emoji("🤡", "clown"), Emoji("👻", "ghost")
        ),
        Category.PEOPLE to listOf(
            Emoji("👋", "wave hello bye"), Emoji("🤚", "hand stop"), Emoji("🖐️", "hand five"), Emoji("✋", "raised hand"),
            Emoji("👌", "ok hand"), Emoji("🤌", "pinched italian"), Emoji("🤏", "small pinch"), Emoji("✌️", "peace victory"),
            Emoji("🤞", "fingers crossed luck"), Emoji("🫰", "finger heart"), Emoji("🤟", "love you hand"), Emoji("🤘", "rock horns"),
            Emoji("👍", "thumb up yes"), Emoji("👎", "thumb down no"), Emoji("✊", "fist"), Emoji("👊", "punch fist"),
            Emoji("👏", "clap applause"), Emoji("🙌", "celebrate hands"), Emoji("🫶", "heart hands"), Emoji("🙏", "pray thanks"),
            Emoji("💪", "strong muscle"), Emoji("🫵", "you point"), Emoji("👀", "eyes look"), Emoji("🧠", "brain smart"),
            Emoji("🗣️", "speak talking"), Emoji("👑", "crown king"), Emoji("🕺", "dance man"), Emoji("💃", "dance woman"),
            Emoji("🧙", "wizard"), Emoji("🥷", "ninja"), Emoji("🦸", "hero"), Emoji("🧌", "troll")
        ),
        Category.NATURE to listOf(
            Emoji("🐶", "dog"), Emoji("🐱", "cat"), Emoji("🐭", "mouse"), Emoji("🐹", "hamster"), Emoji("🐰", "rabbit"),
            Emoji("🦊", "fox"), Emoji("🐻", "bear"), Emoji("🐼", "panda"), Emoji("🐸", "frog"), Emoji("🐵", "monkey"),
            Emoji("🦁", "lion"), Emoji("🐯", "tiger"), Emoji("🐮", "cow"), Emoji("🐷", "pig"), Emoji("🐔", "chicken"),
            Emoji("🐧", "penguin"), Emoji("🐦", "bird"), Emoji("🦄", "unicorn"), Emoji("🐝", "bee"), Emoji("🦋", "butterfly"),
            Emoji("🐲", "dragon"), Emoji("🌵", "cactus"), Emoji("🌲", "tree"), Emoji("🌿", "leaf plant"), Emoji("🍀", "clover luck"),
            Emoji("🔥", "fire lit"), Emoji("🌈", "rainbow"), Emoji("⭐", "star"), Emoji("🌙", "moon"), Emoji("☀️", "sun"),
            Emoji("❄️", "snow winter"), Emoji("⚡", "lightning electric")
        ),
        Category.FOOD to listOf(
            Emoji("🍎", "apple"), Emoji("🍌", "banana"), Emoji("🍉", "watermelon"), Emoji("🍓", "strawberry"), Emoji("🍒", "cherry"),
            Emoji("🍑", "peach"), Emoji("🍍", "pineapple"), Emoji("🥝", "kiwi"), Emoji("🍔", "burger"), Emoji("🍟", "fries"),
            Emoji("🍕", "pizza"), Emoji("🌭", "hotdog"), Emoji("🌮", "taco"), Emoji("🌯", "burrito"), Emoji("🥪", "sandwich"),
            Emoji("🍗", "chicken meat"), Emoji("🥓", "bacon"), Emoji("🍜", "noodles ramen"), Emoji("🍣", "sushi"), Emoji("🍿", "popcorn"),
            Emoji("🍪", "cookie"), Emoji("🍩", "donut"), Emoji("🍫", "chocolate"), Emoji("🍰", "cake"), Emoji("🧁", "cupcake"),
            Emoji("☕", "coffee"), Emoji("🥤", "drink"), Emoji("🧃", "juice"), Emoji("🍺", "beer"), Emoji("🥂", "cheers"),
            Emoji("🧊", "ice"), Emoji("🍽️", "meal plate")
        ),
        Category.ACTIVITY to listOf(
            Emoji("⚽", "football soccer"), Emoji("🏀", "basketball"), Emoji("🏈", "football american"), Emoji("⚾", "baseball"),
            Emoji("🎾", "tennis"), Emoji("🏐", "volleyball"), Emoji("🎱", "pool billiards"), Emoji("🏓", "ping pong"),
            Emoji("🥊", "boxing"), Emoji("🎯", "target dart"), Emoji("🎮", "gaming controller"), Emoji("🕹️", "joystick gaming"),
            Emoji("🎲", "dice game"), Emoji("♟️", "chess"), Emoji("🏆", "trophy win"), Emoji("🥇", "gold first medal"),
            Emoji("🥈", "silver second medal"), Emoji("🥉", "bronze third medal"), Emoji("🎉", "party celebrate"), Emoji("🎊", "confetti"),
            Emoji("🎵", "music"), Emoji("🎧", "headphones"), Emoji("🎸", "guitar"), Emoji("🎬", "movie"), Emoji("📸", "camera"),
            Emoji("🚀", "rocket"), Emoji("🏎️", "race car"), Emoji("⛏️", "pickaxe minecraft"), Emoji("⚔️", "swords pvp"),
            Emoji("🛡️", "shield"), Emoji("🏹", "bow"), Emoji("🎣", "fishing")
        ),
        Category.MORE to listOf(
            Emoji("❤️", "heart love red"), Emoji("🧡", "heart orange"), Emoji("💛", "heart yellow"), Emoji("💚", "heart green"),
            Emoji("💙", "heart blue"), Emoji("💜", "heart purple"), Emoji("🖤", "heart black"), Emoji("🤍", "heart white"),
            Emoji("💔", "broken heart"), Emoji("💯", "hundred perfect"), Emoji("✅", "check yes done"), Emoji("❌", "cross no"),
            Emoji("⚠️", "warning"), Emoji("❗", "exclamation"), Emoji("❓", "question"), Emoji("‼️", "double exclamation"),
            Emoji("💬", "chat message"), Emoji("📌", "pin"), Emoji("🔒", "lock"), Emoji("🔓", "unlock"),
            Emoji("🔑", "key"), Emoji("💎", "diamond gem"), Emoji("🪙", "coin"), Emoji("💰", "money"),
            Emoji("🎁", "gift"), Emoji("💡", "idea light"), Emoji("🔧", "tool wrench"), Emoji("⚙️", "gear settings"),
            Emoji("📢", "announce megaphone"), Emoji("🔔", "bell notification"), Emoji("📅", "calendar"), Emoji("🧪", "test potion")
        )
    )

    init {
        load()
        EventBus.register<AddTextBarElementEvent>({ -30 }) {
            it.elements.add(EmojiPickerTextBarElement(it.screen))
        }
    }

    fun emojis(category: Category, search: String = ""): List<Emoji> {
        val query = search.trim()
        if (query.isNotEmpty()) {
            return catalog.values.flatten().distinctBy { it.value }.filter {
                it.value.contains(query) || it.keywords.contains(query, ignoreCase = true)
            }
        }
        return when (category) {
            Category.RECENT -> recent.map { Emoji(it, "recent") }
            Category.FAVORITES -> favorites.map { Emoji(it, "favorite") }
            else -> catalog[category].orEmpty()
        }
    }

    fun use(value: String) {
        recent.remove(value)
        recent.add(0, value)
        while (recent.size > 32) recent.removeLast()
        save()
    }

    fun toggleFavorite(value: String) {
        if (!favorites.add(value)) favorites.remove(value)
        save()
    }

    fun isFavorite(value: String): Boolean = value in favorites

    private fun load() {
        val file = stateFile
        if (!file.exists()) return
        runCatching {
            val state = json.decodeFromString(PersistedState.serializer(), file.readText())
            recent.clear()
            recent.addAll(state.recent.take(32))
            favorites.clear()
            favorites.addAll(state.favorites)
        }.onFailure {
            ChatPlus.LOGGER.warn("Could not load BattleChat emoji picker state", it)
        }
    }

    private fun save() {
        runCatching {
            stateFile.parentFile?.mkdirs()
            stateFile.writeText(json.encodeToString(PersistedState.serializer(), PersistedState(recent, favorites.toList())))
        }.onFailure {
            ChatPlus.LOGGER.warn("Could not save BattleChat emoji picker state", it)
        }
    }
}

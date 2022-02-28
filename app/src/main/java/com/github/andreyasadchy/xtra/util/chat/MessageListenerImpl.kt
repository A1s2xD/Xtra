package com.github.andreyasadchy.xtra.util.chat

import com.github.andreyasadchy.xtra.model.chat.Badge
import com.github.andreyasadchy.xtra.model.chat.LiveChatMessage
import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
import kotlin.collections.set

class MessageListenerImpl(
    private val callback: OnChatMessageReceivedListener,
    private val callbackUserState: OnUserStateReceivedListener,
    private val callbackRoomState: OnRoomStateReceivedListener,
    private val callbackCommand: OnCommandReceivedListener,
    private val showUserNotice: Boolean,
    private val showClearMsg: Boolean,
    private val showClearChat: Boolean) : LiveChatThread.OnMessageReceivedListener {

    override fun onMessage(message: String) {
        val parts = message.substring(1).split(" ".toRegex(), 2)
        val prefix = parts[0]
        val prefixes = splitAndMakeMap(prefix, ";", "=")
        val messageInfo = parts[1] //:<user>!<user>@<user>.tmi.twitch.tv PRIVMSG #<channelName> :<message>
        val userName = messageInfo.substring(1, messageInfo.indexOf("!"))
        val userMessage: String
        val isAction: Boolean
        messageInfo.substring(messageInfo.indexOf(":", messageInfo.indexOf(":") + 1) + 1).let { //from <message>
            if (!it.startsWith(ACTION)) {
                userMessage = it
                isAction = false
            } else {
                userMessage = it.substring(8, it.lastIndex)
                isAction = true
            }
        }
        var emotesList: MutableList<TwitchEmote>? = null
        val emotes = prefixes["emotes"]
        if (emotes != null) {
            val entries = splitAndMakeMap(emotes, "/", ":").entries
            emotesList = ArrayList(entries.size)
            entries.forEach { emote ->
                emote.value?.split(",")?.forEach { indexes ->
                    val index = indexes.split("-")
                    emotesList.add(TwitchEmote(emote.key, index[0].toInt(), index[1].toInt()))
                }
            }
        }
        var badgesList: MutableList<Badge>? = null
        val badges = prefixes["badges"]
        if (badges != null) {
            val entries = splitAndMakeMap(badges, ",", "/").entries
            badgesList = ArrayList(entries.size)
            entries.forEach {
                it.value?.let { value ->
                    badgesList.add(Badge(it.key, value))
                }
            }
        }
        callback.onMessage(LiveChatMessage(
            id = prefixes["id"],
            userId = prefixes["user-id"],
            userName = userName,
            displayName = prefixes["display-name"]?.replace("\\s", " "),
            message = userMessage,
            color = prefixes["color"],
            isAction = isAction,
            isReward = prefixes["custom-reward-id"] != null,
            isFirst = prefixes["first-msg"] == "1",
            emotes = emotesList,
            badges = badgesList,
            timestamp = prefixes["tmi-sent-ts"]?.toLong(),
            fullMsg = message
        ))
    }

    override fun onCommand(message: String, duration: String?, type: String?, fullMsg: String?) {
        callbackCommand.onCommand(Command(
            message = message,
            duration = duration,
            type = type,
            fullMsg = fullMsg
        ))
    }

    override fun onClearMessage(message: String) {
        if (showClearMsg) {
            val parts = message.substring(1).split(" ".toRegex(), 2)
            val prefix = parts[0]
            val prefixes = splitAndMakeMap(prefix, ";", "=")
            val user = prefixes["login"]
            val messageInfo = parts[1]
            val msgIndex = messageInfo.indexOf(":", messageInfo.indexOf(":") + 1)
            val msg = if (msgIndex != -1) messageInfo.substring(msgIndex + 1) else null
            callbackCommand.onCommand(Command(
                message = user,
                duration = msg,
                type = "clearmsg",
                timestamp = prefixes["tmi-sent-ts"]?.toLong(),
                fullMsg = message
            ))
        }
    }

    override fun onClearChat(message: String) {
        if (showClearChat) {
            val parts = message.substring(1).split(" ".toRegex(), 2)
            val prefix = parts[0]
            val prefixes = splitAndMakeMap(prefix, ";", "=")
            val duration = prefixes["ban-duration"]
            val messageInfo = parts[1]
            val userIndex = messageInfo.indexOf(":", messageInfo.indexOf(":") + 1)
            val user = if (userIndex != -1) messageInfo.substring(userIndex + 1) else null
            val type = if (user == null) { "clearchat" } else { if (duration != null) { "timeout" } else { "ban" } }
            callbackCommand.onCommand(Command(
                message = user,
                duration = duration,
                type = type,
                timestamp = prefixes["tmi-sent-ts"]?.toLong(),
                fullMsg = message
            ))
        }
    }

    override fun onNotice(message: String) {
        val parts = message.substring(1).split(" ".toRegex(), 2)
        val prefix = parts[0]
        val prefixes = splitAndMakeMap(prefix, ";", "=")
        val messageInfo = parts[1]
        val msgId = prefixes["msg-id"]
        callbackCommand.onCommand(Command(
            message = messageInfo.substring(messageInfo.indexOf(":", messageInfo.indexOf(":") + 1) + 1),
            fullMsg = message
        ))
    }

    override fun onUserNotice(message: String) {
        if (showUserNotice) {
            val parts = message.substring(1).split(" ".toRegex(), 2)
            val prefix = parts[0]
            val prefixes = splitAndMakeMap(prefix, ";", "=")
            val system = prefixes["system-msg"]?.replace("\\s", " ")
            val messageInfo = parts[1]
            val msgIndex = messageInfo.indexOf(":", messageInfo.indexOf(":") + 1)
            val msg = if (msgIndex != -1) messageInfo.substring(msgIndex + 1) else null
            var emotesList: MutableList<TwitchEmote>? = null
            val emotes = prefixes["emotes"]
            if (emotes != null && system != null && msg != null) {
                val entries = splitAndMakeMap(emotes, "/", ":").entries
                emotesList = ArrayList(entries.size)
                entries.forEach { emote ->
                    emote.value?.split(",")?.forEach { indexes ->
                        val index = indexes.split("-")
                        emotesList.add(TwitchEmote(emote.key, index[0].toInt() + system.length + 1, index[1].toInt() + system.length + 1))
                    }
                }
            }
            callbackCommand.onCommand(Command(
                message = if (system != null) if (msg != null) "$system $msg" else system else message,
                emotes = emotesList,
                timestamp = prefixes["tmi-sent-ts"]?.toLong(),
                fullMsg = message
            ))
        }
    }

    override fun onRoomState(message: String) {
        val parts = message.substring(1).split(" ".toRegex(), 2)
        val prefix = parts[0]
        val prefixes = splitAndMakeMap(prefix, ";", "=")
        callbackRoomState.onRoomState(RoomState(
            emote = prefixes["emote-only"],
            followers = prefixes["followers-only"],
            unique = prefixes["r9k"],
            slow = prefixes["slow"],
            subs = prefixes["subs-only"]
        ))
    }

    override fun onUserState(message: String) {
        val parts = message.substring(1).split(" ".toRegex(), 2)
        val prefix = parts[0]
        val prefixes = splitAndMakeMap(prefix, ";", "=")
        val sets = prefixes["emote-sets"]
        var list: List<String>? = null
        if (sets != null && list == null) {
            list = sets.split(",".toRegex()).dropLastWhile { it.isEmpty() }
            callbackUserState.onUserState(list)
        }
    }

    private fun splitAndMakeMap(string: String, splitRegex: String, mapRegex: String): Map<String, String?> {
        val list = string.split(splitRegex.toRegex()).dropLastWhile { it.isEmpty() }
        val map = LinkedHashMap<String, String?>()
        for (pair in list) {
            val kv = pair.split(mapRegex.toRegex()).dropLastWhile { it.isEmpty() }
            map[kv[0]] = if (kv.size == 2) kv[1] else null
        }
        return map
    }

    companion object {
        const val ACTION = "\u0001ACTION"
    }
}

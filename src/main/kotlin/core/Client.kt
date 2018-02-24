package core

import sx.blah.discord.api.ClientBuilder
import sx.blah.discord.api.IDiscordClient
import sx.blah.discord.api.events.EventSubscriber
import sx.blah.discord.handle.impl.events.ReadyEvent
import sx.blah.discord.util.DiscordException
import java.util.*

private val client by lazy {
    val builder = ClientBuilder().withToken(Core.getPrivateKey())
    try {
        builder.login()
    } catch (e: DiscordException) {
        e.printStackTrace()
        throw Exception("Unable to instantiate core.Client")
    }
}

object Client : IDiscordClient by client {
    enum class Status {
        ONLINE,
        IDLE,
        BUSY,
        OFFLINE
    }

    @EventSubscriber
    fun onReadyListener(event: ReadyEvent) {
        try {
            event.client.changeUsername(defaultUserName)
            setStatus(defaultState, defaultStatus)

            Log.plus("Client Ready: Initialized $shardCount shard(s).")
        } catch (e: DiscordException) {
            e.printStackTrace()
        }
    }

    fun registerTimer(timer: Timer) {
        timers.add(timer)
    }

    fun setStatus(status: Status, playingText: String = "") {
        if (playingText != "") {
            when (status) {
                Status.ONLINE -> client.online(playingText)
                Status.IDLE -> client.idle(playingText)
                Status.BUSY -> client.dnd(playingText)
                Status.OFFLINE -> client.invisible()
            }
        } else {
            when (status) {
                Status.ONLINE -> client.online()
                Status.IDLE -> client.idle()
                Status.BUSY -> client.dnd()
                Status.OFFLINE -> client.invisible()
            }
        }
    }

    fun resetStatus() {
        setStatus(defaultState, defaultStatus)
    }

    private var timers = mutableListOf<Timer>()

    private const val defaultUserName = "MonikaBot"
    private val defaultState = Status.IDLE
    private const val defaultStatus = "I'm still learning (>.<)"
}

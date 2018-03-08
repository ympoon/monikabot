package cmds

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import core.*
import core.BuilderHelper.buildEmbed
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.DiscordException
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.concurrent.timer
import kotlin.system.measureTimeMillis

/**
 * Singleton handling "warframe" commands
 */
object Warframe : IBase, IChannelLogger, IConsoleLogger {
    override fun handler(event: MessageReceivedEvent): Parser.HandleState {
        val args = Core.getArgumentList(event.message.content)

        if (args.isEmpty()) {
            return Parser.HandleState.NOT_FOUND
        }

        return when (args[0]) {
            "news" -> getAllNews(event)
            else -> {
                help(event, false)
                Parser.HandleState.HANDLED
            }
        }
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        try {
            buildEmbed(event.channel) {
                withTitle("Help Text for `warframe`")
                withDesc("Wrapper for Warframe-related commands.")
                appendField("\u200B", "\u200B", false)
                appendField("Usage", "```warframe [subcommand] [args]```", false)
                appendField("Subcommand: `news`", "Displays the latest Warframe news, same as the news segment in the orbiter.", false)
            }
        } catch (e: DiscordException) {
            log(IChannelLogger.LogLevel.ERROR, "Cannot display help text") {
                author { event.author }
                channel { event.channel }
                info { e.errorMessage }
            }
        }
    }

    /**
     * Handles "warframe news" subcommand.
     *
     * @param event Event which invoked this function.
     *
     * @return core.Parser.HandleState.HANDLED
     */
    private fun getAllNews(event: MessageReceivedEvent): Parser.HandleState {
        val args = Core.getArgumentList(event.message.content).drop(1)
        if (args.isNotEmpty() && args[0].matches("-{0,2}help".toRegex())) {
            buildEmbed(event.channel) {
                withTitle("Help Text for `warframe-news`")
                withDesc("Displays the latest Warframe news.")
                appendField("\u200B", "\u200B", false)
                appendField("Usage", "```warframe news```", false)
            }

            return Parser.HandleState.HANDLED
        }

        // potentially long operation. toggle typing to show that the bot is loading.
        event.channel.toggleTypingStatus()

        buildEmbed(event.channel) {
            withTitle("Warframe News")

            val timer = measureTimeMillis {
                val events = JsonParser().parse(WorldState.json).asJsonObject.get("Events")
                val eventList = gson.fromJson(events, JsonArray::class.java).asJsonArray
                val eventPairs = mutableMapOf<Duration, String>()

                for (wfEvent in eventList) {
                    val eventJson = wfEvent.asJsonObject
                    val time = gson.fromJson(wfEvent.asJsonObject.get("Date").asJsonObject.get("\$date").asJsonObject.get("\$numberLong"), Long::class.java)

                    for (it in eventJson.get("Messages").asJsonArray) {
                        if (gson.fromJson(it.asJsonObject.get("LanguageCode"), String::class.java) == "en") {
                            val diff = Duration.between(Instant.ofEpochMilli(time), Instant.now())
                            eventPairs[diff] = gson.fromJson(it.asJsonObject.get("Message"), String::class.java)
                            break
                        }
                    }
                }

                val sortedPairs = eventPairs.entries.sortedBy { it.key.seconds }
                sortedPairs.forEach { (k, v) ->
                    val diffString = when {
                        k.toDays() > 0 -> "${k.toDays()}d"
                        k.toHours() > 0 -> "${k.toHours()}h"
                        k.toMinutes() > 0 -> "${k.toMinutes()}m"
                        else -> "${k.seconds}s"
                    }
                    appendDesc("\n[$diffString] $v")
                }
            }

            logger.debug("getAllNews(): JSON parsing took ${timer}ms.")

            withTimestamp(LocalDateTime.ofInstant(WorldState.lastModified, ZoneId.of("UTC")))
        }

        return Parser.HandleState.HANDLED
    }

    /**
     * Updates the world state json. Will be invoked periodically by updateWorldStateTask.
     */
    private fun updateWorldState() {
        if (!Client.isReady) {
            return
        }

        val timer = measureTimeMillis {
            WorldState.json = URL(WorldState.source).readText()
        }
        logger.debug("updateWorldState(): JSON update took ${timer}ms.")

        val time = gson.run {
            val timeElement = JsonParser().parse(WorldState.json).asJsonObject.get("Time")
            gson.fromJson(timeElement, Long::class.java)
        }
        WorldState.lastModified = Instant.ofEpochSecond(time)

        PersistentMessage.modify("Warframe", "WorldState Last Modified", WorldState.lastModified.toString(), true)
    }

    /**
     * Task for updating world state JSON periodically.
     */
    val updateWorldStateTask = timer("Update WorldState Timer", true, 0, 30000) { updateWorldState() }

    /**
     * JSON parser.
     */
    private val gson = Gson()

    /**
     * Singleton for storing world state JSON.
     */
    private object WorldState {
        /**
         * URL to world state.
         */
        const val source = "http://content.warframe.com/dynamic/worldState.php"

        /**
         * When the JSON is last modified server-side.
         */
        var lastModified = Instant.EPOCH!!
        /**
         * JSON string.
         */
        var json = URL(source).readText().also { lastModified = Instant.now() }
    }
}

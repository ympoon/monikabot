package cmds

import IConsoleLogger
import Parser
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonParser
import core.Client
import core.Log
import org.slf4j.LoggerFactory
import popFirstWord
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.MessageBuilder
import java.net.URL
import java.util.*
import kotlin.concurrent.thread
import kotlin.concurrent.timer
import kotlin.system.measureTimeMillis

object Warframe : Base, IConsoleLogger {
    override fun handler(event: MessageReceivedEvent): Parser.HandleState {
        val args = Parser.popLeadingMention(event.message.content).popFirstWord().split("\n")

        return when (args[0]) {
            "news" -> getNews(event)
            else -> Parser.HandleState.UNHANDLED
        }
    }

    override fun handlerSu(event: MessageReceivedEvent): Parser.HandleState {
        return Parser.HandleState.UNHANDLED
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        // not handled
    }

    private fun getNews(event: MessageReceivedEvent): Parser.HandleState {
        val messageId = MessageBuilder(event.client).apply {
            withCode("", "Loading...")
            withChannel(event.channel)
        }.build().longID

        var eventStr = ""
        val timer = measureTimeMillis {
            val events = JsonParser().parse(worldStateText).asJsonObject.get("Events")
            val eventList = gson.fromJson(events, JsonArray::class.java).asJsonArray
            for (wfEvent in eventList) {
                val e = wfEvent.asJsonObject
                val time = gson.fromJson(wfEvent.asJsonObject.get("Date").asJsonObject.get("\$date").asJsonObject.get("\$numberLong"), Long::class.java)
                for (it in e.get("Messages").asJsonArray) {
                    if (gson.fromJson(it.asJsonObject.get("LanguageCode"), String::class.java) == "en") {
                        eventStr += "${Date(time)} - ${gson.fromJson(it.asJsonObject.get("Message"), String::class.java)}\n"
                        break
                    }
                }
            }
        }

        logger.debug("getNews() took ${timer}ms.")

        thread {
            event.client.getMessageByID(messageId).edit("```\n${eventStr.dropLastWhile { it == '\n' }}```")
            logger.debug("getNews() threaded update complete.")
        }

        return Parser.HandleState.HANDLED
    }

    private fun updateWorldState(invokedBy: String = "") {
        logger.info("Invoked updateWorldState() ${if (invokedBy.isNotBlank()) "from $invokedBy" else ""}")

        if (!Client.isReady) {
            return
        }

        val timer = measureTimeMillis {
            worldStateText = URL(worldStateLink).readText()
        }
        logger.debug("updateWorldState() JSON update took ${timer}ms.")

        val time = gson.run {
            val timeElement = JsonParser().parse(worldStateText).asJsonObject.get("Time")
            gson.fromJson(timeElement, Long::class.java) * 1000
        }
        WorldState.lastModified = Date(time)
        val currentTime = Date()

        Log.modifyPersistent("Warframe", "WorldState Last Modified", WorldState.lastModified.toString())
        Log.modifyPersistent("Misc", "Last Updated", currentTime.toString(), true)
    }

    var updateWorldStateTask = timer("Update WorldState Timer", true, 0, 30000) { updateWorldState() }

    private const val worldStateLink = "http://content.warframe.com/dynamic/worldState.php"
    private var worldStateText = URL(worldStateLink).readText()
    private val gson = Gson()

    override val logger = LoggerFactory.getLogger(this::class.java)!!

    object WorldState {
        var lastModified = Date()
    }
}

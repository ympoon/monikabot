package cmds.warframe

import cmds.IBase
import cmds.Warframe
import core.BuilderHelper.buildEmbed
import core.BuilderHelper.buildMessage
import core.IChannelLogger
import core.Parser
import insertSeparator
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.DiscordException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import java.util.*

object Cetus : IBase, IChannelLogger {
    override fun handler(event: MessageReceivedEvent): Parser.HandleState {
        val args = getArgumentList(event.message.content).toMutableList().apply {
            removeIf { it.matches(Regex("cetus")) }
        }

        try {
            when {
                args.any { it.matches(Regex("-{0,2}help")) } -> help(event, false)
                args.isEmpty() -> getBounties(event)
                args[0] == "time" -> getTime(event)
                else -> {
                    help(event, false)
                }
            }
        } catch (e: Exception) {
            buildMessage(event.channel) {
                withContent("Warframe is currently updating its information. Please be patient!")
            }

            log(IChannelLogger.LogLevel.ERROR, e.message ?: "Unknown Exception")
        }

        return Parser.HandleState.HANDLED
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        try {
            buildEmbed(event.channel) {
                withTitle("Help Text for `warframe-cetus`")
                withDesc("Displays Cetus-related information.")
                insertSeparator()
                appendField("Usage", "```warframe cetus [time]```", false)
                appendField("`timer`", "If appended, show the current time in Cetus/Plains.", false)
            }
        } catch (e: DiscordException) {
            log(IChannelLogger.LogLevel.ERROR, "Cannot display help text") {
                author { event.author }
                channel { event.channel }
                info { e.errorMessage }
            }
            e.printStackTrace()
        }
    }

    private fun getBounties(event: MessageReceivedEvent) {
        val cetusInfo = Warframe.worldState.syndicateMissions.find { it.tag == "CetusSyndicate" }
                ?: throw Exception("Cannot find Cetus information")
        val timeLeft = Duration.between(Instant.now(), cetusInfo.expiry.date.numberLong)
        val timeLeftString = formatTimeDuration(timeLeft)

        val bounties = cetusInfo.jobs

        bounties.forEachIndexed { i, v ->
            buildEmbed(event.channel) {
                withAuthorName("Cetus Bounties - Tier ${i + 1}")
                withTitle(WorldState.getLanguageFromAsset(v.jobType))

                appendField("Mastery Requirement", v.masteryReq.toString(), false)
                appendField("Enemy Level", "${v.minEnemyLevel}-${v.maxEnemyLevel}", true)
                appendField("Total Standing Reward", v.xpAmounts.sum().toString(), true)

                appendField("Expires in", timeLeftString, false)
                withTimestamp(Instant.now())
            }
        }
    }

    private fun getTime(event: MessageReceivedEvent) {
        val cetusCycleStart = Warframe.worldState.syndicateMissions.find { it.tag == "CetusSyndicate" }?.activation?.date?.numberLong
                ?: throw Exception("Cannot find Cetus information")
        val cetusCycleEnd = Warframe.worldState.syndicateMissions.find { it.tag == "CetusSyndicate" }?.expiry?.date?.numberLong
                ?: throw Exception("Cannot find Cetus information")

        val cetusTimeLeft = run {
            if (Duration.between(Instant.now(), cetusCycleEnd.minus(50, ChronoUnit.MINUTES)).seconds <= 0) {
                Pair(true, Duration.between(Instant.now(), cetusCycleEnd))
            } else {
                Pair(false, Duration.between(Instant.now(), cetusCycleEnd.minus(50, ChronoUnit.MINUTES)))
            }
        }
        val cetusNextDayTime = dateTimeFormatter.format(cetusCycleEnd)
        val cetusNextNightTime = if (!cetusTimeLeft.first) {
            dateTimeFormatter.format(cetusCycleEnd.minus(50, ChronoUnit.MINUTES))
        } else {
            dateTimeFormatter.format(cetusCycleEnd.plus(100, ChronoUnit.MINUTES))
        }
        val cetusNextStateString = if (!cetusTimeLeft.first) "Day" else "Night"
        val timeString = formatTimeDuration(cetusTimeLeft.second)

        val cetusDayCycleTime = Duration.between(cetusCycleStart, cetusCycleEnd)
        val dayLengthString = formatTimeDuration(cetusDayCycleTime)

        buildEmbed(event.channel) {
            withTitle("Cetus Time")
            appendField("Current Time", "$cetusNextStateString - $timeString remaining", false)
            appendField("Next Day Time", "$cetusNextDayTime UTC", true)
            appendField("Next Night Time", "$cetusNextNightTime UTC", true)
            if (dayLengthString.isNotBlank()) {
                appendField("Day Cycle Length", dayLengthString, false)
            }
            withTimestamp(Instant.now())
        }
    }
    
    private fun formatTimeDuration(duration: Duration): String {
        return (if (duration.toDays() > 0) "${duration.toDays()}d " else "") +
                (if (duration.toHours() % 24 > 0) "${duration.toHours() % 24}h " else "") +
                (if (duration.toMinutes() % 60 > 0) "${duration.toMinutes() % 60}m " else "") +
                "${duration.seconds % 60}s"
    }

    private val dateTimeFormatter = DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withLocale(Locale.ENGLISH)
            .withZone(ZoneId.of("UTC"))
}
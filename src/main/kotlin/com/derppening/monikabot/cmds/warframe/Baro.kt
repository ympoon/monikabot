/*
 *  This file is part of MonikaBot.
 *
 *  Copyright (C) 2018 Derppening <david.18.19.21@gmail.com>
 *
 *  MonikaBot is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  MonikaBot is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MonikaBot.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package com.derppening.monikabot.cmds.warframe

import com.derppening.monikabot.cmds.IBase
import com.derppening.monikabot.cmds.Warframe
import com.derppening.monikabot.cmds.Warframe.formatDuration
import com.derppening.monikabot.core.ILogger
import com.derppening.monikabot.core.Parser
import com.derppening.monikabot.models.warframe.worldstate.WorldState
import com.derppening.monikabot.util.BuilderHelper.buildEmbed
import com.derppening.monikabot.util.BuilderHelper.buildMessage
import com.derppening.monikabot.util.BuilderHelper.insertSeparator
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import java.time.Duration
import java.time.Instant

object Baro : IBase, ILogger {
    override fun handler(event: MessageReceivedEvent): Parser.HandleState {
        val args = getArgumentList(event.message.content).drop(1)

        val baro = try {
            Warframe.worldState.voidTraders.first()
        } catch (e: NoSuchElementException) {
            buildMessage(event.channel) {
                withContent("Unable to retrieve Baro Ki'Teer information! Please try again later.")
            }

            return Parser.HandleState.HANDLED
        }

        event.channel.toggleTypingStatus()
        buildEmbed(event.channel) {
            withTitle("Baro Ki'Teer Information")

            if (baro.manifest.isEmpty()) {
                val nextTimeDuration = Duration.between(Instant.now(), baro.activation.date.numberLong)
                appendField("Time to Next Appearance", nextTimeDuration.formatDuration(), true)
                appendField("Relay", WorldState.getSolNode(baro.node).value, true)
            } else {
                val expiryTimeDuration = Duration.between(Instant.now(), baro.expiry.date.numberLong)
                appendField("Time Left", expiryTimeDuration.formatDuration(), false)
                baro.manifest.forEach {
                    val item = WorldState.getLanguageFromAsset(it.itemType).let { fmt ->
                        if (fmt.isEmpty()) {
                            it.itemType
                        } else {
                            fmt
                        }
                    }
                    val ducats = it.primePrice
                    val credits = it.regularPrice
                    appendField(item, "$ducats Ducats - $credits Credits", true)
                }
            }

            withTimestamp(Instant.now())
        }

        return Parser.HandleState.HANDLED
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        buildEmbed(event.channel) {
            withTitle("Help Text for `warframe-baro`")
            withDesc("Displays information about Baro Ki'Teer.")
            insertSeparator()
            appendField("Usage", "```warframe baro```", false)

            onDiscordError { e ->
                log(ILogger.LogLevel.ERROR, "Cannot display help text") {
                    author { event.author }
                    channel { event.channel }
                    info { e.errorMessage }
                }
            }
        }
    }
}
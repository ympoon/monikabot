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

package com.derppening.monikabot.commands

import com.derppening.monikabot.core.Client
import com.derppening.monikabot.core.ILogger
import com.derppening.monikabot.core.Parser
import com.derppening.monikabot.impl.PingService.getEmbed
import com.derppening.monikabot.util.helpers.EmbedHelper.buildEmbed
import com.derppening.monikabot.util.helpers.HelpTextBuilder.buildHelpText
import com.derppening.monikabot.util.helpers.insertSeparator
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import java.time.Instant

object Ping : IBase, ILogger {
    override fun handler(event: MessageReceivedEvent): Parser.HandleState {
        val args = getArgumentList(event.message.content)
        if (args.isNotEmpty() && args.any { it.matches(Regex("-{0,2}help")) }) {
            help(event, false)

            return Parser.HandleState.HANDLED
        }

        event.channel.typingStatus = true
        buildEmbed(event.channel) {
            fields {
                withTitle("Bot Latency Information")

                appendField("Discord Server", "${Client.shards.first().responseTime}ms", false)

                getEmbed().also {
                    appendField("DigitalOcean Servers", it.digitalOcean, false)
                    appendField("DNS Servers", it.dns, false)
                }

                withTimestamp(Instant.now())
            }
        }

        return Parser.HandleState.HANDLED
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        buildEmbed(event.channel) {
            fields {
                withTitle("Help Text for `ping`")
                withDesc("Displays the current latency of the bot to various servers.")
                insertSeparator()
                appendField("Usage", "```ping```", false)
            }

            onError {
                discordException { e ->
                    log(ILogger.LogLevel.ERROR, "Cannot display help text") {
                        author { event.author }
                        channel { event.channel }
                        info { e.errorMessage }
                    }
                }
            }
        }

        buildHelpText("ping", event) {
            description { "Displays the current latency of the bot to various servers." }

            usage("ping")
        }
    }
}
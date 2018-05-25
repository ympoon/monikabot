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

import com.derppening.monikabot.core.ILogger
import com.derppening.monikabot.core.Parser
import com.derppening.monikabot.impl.ChangelogService.getAll
import com.derppening.monikabot.impl.ChangelogService.getLatest
import com.derppening.monikabot.util.helpers.EmbedHelper.buildEmbed
import com.derppening.monikabot.util.helpers.HelpTextBuilder.buildHelpText
import com.derppening.monikabot.util.helpers.MessageHelper.buildMessage
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Changelog : IBase, ILogger {
    override fun cmdName(): String = "changelog"

    override fun handler(event: MessageReceivedEvent): Parser.HandleState {
        val args = getArgumentList(event.message.content)

        val showRelease = args.any { it.matches(Regex("-{0,2}release")) }
        val showAllChanges = args.any { it.matches(Regex("-{0,2}all")) }

        if (showAllChanges) {
            outputAllChanges(event, showRelease)
        } else {
            outputLatestChanges(event, showRelease)
        }

        return Parser.HandleState.HANDLED
    }


    private fun outputAllChanges(event: MessageReceivedEvent, showRel: Boolean) {
        val displayChanges = getAll(showRel)

        buildEmbed(event.channel) {
            fields {
                withTitle("Last 5 Changelogs")
                if (displayChanges.isEmpty()) {
                    withDesc("There are no official releases (yet)!")
                } else {
                    displayChanges.forEach { (ver, changetext) ->
                        appendField(ver, changetext.joinToString("\n"), false)
                    }
                }
            }
        }
    }

    private fun outputLatestChanges(event: MessageReceivedEvent, showRel: Boolean) {
        val displayChange = getLatest(showRel) ?: run {
            buildMessage(event.channel) {
                content {
                    withContent("There are no official releases (yet)!")
                }
            }
            return
        }

        buildEmbed(event.channel) {
            fields {
                withTitle("Changelog for ${displayChange.first}")
                withDesc(displayChange.second.joinToString("\n"))
            }
        }
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        buildHelpText("changelog", event) {
            description { "Displays the changelog for the most recent build(s)." }

            usage("changelog [release] [all]") {
                def("release") { "Only show changes for release builds" }
            }
        }
    }
}
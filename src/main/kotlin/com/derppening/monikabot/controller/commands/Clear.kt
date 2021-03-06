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

package com.derppening.monikabot.controller.commands

import com.derppening.monikabot.controller.CommandInterpreter
import com.derppening.monikabot.core.ILogger
import com.derppening.monikabot.impl.ClearService
import com.derppening.monikabot.impl.ClearService.clearChannel
import com.derppening.monikabot.util.helpers.HelpTextBuilder.buildHelpText
import com.derppening.monikabot.util.helpers.MessageHelper.buildMessage
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Clear : IBase, ILogger {
    override fun cmdName(): String = "clear"

    override fun handlerSu(event: MessageReceivedEvent): CommandInterpreter.HandleState {
        val args = getArgumentList(event.message.content)

        val allFlag = args.any { it.matches(Regex("-{0,2}all")) }

        when (clearChannel(event.channel, allFlag)) {
            ClearService.Result.FAILURE_PRIVATE_CHANNEL -> {
                buildMessage(event.channel) {
                    content {
                        withContent("I can't delete clear messages in private channels!")
                    }
                }
            }
            else -> {
            }
        }

        return CommandInterpreter.HandleState.HANDLED
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        buildHelpText(cmdInvocation(), event) {
            description {
                "Clears all channel messages that are younger than 14 days." +
                        "\nThis command does not work in private channels."
            }

            usage("[--all]") {
                flag("all") { "Deletes all messages from the channel, not only ones which are locally cached." }
            }
        }
    }
}

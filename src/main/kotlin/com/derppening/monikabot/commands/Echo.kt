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
import com.derppening.monikabot.impl.EchoService
import com.derppening.monikabot.impl.EchoService.toGuildChannel
import com.derppening.monikabot.impl.EchoService.toPrivateChannel
import com.derppening.monikabot.util.BuilderHelper.buildEmbed
import com.derppening.monikabot.util.BuilderHelper.buildMessage
import com.derppening.monikabot.util.BuilderHelper.insertSeparator
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Echo : IBase, ILogger {
    override fun handler(event: MessageReceivedEvent): Parser.HandleState {
        val args = if (event.channel.isPrivate) {
            getArgumentList(event.message.content)
        } else {
            getArgumentList(event.message.formattedContent, event.channel.guild)
        }
        if (args.isEmpty()) {
            help(event, false)
            return Parser.HandleState.HANDLED
        }

        if (args[0] == "-d" || args[0] == "--destination") {
            if (args.size == 1) {
                buildMessage(event.channel) {
                    withContent("Please specify a destination and a message!")
                }

                return Parser.HandleState.HANDLED
            }

            return if (args[1].any { it == '/' }) {
                messageToGuildChannel(args, event)
                Parser.HandleState.HANDLED
            } else {
                Parser.HandleState.PERMISSION_DENIED
            }
        }

        val message = getArgumentList(event.message.content).joinToString(" ")

        buildMessage(event.channel) {
            withContent(message)

            onDiscordError { e ->
                log(ILogger.LogLevel.ERROR, "Cannot echo message \"$message\"!") {
                    message { event.message }
                    author { event.author }
                    channel { event.channel }
                    info { e.errorMessage }
                    stackTrace { e.stackTrace }
                }
            }
        }

        return Parser.HandleState.HANDLED
    }

    override fun handlerSu(event: MessageReceivedEvent): Parser.HandleState {
        val args = if (event.channel.isPrivate) {
            getArgumentList(event.message.content)
        } else {
            getArgumentList(event.message.formattedContent, event.channel.guild)
        }
        if (args.isEmpty()) {
            help(event, true)
            return Parser.HandleState.HANDLED
        }

        if (args[0] == "-d" || args[0] == "--destination") {
            return when {
                args.size == 1 -> {
                    buildMessage(event.channel) {
                        withContent("Please specify a destination and a message!")
                    }
                    Parser.HandleState.HANDLED
                }
                args[1].any { it == '/' } -> // delegate to normal handler
                    Parser.HandleState.UNHANDLED
                else -> {
                    messageToPrivateChannel(args, event)
                    Parser.HandleState.HANDLED
                }
            }
        }

        return Parser.HandleState.UNHANDLED
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        buildEmbed(event.channel) {
            withTitle("Help Text for `echo`")
            withDesc("Echo: Repeats a string.")
            insertSeparator()
            appendField("Usage", "```echo [string]```", false)
            appendField("`[string]`", "String to repeat.", false)

            insertSeparator()
            appendField("Usage", "```echo -d [destination] [string]```", false)

            val destinationText = "Destination of the string. Recognized formats include:" +
                    "\n\t- `/channel`: Sends to `channel` in current server." +
                    if (isSu) {
                        "\n\t- `server/channel`: Sends to `channel` in `server`." +
                                "\n\t- `username#discriminator`: Sends to user with this Discord Tag."
                    } else ""
            appendField("`[destination]`", destinationText, false)
            appendField("`[string]`", "String to repeat.", false)

            onDiscordError { e ->
                log(ILogger.LogLevel.ERROR, "Cannot display help text") {
                    author { event.author }
                    channel { event.channel }
                    info { e.errorMessage }
                }
            }
        }
    }

    private fun messageToPrivateChannel(args: List<String>, event: MessageReceivedEvent) {
        val result = toPrivateChannel(args)
        when (result) {
            is EchoService.Result.Failure -> {
                buildMessage(event.channel) {
                    withContent(result.message)
                }
            }
            else -> {}
        }
    }

    private fun messageToGuildChannel(args: List<String>, event: MessageReceivedEvent) {
        val result = toGuildChannel(args, event)
        when (result) {
            is EchoService.Result.Failure -> {
                buildMessage(event.channel) {
                    withContent(result.message)
                }
            }
            else -> {}
        }
    }
}
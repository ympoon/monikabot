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
import com.derppening.monikabot.impl.ConfigService
import com.derppening.monikabot.impl.ConfigService.configureOwnerEchoFlag
import com.derppening.monikabot.impl.ConfigService.ownerModeEchoForSu
import com.derppening.monikabot.util.helpers.EmbedHelper.buildEmbed
import com.derppening.monikabot.util.helpers.HelpTextBuilder.buildHelpText
import com.derppening.monikabot.util.helpers.MessageHelper.buildMessage
import com.derppening.monikabot.util.helpers.insertSeparator
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Config : IBase, ILogger {
    override fun cmdName(): String = "config"

    override fun handlerSu(event: MessageReceivedEvent): Parser.HandleState {
        val args = getArgumentList(event.message.content)

        if (args.isEmpty()) {
            help(event, true)
            return Parser.HandleState.HANDLED
        }

        when (args[0]) {
            "owner_echo_for_su" -> {
                ownerModeEchoHandler(args, event)
            }
        }

        return Parser.HandleState.HANDLED
    }

    /**
     * Handler for "config owner_echo_for_su" commands.
     *
     * @param args List of arguments.
     * @param event Event of the original message.
     */
    private fun ownerModeEchoHandler(args: List<String>, event: MessageReceivedEvent) {
        when (configureOwnerEchoFlag(args)) {
            ConfigService.Result.GET -> {
                buildMessage(event.channel) {
                    content {
                        withContent("Owner Mode Echo for Superusers: ${if (ownerModeEchoForSu) "Allow" else "Deny"}.")
                    }
                }
            }
            ConfigService.Result.SET -> {
                buildMessage(event.channel) {
                    content {
                        withContent("Owner Mode Echo for Superusers now ${if (ownerModeEchoForSu) "allowed" else "denied"}.")
                    }
                }
            }
            ConfigService.Result.HELP -> {
                buildHelpText("config-owner_echo_for_su", event) {
                    description { "Whether to allow superusers access to owner mode `echo`." }

                    usage("config owner_echo_for_su [allow|deny]") {
                        def("[allow|deny]") { "Allows or denies owner mode echo for superusers." }
                    }
                }
            }
        }
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        buildEmbed(event.channel) {
            fields {
                withTitle("Help Text for `config`")
                withDesc("Core configurations for MonikaBot.")
                insertSeparator()
                appendField("Usage", "```config [configuration] [options...]```", false)
                appendField("Configuration: `owner_echo_for_su`",
                        "Whether to allow superusers to access owner mode echo, i.e. allowing echoing to any channel.",
                        false)
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
    }
}
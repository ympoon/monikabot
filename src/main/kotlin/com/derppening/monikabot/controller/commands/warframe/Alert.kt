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

package com.derppening.monikabot.controller.commands.warframe

import com.derppening.monikabot.controller.CommandInterpreter
import com.derppening.monikabot.controller.commands.IBase
import com.derppening.monikabot.core.ILogger
import com.derppening.monikabot.impl.warframe.AlertService.getAlertEmbeds
import com.derppening.monikabot.impl.warframe.AlertService.getGoalEmbeds
import com.derppening.monikabot.util.helpers.EmbedHelper.sendEmbed
import com.derppening.monikabot.util.helpers.HelpTextBuilder.buildHelpText
import com.derppening.monikabot.util.helpers.MessageHelper.buildMessage
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent

object Alert : IBase, ILogger {
    override fun cmdName(): String = "warframe-alert"
    override fun cmdInvocationAlias(): List<String> = listOf("warframe alerts")

    override fun handler(event: MessageReceivedEvent): CommandInterpreter.HandleState {
        val args = getArgumentList(event.message.content).drop(1)

        try {
            when {
                args.isEmpty() -> {
                    getGoals(event)
                    getAlerts(event)
                }
                args[0].endsWith("alert") -> getAlerts(event)
                args[0].endsWith("special") -> getGoals(event, true)
                else -> {
                    help(event, false)
                }
            }
        } catch (e: Exception) {
            buildMessage(event.channel) {
                content {
                    withContent("Warframe is currently updating its information. Please be patient!")
                }
            }
            e.printStackTrace()

            logToChannel(ILogger.LogLevel.ERROR, e.message ?: "Unknown Exception")
        }

        return CommandInterpreter.HandleState.HANDLED
    }

    private fun getAlerts(event: MessageReceivedEvent) {
        getAlertEmbeds().also {
            if (it.isEmpty()) {
                buildMessage(event.channel) {
                    content {
                        withContent("There are currently no alerts!")
                    }
                }
            }
        }.forEach {
            sendEmbed(it to event.channel)
        }
    }

    private fun getGoals(event: MessageReceivedEvent, isDirectlyInvoked: Boolean = false) {
        getGoalEmbeds().also {
            if (isDirectlyInvoked && it.isEmpty()) {
                buildMessage(event.channel) {
                    content {
                        withContent("There are currently no special alerts!")
                    }
                }
            }
        }.forEach {
            sendEmbed(it to event.channel)
        }
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        buildHelpText(cmdInvocation(), event) {
            description { "Displays all currently ongoing alerts." }

            usage("[--alert|--special]") {
                flag("alert") { "Only show normal mission alerts." }
                flag("special") { "Only show special alerts." }
            }
        }
    }
}
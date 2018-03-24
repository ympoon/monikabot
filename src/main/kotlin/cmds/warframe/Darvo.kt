/**
 * This file is part of MonikaBot.
 *
 * Copyright (C) 2018 Derppening <david.18.19.21@gmail.com>
 *
 * MonikaBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MonikaBot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MonikaBot.  If not, see <http://www.gnu.org/licenses/>.
 */

package cmds.warframe

import cmds.IBase
import cmds.Warframe
import core.BuilderHelper.buildEmbed
import core.BuilderHelper.buildMessage
import core.BuilderHelper.insertSeparator
import core.IChannelLogger
import core.Parser
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import java.time.Duration
import java.time.Instant

object Darvo : IBase, IChannelLogger {
    override fun handler(event: MessageReceivedEvent): Parser.HandleState {
        val args = getArgumentList(event.message.content).drop(1)
        if (args.isNotEmpty() && args.any { it.matches(Regex("-{0,2}help")) }) {
            help(event, false)

            return Parser.HandleState.HANDLED
        }

        val darvoDeal = try {
            Warframe.worldState.dailyDeals.first()
        } catch (e: NoSuchElementException) {
            buildMessage(event.channel) {
                withContent("Darvo currently has no items on sale!")
            }

            return Parser.HandleState.HANDLED
        }
        buildEmbed(event.channel) {
            withAuthorName("Darvo Sale")
            withTitle(WorldState.getLanguageFromAsset(darvoDeal.storeItem))

            appendField("Time Left", formatTimeDuration(Duration.between(Instant.now(), darvoDeal.expiry.date.numberLong)), false)
            appendField("Price", "${darvoDeal.originalPrice} -> ${darvoDeal.salePrice}", true)
            appendField("Discount", "${darvoDeal.discount}%", true)
            if (darvoDeal.amountSold == darvoDeal.amountTotal) {
                appendField("Amount Left", "Sold Out", false)
            } else {
                appendField("Amount Left", "${darvoDeal.amountTotal - darvoDeal.amountSold}/${darvoDeal.amountTotal}", false)
            }

            val imageRegex = Regex(darvoDeal.storeItem.takeLastWhile { it != '/' } + '$')
            withImage(Manifest.findImageByRegex(imageRegex))

            withTimestamp(Instant.now())
        }

        return Parser.HandleState.HANDLED
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        buildEmbed(event.channel) {
            withTitle("Help Text for `warframe-darvo`")
            withDesc("Displays the ongoing Darvo sale.")
            insertSeparator()
            appendField("Usage", "```warframe darvo```", false)

            onDiscordError { e ->
                log(IChannelLogger.LogLevel.ERROR, "Cannot display help text") {
                    author { event.author }
                    channel { event.channel }
                    info { e.errorMessage }
                }
            }
        }
    }

    /**
     * Formats a duration.
     */
    private fun formatTimeDuration(duration: Duration): String {
        return (if (duration.toDays() > 0) "${duration.toDays()}d " else "") +
                (if (duration.toHours() % 24 > 0) "${duration.toHours() % 24}h " else "") +
                (if (duration.toMinutes() % 60 > 0) "${duration.toMinutes() % 60}m " else "") +
                "${duration.seconds % 60}s"
    }
}
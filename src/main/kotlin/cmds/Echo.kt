package cmds

import Parser
import core.Core
import core.Log
import popFirstWord
import removeQuotes
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MessageBuilder

object Echo : Base {
    override fun handler(event: MessageReceivedEvent) {
        if (Core.isEventFromAdmin(event)) {
            handlerSudo(event)
        }

        if (event.message.content.split(" ")[1] == "--help") {
            help(event, false)
            return
        }

        val channel = event.channel
        val message = Parser.popLeadingMention(event.message.content).popFirstWord()

        if (!event.channel.isPrivate) {
            try {
                event.message.delete()
            } catch (e: DiscordException) {
                Log.minus("ECHO: Cannot delete \"$message\".\n" +
                        "\tFrom ${Core.getDiscordTag(event.author)}\n" +
                        "\tIn \"${Core.getChannelId(event.channel)}\"\n" +
                        "\tReason: ${e.errorMessage}")
                e.printStackTrace()
            }
        }

        try {
            MessageBuilder(event.client).apply {
                withChannel(channel)
                withContent(message.removeQuotes())
            }.build()
        } catch (e: DiscordException) {
            Log.minus("ECHO: \"$message\" not handled.\n" +
                    "\tFrom ${Core.getDiscordTag(event.author)}\n" +
                    "\tIn \"${Core.getChannelId(event.channel)}\"" +
                    "\t Reason: ${e.errorMessage}")
            e.printStackTrace()
        }
    }

    override fun handlerSudo(event: MessageReceivedEvent): Boolean {
        return false
    }

    override fun help(event: MessageReceivedEvent, isSu: Boolean) {
        try {
            MessageBuilder(event.client).apply {
                withChannel(event.channel)
                withCode("","Repeats a user-defined string.")
            }.build()
        } catch (e: DiscordException) {
            Log.minus("ECHO: Cannot display help text.\n" +
                    "\tInvoked by ${Core.getDiscordTag(event.author)}\n" +
                    "\tIn \"${Core.getChannelId(event.channel)}\"" +
                    "\tReason: ${e.errorMessage}")
            e.printStackTrace()
        }
    }
}

package cmds

import core.Log
import getChannelId
import getDiscordTag
import popFirstWord
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageReceivedEvent
import sx.blah.discord.util.DiscordException
import sx.blah.discord.util.MessageBuilder

//    private fun adminMessage(event: MessageReceivedEvent) {
//        when (event.message.content.takeWhile { it != ' ' }) {
//            "kill" -> {
//                val message = event.message.content.popFirstWord()
//
//                MessageBuilder(event.client).apply {
//                    withChannel(event.channel)
//                    withCode("py", "print(\"$message\")")
//                }.build()
//            }
//            "status" -> adminChangeStatus(event)
//        }
//    }

object Echo : Base {
    override fun handler(event: MessageReceivedEvent) {
        if (!event.message.content.startsWith(event.client.ourUser.mention(false))) return

        val channel = event.channel
        val message = event.message.content.popFirstWord()

        try {
            MessageBuilder(event.client).apply {
                withChannel(channel)
                withContent(message)
            }.build()
        } catch (e: DiscordException) {
            Log.minus("Message \"${event.message.content}\" not handled.\n" +
                    "\tFrom ${getDiscordTag(event.author)}\n" +
                    "\tIn \"${getChannelId(event.channel)}\"")
            e.printStackTrace()
        }
    }

    override fun handlerSudo(event: MessageReceivedEvent): Boolean {
        return false
    }
}

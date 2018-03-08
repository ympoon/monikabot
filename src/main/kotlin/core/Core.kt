package core

import popFirstWord
import sx.blah.discord.handle.impl.events.guild.channel.message.MessageEvent
import sx.blah.discord.handle.obj.IChannel
import sx.blah.discord.handle.obj.IPrivateChannel
import sx.blah.discord.handle.obj.IUser
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

object Core {
    /**
     * Whether action is performed in a superuser channel (currently only in PM or MonikaBot/debug)
     */
    fun isOwnerLocationValid(event: MessageEvent) =
            event.channel == serverDebugChannel || event.channel == ownerPrivateChannel
    /**
     * @return Whether event is from the bot owner.
     */
    fun isEventFromOwner(event: MessageEvent) =
            event.author.longID == ownerId
    /**
     * @return Whether event is from a superuser.
     */
    fun isEventFromSuperuser(event: MessageEvent) = suIds.any { it == event.author.longID }

    fun popLeadingMention(message: String): String {
        return if (message.startsWith(Client.ourUser.mention(false))) {
            message.popFirstWord()
        } else {
            message
        }
    }

    /**
     * @return List of arguments.
     */
    fun getArgumentList(str: String): List<String> {
        val cmdStr = popLeadingMention(str)
        val tokens = cmdStr.split(" ").drop(1).joinToString(" ")
        val list = mutableListOf<String>()

        var parseQuote = false
        var s = ""
        tokens.forEach {
            when {
                it == '\"' -> {
                    if (parseQuote) {
                        if (s.isNotBlank()) list.add(s)
                        s = ""
                    }
                    parseQuote = !parseQuote
                }
                it == ' ' && !parseQuote -> {
                    if (s.isNotBlank()) list.add(s)
                    s = ""
                }
                else -> s += it
            }
        }
        if (s.isNotBlank()) list.add(s)

        return list.toList()
    }

    /**
     * @return Discord tag.
     */
    fun getDiscordTag(user: IUser): String = "${user.name}#${user.discriminator}"

//    fun getUserId(username: String, discriminator: Int): IUser {
//        return Persistence.client.getUsersByName(username).find { it.discriminator == discriminator.toString() }
//    }

//    fun getGuildByName(name: String): IGuild {
//        return Persistence.client.guilds.find { it.name == name }
//    }

//    fun getChannelByName(name: String, guild: IGuild): IChannel {
//        return guild.channels.find { it.name == name }
//    }

    /**
     * @return Channel name in "Server/Channel" format.
     */
    fun getChannelName(channel: IChannel): String {
        val guild = if (channel is IPrivateChannel) "[Private]" else channel.guild.name
        return "$guild/${channel.name}"
    }

    /**
     * Performs a full reload of the bot.
     */
    fun reload() {
        loadVersion()
        loadSuIds()

        thread {
            PersistentMessage.modify("Misc", "Version", monikaVersion, true)
        }
    }

    /**
     * Loads the bot's version and returns itself.
     */
    private fun loadVersion(): String {
        monikaVersion = "${loadSemVersion()}+$monikaVersionBranch"
        return monikaVersion
    }

    /**
     * Loads the bot's SemVer portion of version and returns itself.
     */
    private fun loadSemVersion(): String {
        monikaSemVersion = getProperties(VERSION_PROP).getProperty("version")!!
        return monikaSemVersion
    }

    /**
     * Loads superuser IDs and returns itself.
     */
    private fun loadSuIds(): Set<Long> {
        suIds = getProperties(SOURCE_PROP)
                .getProperty("suId")
                .split(',')
                .map { it.toLong() }
                .union(listOf(ownerId))
        return suIds
    }

    /**
     * Gets the method name which invoked this method.
     */
    fun getMethodName(): String {
        return Thread.currentThread().stackTrace[2].methodName + "(?)"
    }

    /**
     * Loads a property object based on a file. Application will terminate if file cannot be found.
     *
     * @param filename Filename of properties file to load.
     *
     * @return Properties object of loaded file.
     */
    private fun getProperties(filename: String): Properties {
        try {
            return Properties().apply {
                val relpath = "properties/$filename"
                load(FileInputStream(File(Thread.currentThread().contextClassLoader.getResource(relpath).toURI())))
            }
        } catch (ioException: FileNotFoundException) {
            println("Cannot find properties file")
            ioException.printStackTrace()

            exitProcess(0)
        }
    }

    /**
     * Filename of source.properties.
     */
    private const val SOURCE_PROP = "source.properties"
    /**
     * Filename of version.properties.
     */
    private const val VERSION_PROP = "version.properties"

    /**
     * PM Channel of bot admin.
     */
    val ownerPrivateChannel: IPrivateChannel by lazy { Client.fetchUser(ownerId).orCreatePMChannel }
    /**
     * Debug channel.
     */
    val serverDebugChannel: IChannel? by lazy { Client.getChannelByID(serverDebugChannelId) }
    /**
     * Bot private key.
     */
    val privateKey = getProperties(SOURCE_PROP).getProperty("privateKey")!!

    /**
     * SemVer version of the bot.
     */
    var monikaSemVersion = loadSemVersion()
        private set
    /**
     * The git branch of this bot.
     */
    val monikaVersionBranch = getProperties(VERSION_PROP).getProperty("gitbranch")!!
    /**
     * Version of the bot.
     */
    var monikaVersion = loadVersion()
        private set

    /**
     * ID of bot admin.
     */
    private val ownerId = getProperties(SOURCE_PROP).getProperty("adminId").toLong()
    /**
     * IDs for bot superusers.
     */
    private var suIds = loadSuIds()
    /**
     * ID of Debug channel.
     */
    private val serverDebugChannelId = getProperties(SOURCE_PROP).getProperty("debugChannelId").toLong()
}

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

package com.derppening.monikabot.impl.warframe

import com.derppening.monikabot.core.Core
import com.derppening.monikabot.core.ILogger
import com.derppening.monikabot.impl.WarframeService.worldState
import com.derppening.monikabot.models.warframe.worldstate.WorldState
import com.derppening.monikabot.util.ChronoHelper.formatDuration
import sx.blah.discord.api.internal.json.objects.EmbedObject
import sx.blah.discord.util.EmbedBuilder
import java.time.Duration
import java.time.Instant

object SortieService : ILogger {
    fun isSortieInWorldState(): Boolean = worldState.sorties.isNotEmpty()

    fun getSortie(): WorldState.Sorties {
        if (worldState.sorties.size > 1) {
            fix("worldState[\"voidTraders\"] has more than 1 entry!", Core.getMethodName())
        }

        return worldState.sorties.first()
    }

    fun WorldState.Sorties.toEmbed(): EmbedObject {
        return EmbedBuilder().apply {
            val boss = WorldState.getSortieBoss(boss)
            withTitle("Sortie Information")

            appendField("Expires in", Duration.between(Instant.now(), expiry.date.numberLong).formatDuration(), false)
            appendField("Boss", "${boss.name} (${boss.faction})", false)
            variants.forEachIndexed { i, m ->
                val missionType = WorldState.getMissionType(m.missionType)
                val modifier = WorldState.getSortieModifier(m.modifierType)
                val node = WorldState.getSolNode(m.node)
                appendField("Mission ${i + 1} - $missionType on ${node.value}", "Modifier: ${modifier.type}", true)
            }

            withTimestamp(Instant.now())
        }.build()
    }
}
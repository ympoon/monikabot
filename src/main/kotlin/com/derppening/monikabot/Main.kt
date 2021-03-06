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

package com.derppening.monikabot

import com.derppening.monikabot.controller.CommandInterpreter
import com.derppening.monikabot.core.Client
import com.derppening.monikabot.impl.WarframeService

object Main {
    private fun setupDispatchers() {
        // core
        Client.dispatcher.registerListener(Client)
        Client.dispatcher.registerListener(CommandInterpreter)
    }

    private fun setupTimers() {
        Client.registerTimer(WarframeService.updateDropTablesTask)
        Client.registerTimer(WarframeService.updateWorldStateTask)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        setupDispatchers()
        setupTimers()
    }
}
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

import com.derppening.monikabot.core.ILogger
import com.derppening.monikabot.models.warframe.prime.PrimeInfo
import com.derppening.monikabot.util.helpers.toNearestChronoYear
import java.io.File
import java.nio.file.Paths
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

object PrimeService : ILogger {
//    private const val primeFilePath = "resources/primes.csv"
    private val primesFile = Paths.get("resources/primes.csv").toUri()

    private val allInfo
        get() = readFromFile().filterNot { it.name == "Excalibur" }
    private val primes = allInfo.filter { it.primeDate != null }.sortedBy { it.primeDate?.epochSecond ?: 0 }
    private val nonprimes = allInfo.filter { it.primeDate == null }.sortedBy { it.date?.epochSecond ?: 0 }

    fun getReleasedPrimesStr(size: Int): List<String> {
        val released = primes.takeLast(size)

        return released.mapIndexed { i, it ->
            val content = "\n\t- ${it.name}"
            val duration = if (i != released.lastIndex) {
                Duration.between(it.primeDate, released[i + 1].primeDate).toDays()
            } else {
                0
            }

            "$content ${if (i != released.lastIndex) "(Lasted for $duration days)" else ""}"
        }
    }

    private fun getReleasedPrimes(size: Int): List<PrimeInfo> {
        return primes.takeLast(size)
    }

    fun getPredictedPrimesStr(size: Int): List<String> {
        var time = getReleasedPrimes(size).last().primeDate ?: error("Primes should have a prime date.")
        val male = getPredictedPrimes(size).filter { it.gender.toUpperCase() == 'M' }.sortedBy {
            it.date?.epochSecond ?: 0
        }.toMutableList()
        val female = getPredictedPrimes(size).filter { it.gender.toUpperCase() == 'F' }.sortedBy {
            it.date?.epochSecond ?: 0
        }.toMutableList()

        val currentPrimes = primes.subList(primes.size - 2, primes.size).toMutableList()
        val predictedStr = mutableListOf<String>()
        while (male.isNotEmpty() || female.isNotEmpty()) {
            time = time.plus(90, ChronoUnit.DAYS)

            val gender = currentPrimes[currentPrimes.size - 2].gender.toUpperCase()
            when {
                gender == 'M' && female.isNotEmpty() || gender == 'F' && male.isEmpty() -> {
                    currentPrimes.add(female[0])
                    female.removeAt(0)
                }
                else -> {
                    currentPrimes.add(male[0])
                    male.removeAt(0)
                }
            }
            val durationToPrime = Duration.between(Instant.now(), time)
            val durationStr = durationToPrime.toNearestChronoYear()
            predictedStr.add("\n\t- ${currentPrimes.last().name} (In ~$durationStr)")
        }

        return predictedStr.toList()
    }

    private fun getPredictedPrimes(size: Int): List<PrimeInfo> {
        return nonprimes.take(size)
    }

    private fun readFromFile(): List<PrimeInfo> {
        val lines = File(primesFile).also { check(it.exists()) }.readLines()

        return lines.map {
            val props = it.split(',')
            check(props.size == 5)
            PrimeInfo(props[0],
                    props[1][0],
                    props[2].toLongOrNull() ?: 0L,
                    props[3].toLongOrNull() ?: 0,
                    props[4].toLongOrNull() ?: 0)
        }
    }
}
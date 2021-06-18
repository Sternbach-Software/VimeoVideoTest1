package com.mycompany.vimeo.contributors

import contributors.createVimeoService
import contributors.matchesVideoConstraint
import kotlinx.coroutines.runBlocking
import tasks.loadVideosChannels

fun main() {
    val x = createVimeoService()
    runBlocking {
        loadVideosChannels(x, 480316302..480316304) { a, b ->
            println(a)
            println(a.title.matchesVideoConstraint())
        }
    }
}
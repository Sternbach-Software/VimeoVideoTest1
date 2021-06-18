package com.mycompany.vimeo.contributors

import contributors.Video
import kotlinx.coroutines.*
import tasks.*
import java.awt.event.ActionListener
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.coroutines.CoroutineContext

enum class Variant {
    SYNCHRONOUS,      // Request1Blocking-loadContributorsURL
//    BLOCKING,         // Request1Blocking
//    BACKGROUND,       // Request2Background
//    CALLBACKS,        // Request3Callbacks
//    SUSPEND,          // Request4Coroutine
//    CONCURRENT,       // Request5Concurrent
//    NOT_CANCELLABLE,  // Request6NotCancellable
//    PROGRESS,         // Request6Progress
ASYNCHRONOUS          // Request7Channels
}


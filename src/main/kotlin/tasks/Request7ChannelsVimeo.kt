package tasks

import contributors.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Response
import okhttp3.ResponseBody

suspend fun loadVideosChannels(
    service: VimeoService,
    ids: IntRange,
    updateResults: suspend (Video, completed: Boolean) -> Unit
) = coroutineScope {
   val channel = Channel<Video>()
    for (id in ids) {
        launch {
            val user = service.getVideo(id).parseVideo(id)
            channel.send(user)
        }
        println("Coroutine made: $id")
    }
    repeat(ids.last) {
        updateResults(channel.receive(), it == ids.last -1)
    }
}


private fun <T> retrofit2.Response<T>.parseVideo(id: Int): Video {
    println("Video processed:     $id")
    val body = body() as ResponseBody?
        ?: //        println("Body was null.")
        return Video(0,"")
    val string = body.string()
    return Video(id, string.substringBetween("<title>", "</title>"))
}

fun String.substringBetween(str1: String, str2: String): String {
    val index1 = indexOf(str1)
    return substring(index1 + str1.length, indexOf(str2,index1))
}

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
    updateResults: suspend (List<Video>, completed: Boolean) -> Unit
) = coroutineScope {
   val channel = Channel<Video>()
    for (id in ids) {
        launch {
            val user = service.getVideo(id).parseVideo(id)
            channel.send(user)
        }
    }
    val allVideos = mutableListOf<Video>()
    repeat(ids.last) {
        val video = channel.receive()
        allVideos.add(video)
        updateResults(allVideos, it == ids.last)
    }
}

private fun <T> retrofit2.Response<T>.parseVideo(id: Int): Video {
    val body = body() as ResponseBody?
    if(body == null) {
        println("Body was null.")
        return Video(0,"")
    }
    val string = body.string()
    println("Response recieved: $string")
    return Video(id, string.substringBetween("<title>", "</title>"))
}

fun String.substringBetween(str1: String, str2: String): String {
    val index1 = indexOf(str1)
    return substring(index1 + str1.length, indexOf(str2,index1))
}

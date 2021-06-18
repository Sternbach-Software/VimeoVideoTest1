package contributors

import contributors.Contributors.LoadingStatus.*
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
val setOfVideos = mutableSetOf<Video>()
interface Contributors: CoroutineScope {

    val job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    fun init() {
        // Start a new loading on 'load' click
        addLoadListener {
//            saveParams()
            loadVideos()
        }

        // Save preferences and exit on closing the window
        addOnWindowClosingListener {
            job.cancel()
//            saveParams()
            System.exit(0)
        }

        // Load stored params (user & password values)
//        loadInitialParams()
    }

    fun loadVideos(start: Int = getStartAndEnd().first, end: Int = getStartAndEnd().second) {
        val vimeoService = createVimeoService()
        val startTime = System.currentTimeMillis()
        launch(Dispatchers.Default) {
            loadVideosChannels(vimeoService, start..end) { video, completed ->
                withContext(Dispatchers.IO) {
                    updateResults(video, startTime, completed)
                }
            }
        }.setUpCancellation()
    }

    private enum class LoadingStatus { COMPLETED, CANCELED, IN_PROGRESS }

    private fun clearResults() {
        updateContributors(listOf())
        updateLoadingStatus(IN_PROGRESS)
        setActionsStatus(newLoadingEnabled = false)
    }

 @JvmName("updateResults1")
 private fun updateResults(
     video: Video,
     startTime: Long,
     completed: Boolean = true,
 ) {
     if(video.title.matchesVideoConstraint()){
         setOfVideos.add(video)
         updateVideos(video)
         updateLoadingStatus(if (completed) COMPLETED else IN_PROGRESS, startTime)
     }
     if (completed) {
         setActionsStatus(newLoadingEnabled = true)
     }
 }
    fun writeVideosToFile(){
        val outputFile = File("Results.txt").apply{createNewFile()}.toPath()
        outputFile.writeLinesToFile(setOfVideos.map { "$baseUrl${it.id}" })
    }
    // append lines of text
    @Throws(IOException::class)
    fun Path.appendLinesToFile(list: List<String>) {
        Files.write(
            this, list,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND
        )
    }
    // rewrite text
    @Throws(IOException::class)
    fun Path.writeLinesToFile(list: List<String>) {
        Files.write(
            this, list,
            StandardOpenOption.CREATE,
        )
    }
    private fun updateLoadingStatus(
        status: LoadingStatus,
        startTime: Long? = null,
    ) {
        val time = if (startTime != null) {
            val time = (System.currentTimeMillis() - startTime).also{elapsed->
                print("Speed:                       ${counter++/(elapsed / 1000.0)} videos/sec")
            }
            "${(time / 1000)}.${time % 1000 / 100} sec"
        } else ""

        val text = "Loading status: " +
                when (status) {
                    COMPLETED -> "completed in $time"
                    IN_PROGRESS -> "in progress $time"
                    CANCELED -> "canceled"
                }
        setLoadingStatus(text, status == IN_PROGRESS)
        if(status == COMPLETED){
            File("Time to complete.txt").apply{
                createNewFile()
                appendText("$time to complete\n")
            }
        }
    }

    private fun Job.setUpCancellation() {
        // make active the 'cancel' button
        setActionsStatus(newLoadingEnabled = false, cancellationEnabled = true)

        val loadingJob = this

        // cancel the loading job if the 'cancel' button was clicked
        val listener = ActionListener {
            loadingJob.cancel()
            updateLoadingStatus(CANCELED)
        }
        addCancelListener(listener)

        // update the status and remove the listener after the loading job is completed
        launch {
            loadingJob.join()
            setActionsStatus(newLoadingEnabled = true)
            removeCancelListener(listener)
        }
    }

    var counter: Int
        get() = 0
        set(value) = TODO()


    private fun updateResults(
        users: List<User>,
        startTime: Long,
        completed: Boolean = true
    ) {
        updateContributors(users)
        updateLoadingStatus(if (completed) COMPLETED else IN_PROGRESS, startTime)
        if (completed) {
            setActionsStatus(newLoadingEnabled = true)
        }
    }

    fun loadInitialParams() {
        setParams(loadStoredParams())
    }

    fun saveParams() {
        val params = getParams()
        if (params.username.isEmpty() && params.password.isEmpty()) {
            removeStoredParams()
        }
        else {
            saveParams(params)
        }
    }

    fun getSelectedVariant(): Variant

    fun updateContributors(users: List<User>)


    fun setLoadingStatus(text: String, iconRunning: Boolean)

    fun setActionsStatus(newLoadingEnabled: Boolean, cancellationEnabled: Boolean = false)

    fun addCancelListener(listener: ActionListener)

    fun removeCancelListener(listener: ActionListener)

    fun addLoadListener(listener: () -> Unit)

    fun addOnWindowClosingListener(listener: () -> Unit)

    fun setParams(params: Params)

    fun getParams(): Params

    fun updateVideos(users: Video){}

    fun getStartAndEnd():Pair<Int,Int>{return Pair(1,1)}
}

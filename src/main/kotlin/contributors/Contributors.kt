package contributors

import contributors.Contributors.LoadingStatus.*
import contributors.Variant.*
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
enum class ShmulyVarient{SYNCHRONOUS, ASYCNCHRONOUS}
val setOfVideos = mutableSetOf<Video>()
interface Contributors: CoroutineScope {

    val job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.IO

    fun init() {
        // Start a new loading on 'load' click
        addLoadListener {
            saveParams()
            loadContributors()
        }

        // Save preferences and exit on closing the window
        addOnWindowClosingListener {
            job.cancel()
            saveParams()
            System.exit(0)
        }

        // Load stored params (user & password values)
//        loadInitialParams()
    }

    fun loadContributors() {
        val (username, password, org, _) = getParams()
        val req = RequestData(username, password, org)
        val (start, end) = getStartAndEnd()
        clearResults()
        val service = createGitHubService(req.username, req.password)
        val vimeoService = createVimeoService()

        val startTime = System.currentTimeMillis()
        when (getSelectedVariant()) {
            SYNCHRONOUS -> {
                val videos = loadVideosURL(start..end)
                updateResults(videos, startTime)
            }
           /* BLOCKING -> { // Blocking UI thread
                val users = loadContributorsBlocking(service, req)
                updateResults(users, startTime)
            }
            BACKGROUND -> { // Blocking a background thread
                loadContributorsBackground(service, req) { users ->
                    SwingUtilities.invokeLater {
                        updateResults(users, startTime)
                    }
                }
            }
            CALLBACKS -> { // Using callbacks
                loadContributorsCallbacks(service, req) { users ->
                    SwingUtilities.invokeLater {
                        updateResults(users, startTime)
                    }
                }
            }
            SUSPEND -> { // Using coroutines
                launch {
                    val users = loadContributorsSuspend(service, req)
                    updateResults(users, startTime)
                }.setUpCancellation()
            }
            CONCURRENT -> { // Performing requests concurrently
                launch {
                    val users = loadContributorsConcurrent(service, req)
                    updateResults(users, startTime)
                }.setUpCancellation()
            }
            NOT_CANCELLABLE -> { // Performing requests in a non-cancellable way
                launch {
                    val users = loadContributorsNotCancellable(service, req)
                    updateResults(users, startTime)
                }.setUpCancellation()
            }
            PROGRESS -> { // Showing progress
                launch(Dispatchers.Default) {
                    loadContributorsProgress(service, req) { users, completed ->
                        withContext(Dispatchers.Main) {
                            updateResults(users, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }*/
            ASYNCHRONOUS -> {  // Performing requests concurrently and showing progress
              /*  launch(Dispatchers.Default) {
                    loadContributorsChannels(service, req) { users, completed ->
                        withContext(Dispatchers.Main) {
                            updateResults(users, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }*/
                launch(Dispatchers.Default) {
                    loadVideosChannels(vimeoService,start..end) { videos, completed ->
                        withContext(Dispatchers.IO) {
                            updateResults(videos, startTime, completed)
                        }
                    }
                }.setUpCancellation()
            }
        }
    }

    private enum class LoadingStatus { COMPLETED, CANCELED, IN_PROGRESS }

    private fun clearResults() {
        updateContributors(listOf())
        updateLoadingStatus(IN_PROGRESS)
        setActionsStatus(newLoadingEnabled = false)
    }

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
 @JvmName("updateResults1")
 private fun updateResults(
     videos: List<Video>,
     startTime: Long,
     completed: Boolean = true,
 ) {
     val filtered = videos.filter { it.title.matchesVideoConstraint() }
     updateVideos(filtered)
     updateLoadingStatus(if (completed) COMPLETED else IN_PROGRESS, startTime, videos.size)
     println()
     if (completed) {
         setActionsStatus(newLoadingEnabled = true)
     }
     setOfVideos.apply {
         addAll(filtered)
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
    var counter: Int
        get() = 0
        set(value) = TODO()

    private fun updateLoadingStatus(
        status: LoadingStatus,
        startTime: Long? = null,
        videoSize: Int? = null
    ) {
        val time = if (startTime != null) {
            val time = (System.currentTimeMillis() - startTime).also{elapsed->
                videoSize?.let{/*if(counter++ % 10 == 0)*/ print("Speed:                       ${it.toLong()/(elapsed / 1000)} videos/sec")}
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

    fun updateVideos(users: List<Video>){}

    fun setLoadingStatus(text: String, iconRunning: Boolean)

    fun setActionsStatus(newLoadingEnabled: Boolean, cancellationEnabled: Boolean = false)

    fun addCancelListener(listener: ActionListener)

    fun removeCancelListener(listener: ActionListener)

    fun addLoadListener(listener: () -> Unit)

    fun addOnWindowClosingListener(listener: () -> Unit)

    fun setParams(params: Params)

    fun getParams(): Params

    fun getStartAndEnd():Pair<Int,Int>{return Pair(1,1)}
}

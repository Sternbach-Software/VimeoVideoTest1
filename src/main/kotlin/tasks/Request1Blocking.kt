package tasks

import contributors.*
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

val outputSynch = File("ResultsSynchronous.txt").apply{createNewFile()}.toPath()
fun loadVideosURL(ids: IntRange): List<Video> {
    val list = mutableListOf<Video>()
    for (id in ids) {
        try {
            val html = URL("$baseUrl$id").readText()
            list.add(
                Video(
                    id,
                    html.substringBetween("<title>", "</title>")
                ).also {
                    println("Video: $it")
                    outputSynch.appendLinesToFile(listOf("$baseUrl${it.id}"))
                }
            )
        } catch (t: Throwable){}
    }
    return list
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
fun loadContributorsURL(req: RequestData): List<User> {
    val rawHTML = URL("https://api.github.com/orgs/${req.org}/repos?per_page=100").readText().also { logRepos(req, it) }
    val regex = "}},\\{".toRegex()
    println("Regex found ${regex.findAll(rawHTML).toList().size} times")
    val repos = rawHTML.split(regex)
    println("size: ${repos.size} repos: $repos")
    val mapped = repos.map {
        Repo(
            ("""(?<="id":)\d+""".toRegex()).find(it)?.value?.toLong() ?: 0,
            """(?<="name":")\w+(?=","full)""".toRegex().find(it)?.value ?: "kotlin-benchmarks"
        )
    }
    return mapped.flatMap { repo ->
        /*:https://api.github.com/repos/Kotlin/kotlin-benchmarks*/
        val rawHTML = URL("https://api.github.com/repos/${req.org}/${repo.name}/contributors?per_page=100").readText()
//            .also { log.info("${repo.name}: loaded ${it.size} contributors") }
        rawHTML.split(regex).map {
            User(
                """(?<="login": ")[A-Za-z]+""".toRegex().find(it)?.value ?: "null",
                """(?<="contributions": ")\\d+""".toRegex().find(it)?.value?.toInt() ?: 0
            )
        }
    }.aggregate()
}

fun loadContributorsBlocking(service: GitHubService, req: RequestData): List<User> {
    val repos = service
        .getOrgReposCall(req.org)
        .execute() // Executes request and blocks the current thread
        .also { logRepos(req, it) }
        .body() ?: listOf()

    return repos.flatMap { repo ->
        service
            .getRepoContributorsCall(req.org, repo.name)
            .execute() // Executes request and blocks the current thread
            .also { logUsers(repo, it) }
            .bodyList()
    }.aggregate()
}

fun <T> Response<List<T>>.bodyList(): List<T> {
    return body() ?: listOf()
}
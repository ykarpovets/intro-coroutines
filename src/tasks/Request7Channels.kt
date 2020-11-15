package tasks

import contributors.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

suspend fun loadContributorsChannels(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    coroutineScope {
        val repos = service.getOrgRepos(req.org)
                .also { logRepos(req, it) }
                .body() ?: listOf()

        val channel = Channel<List<User>>(repos.size)
        repos.map { repo ->
            launch {
                channel.send(
                        service
                                .getRepoContributors(req.org, repo.name)
                                .also { logUsers(repo, it) }
                                .bodyList()
                )
            }
        }
        var loadedUsers = emptyList<User>()
        repeat(repos.size) {
            val users = channel.receive()
            loadedUsers = (loadedUsers + users).aggregate()
            updateResults(loadedUsers, it == repos.lastIndex)
        }
    }
}

package tasks

import contributors.*
import kotlinx.coroutines.*
import kotlin.coroutines.coroutineContext

suspend fun loadContributorsNotCancellable(service: GitHubService, req: RequestData): List<User> {
    return GlobalScope.async {
        val repos = service.getOrgRepos(req.org)
                .also { logRepos(req, it) }
                .body() ?: listOf()

        repos.map { repo ->
            GlobalScope.async {
                service
                        .getRepoContributors(req.org, repo.name)
                        .also { logUsers(repo, it) }
                        .bodyList()
            }
        }.awaitAll().flatten().aggregate()
    }.await()
}
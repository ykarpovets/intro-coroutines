package tasks

import contributors.*

suspend fun loadContributorsProgress(
    service: GitHubService,
    req: RequestData,
    updateResults: suspend (List<User>, completed: Boolean) -> Unit
) {
    val repos = service.getOrgRepos(req.org)
            .also { logRepos(req, it) }
            .body() ?: listOf()

    var loadedUsers = emptyList<User>()
    repos.forEachIndexed { index, repo ->
        val users = service
                .getRepoContributors(req.org, repo.name)
                .also { logUsers(repo, it) }
                .bodyList()
        loadedUsers = (loadedUsers + users).aggregate()
        updateResults(loadedUsers, index == repos.lastIndex)
    }
}

package tasks

import contributors.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger

fun loadContributorsCallbacks(service: GitHubService, req: RequestData, updateResults: (List<User>) -> Unit) {
    service.getOrgReposCall(req.org).onResponse { responseRepos ->
        logRepos(req, responseRepos)
        val repos = responseRepos.bodyList()
        val allUsers = ConcurrentLinkedQueue<User>()
        val countDownLatch = CountDownLatch(repos.size)
        for (repo in repos) {
            service.getRepoContributorsCall(req.org, repo.name).onComplete({ responseUsers ->
                logUsers(repo, responseUsers)
                val users = responseUsers.bodyList()
                allUsers += users
                countDownLatch.countDown()
            }, {
                log.error("Call failed", it)
                countDownLatch.countDown()
            })
        }
        countDownLatch.await()
        updateResults(allUsers.toList().aggregate())
    }
}

inline fun <T> Call<T>.onResponse(crossinline responseCallback: (Response<T>) -> Unit) {
    onComplete({ responseCallback(it)}, { log.error("Call failed", it)})
}

inline fun <T> Call<T>.onComplete(crossinline responseCallback: (Response<T>) -> Unit, crossinline failedCallback: (Throwable) -> Unit) {
    enqueue(object : Callback<T> {
        override fun onResponse(call: Call<T>, response: Response<T>) {
            responseCallback(response)
        }

        override fun onFailure(call: Call<T>, t: Throwable) {
            failedCallback(t)
        }
    })
}

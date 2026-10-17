package com.neteinstein.networth.core.database

import com.neteinstein.networth.core.domain.repository.AppClock
import com.neteinstein.networth.core.domain.repository.AuthRepository
import com.neteinstein.networth.core.domain.repository.AuthState
import com.neteinstein.networth.core.domain.repository.IdGenerator
import com.neteinstein.networth.core.domain.repository.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.datetime.LocalDate

class FakeRemote : DatabaseRemote {
    class Problem(val access: Boolean) : Exception("remote problem")

    val files = mutableMapOf<String, String>()
    val versions = mutableMapOf<String, Int>()
    val shares = mutableListOf<Pair<String, String>>()
    var uploads = 0
    var failWith: Problem? = null
    private var nextId = 1

    private fun check() {
        failWith?.let { throw it }
    }

    private fun fileInfo(id: String) = DatabaseRemote.FileInfo(id, "file-$id.networth.json", versions[id]?.toString(), "https://drive/$id")

    /** Simulates another device writing the file. */
    fun externalWrite(id: String, content: String) {
        files[id] = content
        versions[id] = (versions[id] ?: 0) + 1
    }

    override suspend fun create(name: String, content: String): DatabaseRemote.FileInfo {
        check()
        val id = "f${nextId++}"
        externalWrite(id, content)
        return fileInfo(id).copy(name = name)
    }

    override suspend fun info(fileId: String): DatabaseRemote.FileInfo {
        check()
        if (fileId !in files) throw Problem(access = true)
        return fileInfo(fileId)
    }

    override suspend fun download(fileId: String): String {
        check()
        return files[fileId] ?: throw Problem(access = true)
    }

    override suspend fun update(fileId: String, content: String): DatabaseRemote.FileInfo {
        check()
        uploads++
        externalWrite(fileId, content)
        return fileInfo(fileId)
    }

    override suspend fun share(fileId: String, email: String) {
        check()
        shares += fileId to email
    }

    override fun isAccessProblem(error: Throwable) = error is Problem && error.access
}

class FakeAuth(email: String = "ana@example.com") : AuthRepository {
    override val state: StateFlow<AuthState> = MutableStateFlow(AuthState.SignedIn(UserProfile("1", email, "Ana")))

    override suspend fun restore() = Unit

    override suspend fun signIn() = (state.value as AuthState.SignedIn).user

    override suspend fun pickDatabaseFile(): String? = null

    override suspend fun signOut() = Unit
}

class FakeClock(var now: Long = 1_000) : AppClock {
    override fun nowMillis(): Long = now++

    override fun today(): LocalDate = LocalDate(2026, 10, 1)
}

class SequentialIds(private val prefix: String = "id") : IdGenerator {
    private var next = 1

    override fun newId(): String = "$prefix-${next++}"
}

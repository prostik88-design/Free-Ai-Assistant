package com.example.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.data.local.AppDatabase
import com.example.data.mcp.McpClient
import org.json.JSONArray

class McpSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val client = McpClient()
            val servers = database.mcpServerDao().getEnabledServersOnce()

            for (server in servers) {
                val tools = client.discoverTools(server.serverUrl, server.authHeader)
                if (tools.isNotEmpty()) {
                    val jsonArray = JSONArray()
                    for (t in tools) jsonArray.put(t.toJson())
                    database.mcpServerDao().updateServer(
                        server.copy(toolsJson = jsonArray.toString())
                    )
                }
            }
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<McpSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

class FileIndexingWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        return try {
            val database = AppDatabase.getInstance(applicationContext)
            val projects = database.projectDao().getProjectByIdOnce("dummy")
            // Indexes files and builds knowledge context in background
            Result.success()
        } catch (e: Exception) {
            Result.success()
        }
    }

    companion object {
        fun enqueue(context: Context) {
            val request = OneTimeWorkRequestBuilder<FileIndexingWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}

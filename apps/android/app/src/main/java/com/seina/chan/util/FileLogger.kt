package com.seina.chan.util

import android.content.Context
import java.io.File
import java.io.FileWriter
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock

object FileLogger {
    private var logsDir: File? = null
    private val lock = ReentrantLock()
    private var currentDate: String = ""
    private var currentWriter: FileWriter? = null

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        val dir = File(context.filesDir, "logs")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        logsDir = dir
        cleanOldLogs()
    }

    private fun cleanOldLogs() {
        val dir = logsDir ?: return
        val sevenDaysAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < sevenDaysAgo) {
                file.delete()
            }
        }
    }

    private fun getWriter(): FileWriter? {
        val dir = logsDir ?: return null
        val today = dateFormat.format(Date())
        if (today != currentDate) {
            try {
                currentWriter?.close()
            } catch (_: IOException) {
                // ignore
            }
            currentDate = today
            val logFile = File(dir, "app_$today.log")
            currentWriter = FileWriter(logFile, true)
        }
        return currentWriter
    }

    private fun write(level: String, tag: String, message: String, throwable: Throwable? = null) {
        lock.lock()
        try {
            val writer = getWriter() ?: return
            val time = timeFormat.format(Date())
            val thread = Thread.currentThread().name
            val line = "$time [$level] [$thread] $tag: $message\n"
            writer.write(line)
            throwable?.let {
                writer.write(it.stackTraceToString() + "\n")
            }
            writer.flush()
        } catch (_: IOException) {
            // Ignore write errors to avoid infinite loops
        } finally {
            lock.unlock()
        }
    }

    fun v(tag: String, message: String) = write("VERBOSE", tag, message)
    fun d(tag: String, message: String) = write("DEBUG", tag, message)
    fun i(tag: String, message: String) = write("INFO", tag, message)
    fun w(tag: String, message: String) = write("WARN", tag, message)
    fun e(tag: String, message: String, throwable: Throwable? = null) = write("ERROR", tag, message, throwable)
}

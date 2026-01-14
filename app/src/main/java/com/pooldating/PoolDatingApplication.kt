package com.pooldating

import android.app.Application
import android.util.Log
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter

class PoolDatingApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        
        // Set up global exception handler to catch crashes
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            // Log the crash
            val sw = StringWriter()
            exception.printStackTrace(PrintWriter(sw))
            val stackTrace = sw.toString()
            
            Log.e("CRASH_HANDLER", "=== UNCAUGHT EXCEPTION ===")
            Log.e("CRASH_HANDLER", "Thread: ${thread.name}")
            Log.e("CRASH_HANDLER", "Exception: ${exception.javaClass.simpleName}")
            Log.e("CRASH_HANDLER", "Message: ${exception.message}")
            Log.e("CRASH_HANDLER", "Stack Trace:\n$stackTrace")
            
            // Write to file for easy access
            try {
                val crashFile = File(getExternalFilesDir(null), "crash_log.txt")
                crashFile.writeText("=== CRASH LOG ===\n")
                crashFile.appendText("Time: ${System.currentTimeMillis()}\n")
                crashFile.appendText("Exception: ${exception.javaClass.simpleName}\n")
                crashFile.appendText("Message: ${exception.message}\n\n")
                crashFile.appendText("Stack Trace:\n$stackTrace")
                Log.e("CRASH_HANDLER", "Crash log saved to: ${crashFile.absolutePath}")
            } catch (e: Exception) {
                Log.e("CRASH_HANDLER", "Failed to write crash log", e)
            }
            
            // Call default handler to show crash dialog/exit
            defaultHandler?.uncaughtException(thread, exception)
        }
        
        Log.d("PoolDatingApp", "Application initialized with crash handler")
    }
}

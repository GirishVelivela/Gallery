package com.v19.gal.sync

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.HandlerThread
import android.provider.MediaStore
import java.lang.ref.WeakReference
import java.util.Collections

class MediaObserver private constructor(private val context: WeakReference<Context>) {

    private val observerCallbacks: MutableList<ObserverCallback> =
        Collections.synchronizedList(mutableListOf<ObserverCallback>())

    private val backgroundThread = HandlerThread("ContentObserverThread").apply { start() }
    val handler = Handler(backgroundThread.looper)

    val runnable = Runnable {
        for (observerCallback in observerCallbacks) {
            observerCallback.onChange(false)
        }
    }

    private val contentObserver by lazy {
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                handler.removeCallbacks(runnable)
                handler.postDelayed(runnable, 500)
            }
        }
    }

    @Synchronized
    fun register(observerCallback: ObserverCallback) {
        if (observerCallbacks.isEmpty()) {
            observerCallbacks.add(observerCallback)
            context.get()?.contentResolver?.registerContentObserver(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                contentObserver
            )
        } else {
			observerCallbacks.add(observerCallback)
		}
    }

    @Synchronized
    fun unregister(observerCallback: ObserverCallback) {
        observerCallbacks.remove(observerCallback)
        if (observerCallbacks.isEmpty()) {
            context.get()?.contentResolver?.unregisterContentObserver(
                contentObserver
            )
        }
    }

    companion object {
        @Volatile
        private var instance: MediaObserver? = null

        fun getInstance(context: Context): MediaObserver {
            return instance ?: synchronized(this) {
                instance ?: MediaObserver(WeakReference(context.applicationContext)).also {
                    instance = it
                }
            }
        }
    }
}

interface ObserverCallback {
    fun onChange(selfChange: Boolean)
}
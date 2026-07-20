package com.merino.ddfilms.configuration

import com.merino.ddfilms.api.FirebaseManager
import com.merino.ddfilms.utils.TaskCompletionCallback

class ApiKeyManager private constructor() {

    var apiKey: String? = null
        private set

    private var isApiKeyFetched = false
    private val firebaseManager = FirebaseManager()

    fun fetchApiKey(callback: TaskCompletionCallback<String>) {
        if (isApiKeyFetched && apiKey != null) {
            callback.onComplete(apiKey, null)
            return
        }

        firebaseManager.getTmdbApiKey { result, error ->
            if (error != null) {
                callback.onComplete(null, error)
            } else {
                apiKey = result
                isApiKeyFetched = true
                callback.onComplete(apiKey, null)
            }
        }
    }

    companion object {
        private var instance: ApiKeyManager? = null

        @JvmStatic
        @Synchronized
        fun getInstance(): ApiKeyManager {
            if (instance == null) {
                instance = ApiKeyManager()
            }
            return instance!!
        }
    }
}

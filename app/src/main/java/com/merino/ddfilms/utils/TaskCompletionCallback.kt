package com.merino.ddfilms.utils

fun interface TaskCompletionCallback<T> {
    fun onComplete(result: T?, e: Exception?)
}

package com.bakeflow.app.data.remote

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestoreException

object FirestoreListenerHelper {
    private const val TAG = "BakeFlowFirestore"

    fun isMissingCompositeIndex(error: Exception): Boolean =
        error is FirebaseFirestoreException &&
            error.code == FirebaseFirestoreException.Code.FAILED_PRECONDITION

    fun logMissingIndex(error: FirebaseFirestoreException, collection: String, queryDescription: String) {
        Log.w(
            TAG,
            "Composite index required for $collection ($queryDescription): ${error.message}"
        )
    }
}

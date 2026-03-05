package com.app.azkary.util

import android.util.Log

class NoOpAppUpdateManager : AppUpdateManager {
    override fun checkForUpdate() {
        Log.d(TAG, "In-app updates not available on F-Droid")
    }
    
    override fun onActivityResult(requestCode: Int, resultCode: Int) {}
    override fun onResume() {}
    override fun onDestroy() {}
    
    companion object {
        private const val TAG = "NoOpAppUpdateManager"
    }
}

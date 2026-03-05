package com.app.azkary.util

interface AppUpdateManager {
    fun checkForUpdate()
    fun onActivityResult(requestCode: Int, resultCode: Int)
    fun onResume()
    fun onDestroy()
    
    companion object {
        const val UPDATE_REQUEST_CODE = 1001
    }
}

package com.example.esriapplication

import android.app.Application

class MyApplication : Application() {


    override fun onCreate() {
        super.onCreate()
        Globals.trustEveryone()
    }

}
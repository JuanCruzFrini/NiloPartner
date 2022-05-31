package com.example

import android.app.Application
import com.example.nilopartner.cloudmessaging.VolleyHelper

class NiloPartnerApplication : Application() {
    companion object{
        lateinit var volleyHelper: VolleyHelper
    }

    override fun onCreate() {
        super.onCreate()

        volleyHelper = VolleyHelper.getInstance(this)
    }
}
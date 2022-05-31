package com.example.nilopartner.cloudmessaging

import android.content.Context
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

class VolleyHelper(context: Context) {

    companion object{
        @Volatile
        private var Instance:VolleyHelper? = null

        fun getInstance(context: Context) = Instance ?: synchronized(this){
            Instance ?: VolleyHelper(context).also { Instance = it }
        }
    }

    val requestQueue: RequestQueue by lazy {
        Volley.newRequestQueue(context.applicationContext)
    }

    fun <T>addToRequestQueue(req: Request<T>){
        requestQueue.add(req)
    }
}
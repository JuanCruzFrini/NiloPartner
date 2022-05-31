package com.example.nilopartner.cloudmessaging

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.toolbox.JsonObjectRequest
import com.example.NiloPartnerApplication
import com.example.nilopartner.Constants
import com.google.android.gms.auth.api.Auth
import com.squareup.okhttp.Response
import org.json.JSONException
import org.json.JSONObject

class NotificationRemoteService {

    fun sendNotification(title:String, message:String, tokens:String){
        val params = JSONObject()
        params.put(Constants.PARAM_METHOD, Constants.SEND_NOTIFICATION)
        params.put(Constants.PARAM_TITLE, title)
        params.put(Constants.PARAM_MESSAGE, message)
        params.put(Constants.PARAM_TOKENS, tokens)

        val jsonObjectRequest:JsonObjectRequest =
            object : JsonObjectRequest(
                Method.POST,
                Constants.NILO_PARTNER_RS,
                params,
                com.android.volley.Response.Listener { response ->
                    try {
                        val success = response.getInt(Constants.PARAM_SUCCESS)
                        Log.i("Volley success", success.toString())
                        Log.i("Volley Response", response.toString())
                    } catch(e:JSONException) {
                        e.printStackTrace()
                        Log.e("Volley exception", e.localizedMessage)
                    }
                }, com.android.volley.Response.ErrorListener { error ->
                    if (error.localizedMessage != null) {
                        Log.e("Volley error", error.localizedMessage)
                    }
                }) {

                @Throws(AuthFailureError::class)
                override fun getHeaders(): MutableMap<String, String> {
                    val paramsHeaders = HashMap<String, String>()
                    paramsHeaders["Content-Type"] = "application/json; charset=utf-8"
                    return super.getHeaders()
                }
        }
        NiloPartnerApplication.volleyHelper.addToRequestQueue(jsonObjectRequest)
    }
}
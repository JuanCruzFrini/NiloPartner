package com.example.nilopartner.order

import com.example.order.Order

interface OnOrderListener {
    fun onStartChat(order: Order)
    fun onStatusChange(order: Order)
}
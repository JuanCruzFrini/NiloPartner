package com.example.nilopartner.producto

import com.example.nilopartner.entities.Producto

interface OnProductListener {
    fun onClick(producto: Producto)
    fun onLongClick(producto: Producto)
}
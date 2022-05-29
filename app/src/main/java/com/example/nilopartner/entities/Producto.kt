package com.example.nilopartner.entities

import com.google.firebase.firestore.Exclude

data class Producto(
    //"@get:Exclude" No toma en cuenta la propiedad al insertar el objeto en la DB
    //*excluimos el id por que ya se agrega por default, para no repetir*
    @get:Exclude var id:String? = null,
    var name:String? = null,
    var description:String? = null,
    var imgUrl:String? = null,
    var quantity:Int = 0,
    var price:Double = 0.0
)
{
    //clickDerecho>generate>equals and hashcode
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Producto

        if (id != other.id) return false

        return true
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}

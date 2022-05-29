package com.example.nilopartner.producto

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilopartner.entities.Producto
import com.example.nilopartner.R
import com.example.nilopartner.databinding.ItemProductoBinding

class ProductAdapter(
    private val productList: MutableList<Producto>,
    val listener: OnProductListener
)
    : RecyclerView.Adapter<ProductAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_producto, parent, false))

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.setListener(productList[position])

        holder.binding.txtName.text = productList[position].name
        holder.binding.txtPrecio.text = productList[position].price.toString()
        holder.binding.txtQuantity.text = productList[position].quantity.toString()

        Glide.with(holder.itemView.context)
            .load(productList[position].imgUrl)
            .placeholder(R.drawable.time_lapse)
            .error(R.drawable.broken_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .centerCrop().into(holder.binding.imgProducto)
    }

    override fun getItemCount(): Int = productList.size

    //agrega producto a Firestore
    fun addProduct(producto: Producto){
        if (!productList.contains(producto)){
            productList.add(producto)
            notifyItemInserted(productList.size - 1)
        } else {
            updateProduct(producto)
        }
    }

    //actualiza producto de Firestore
    fun updateProduct(producto: Producto){
        val index = productList.indexOf(producto)
        if (index != -1){
            productList.set(index, producto)
            notifyItemChanged(index)
        }
    }

    //elimina producto de Firestore
    fun deleteProduct(producto: Producto){
        val index = productList.indexOf(producto)
        if (index != -1){
            productList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val binding = ItemProductoBinding.bind(itemView)

        fun setListener(producto: Producto){
            binding.root.setOnClickListener {
                listener.onClick(producto)
            }
            binding.root.setOnLongClickListener {
                listener.onLongClick(producto)
                true
            }
        }
    }
}
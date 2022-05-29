package com.example.nilopartner.order

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nilopartner.Constants
import com.example.nilopartner.R
import com.example.nilopartner.databinding.ActivityMainBinding
import com.example.nilopartner.databinding.ActivityOrderBinding
import com.example.order.Order
import com.google.firebase.firestore.FirebaseFirestore

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding

    private lateinit var adapter: OrderAdapter

    private lateinit var orderSelected:Order

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setRecyclerView()
        setupFirestore()
    }

    override fun onStartChat(order: Order) {
        TODO("Not yet implemented")
    }

    override fun onStatusChange(order: Order) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_REQUESTS)
            .document(order.id)
            .update(Constants.PROP_STATUS, order.status)
            .addOnSuccessListener {
                Toast.makeText(this, "Orden actualizada", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar orden", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getOrderSelected(): Order = orderSelected

    private fun setupFirestore(){
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_REQUESTS)
            .get()
            .addOnSuccessListener {
                for (document in it){
                    val order = document.toObject(Order::class.java)
                    order.id = document.id
                    adapter.addOrder(order)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al consultar los datos", Toast.LENGTH_SHORT).show()
            }

    }

    private fun setRecyclerView() {
        adapter = OrderAdapter(mutableListOf(), this)

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@OrderActivity)
            adapter = this@OrderActivity.adapter
        }
    }


}
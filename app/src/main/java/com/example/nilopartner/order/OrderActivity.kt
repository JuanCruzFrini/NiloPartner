package com.example.nilopartner.order

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.message.ChatFragment
import com.example.nilopartner.Constants
import com.example.nilopartner.R
import com.example.nilopartner.cloudmessaging.NotificationRemoteService
import com.example.nilopartner.databinding.ActivityMainBinding
import com.example.nilopartner.databinding.ActivityOrderBinding
import com.example.order.Order
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase

class OrderActivity : AppCompatActivity(), OnOrderListener, OrderAux {

    private lateinit var binding: ActivityOrderBinding

    private lateinit var adapter: OrderAdapter

    private lateinit var orderSelected:Order

    private val arrayValues:Array<String> by lazy {
        resources.getStringArray(R.array.status_values)
    }
    private val arrayKeys:Array<Int> by lazy {
        resources.getIntArray(R.array.status_key).toTypedArray()
    }

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setRecyclerView()
        setupFirestore()
        configAnalytics()
    }

    private fun configAnalytics(){
        firebaseAnalytics = Firebase.analytics
    }

    private fun notifyClient(order: Order){
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_USERS)
            .document(order.clientId)
            .collection(Constants.COLL_TOKENS)
            .get()
            .addOnSuccessListener {
                var tokensStr = ""
                for (document in it){
                    val tokenMap = document.data
                    tokensStr += "${tokenMap.getValue(Constants.PROP_TOKEN)},"
                }
                if (tokensStr.length > 0) {
                    tokensStr = tokensStr.dropLast(1)

                    var names = ""
                    order.products.forEach {
                        names += "${it.value.name}, "
                    }
                    names = names.dropLast(2)

                    val index = arrayKeys.indexOf(order.status)
                    val notificationRs = NotificationRemoteService()
                    notificationRs.sendNotification(
                        "Tu pedido ha sido ${arrayValues[index]}",
                        names,
                        tokensStr)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al notificar", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onStartChat(order: Order) {
        orderSelected = order
        val fragment = ChatFragment()
        supportFragmentManager.beginTransaction()
            .add(R.id.containerMain, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onStatusChange(order: Order) {
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_REQUESTS)
            .document(order.id)
            .update(Constants.PROP_STATUS, order.status)
            .addOnSuccessListener {
                Toast.makeText(this, "Orden actualizada", Toast.LENGTH_SHORT).show()
                notifyClient(order)

                //Enviar array de parametros en Analytics
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.ADD_SHIPPING_INFO){
                    val productos = mutableListOf<Bundle>()
                    order.products.forEach{
                        val bundle = Bundle()
                        bundle.putString("id_producto", it.key)
                        productos.add(bundle)
                    }
                    param(FirebaseAnalytics.Param.SHIPPING, productos.toTypedArray())
                    param(FirebaseAnalytics.Param.PRICE, order.totalPrice)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Error al actualizar orden", Toast.LENGTH_SHORT).show()
            }
    }

    override fun getOrderSelected(): Order = orderSelected

    private fun setupFirestore(){
        val db = FirebaseFirestore.getInstance()
        db.collection(Constants.COLL_REQUESTS)
            //Para las compras, optimo es ordenar en base a la fecha
            .orderBy(Constants.PROP_DATE, Query.Direction.DESCENDING)
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
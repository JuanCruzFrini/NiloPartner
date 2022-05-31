package com.example.nilopartner.producto

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.recyclerview.widget.GridLayoutManager
import com.example.nilopartner.add.AddDialogFragment
import com.example.nilopartner.Constants
import com.example.nilopartner.entities.Producto
import com.example.nilopartner.R
import com.example.nilopartner.databinding.ActivityMainBinding
import com.example.nilopartner.order.OrderActivity
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.ErrorCodes
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity(), OnProductListener, MainAux {

    private lateinit var binding: ActivityMainBinding
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var authStateListener: FirebaseAuth.AuthStateListener
    private lateinit var adapter: ProductAdapter

    private lateinit var firestoreListener:ListenerRegistration
    private var productSelected: Producto? = null

    private lateinit var firebaseAnalytics: FirebaseAnalytics

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configAuth()
        setRecyclerView()
        //configFirestore()
        //configFirestoreRealtime()
        configButtons()
        configAnalytics()
    }

    //agrega los buttons al fragment
    private fun configButtons() {
        binding.extendFab.setOnClickListener {
            productSelected = null
            AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
        }
    }

    private fun configAnalytics(){
        //Inicializamos el servicio de Analytics
        firebaseAnalytics = Firebase.analytics
    }

    private fun configAuth(){
        firebaseAuth = FirebaseAuth.getInstance()
        authStateListener = FirebaseAuth.AuthStateListener { auth ->
            //si hay un usuario activo
            if (auth.currentUser != null){
                supportActionBar?.title = auth.currentUser?.displayName
                binding.NestScrollView.show()
                binding.progressLayout.hide()
                binding.extendFab.show()
            } else {
                //si no hay un usuario activo, muestra FirebaseUI login.
                //proveedores de login
                val providers = arrayListOf(
                    AuthUI.IdpConfig.EmailBuilder().build(),
                    AuthUI.IdpConfig.GoogleBuilder().build())

                //creamos el UI de autenticacion/login
                resultLauncher.launch(
                    AuthUI.getInstance()
                        .createSignInIntentBuilder()
                        .setAvailableProviders(providers)
                        .setIsSmartLockEnabled(false)
                        .build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        firebaseAuth.addAuthStateListener(authStateListener)
        configFirestoreRealtime()
    }
    override fun onPause() {
        super.onPause()
        firebaseAuth.removeAuthStateListener(authStateListener)
        firestoreListener.remove()
    }

    //launcher para autenticacion/login
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { ActivityResult ->
        val response = IdpResponse.fromResultIntent(ActivityResult.data)

        if (ActivityResult.resultCode == RESULT_OK) {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                Toast.makeText(this, "Bienvenido", Toast.LENGTH_SHORT).show()

                //registramos un evento en Analytics
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                    param(FirebaseAnalytics.Param.SUCCESS, 100)//100 = Login successfully
                    param(FirebaseAnalytics.Param.METHOD, "login")
                }
            }
        } else {
            if (response == null){
                Toast.makeText(this, "Hasta luego", Toast.LENGTH_SHORT).show()

                //registramos un evento en Analytics
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                    param(FirebaseAnalytics.Param.SUCCESS, 200)//200 = cancel
                    param(FirebaseAnalytics.Param.METHOD, "login")
                }

                finishAffinity()
            } else {
                response.error?.let { firebaseUiException ->
                    if (firebaseUiException.errorCode == ErrorCodes.NO_NETWORK){
                        Toast.makeText(this, "Sin Internet", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: ${firebaseUiException.errorCode}", Toast.LENGTH_SHORT).show()
                    }
                    //registramos un evento en Analytics
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                        param(FirebaseAnalytics.Param.SUCCESS, firebaseUiException.errorCode.toLong())
                        param(FirebaseAnalytics.Param.METHOD, "login")
                    }
                }
            }
        }
    }

    private fun setRecyclerView() {
        adapter = ProductAdapter(mutableListOf(), this)
        binding.recyclerView.let {
            it.layoutManager = GridLayoutManager(this@MainActivity, 3)
            it.adapter = adapter
        }
    }

    //abre el detalle del producto con el mismo dialog de addProducto
    override fun onClick(producto: Producto) {
        productSelected = producto
        AddDialogFragment().show(supportFragmentManager, AddDialogFragment::class.java.simpleName)
    }

    //borra el producto de la databse
    override fun onLongClick(producto: Producto) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTOS)
        producto.id?.let { id ->
            productRef.document(id)
                .delete()
                .addOnFailureListener {
                    Toast.makeText(this, "Error al eliminar", Toast.LENGTH_SHORT).show()
                }
        }
    }

    //menu overflow
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_sign_out -> cerrarSesion()
            R.id.action_order_history -> openHistory()
        }
        return true
    }

    private fun openHistory() {
        startActivity(Intent(this, OrderActivity::class.java))
    }

    //lee listado de firebase en y lo actualiza en tiempo real
    private fun configFirestoreRealtime(){
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTOS)

        firestoreListener = productRef.addSnapshotListener { snapshots, error ->
            if (error != null){
                Toast.makeText(this, "Error ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }
            for (snapshot in snapshots!!.documentChanges){
                val producto = snapshot.document.toObject(Producto::class.java)
                producto.id = snapshot.document.id
                when(snapshot.type){
                    DocumentChange.Type.ADDED -> adapter.addProduct(producto)
                    DocumentChange.Type.MODIFIED -> adapter.updateProduct(producto)
                    DocumentChange.Type.REMOVED -> adapter.deleteProduct(producto)
                }
            }
        }
    }

    //leer listado de Cloud Firestore Database
    private fun configFirestore() {
        val db = FirebaseFirestore.getInstance()

        db.collection(Constants.COLL_PRODUCTOS).get()
            //puede que no se carguen los datos sin un constructor
            //por default en los atributos de la data class.
            .addOnSuccessListener { snapshots ->
                for (document in snapshots){
                    val producto = document.toObject(Producto::class.java)
                    producto.id = document.id
                    adapter.addProduct(producto)
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Error al consultar datos ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cerrarSesion() {
        AuthUI.getInstance().signOut(this)
            .addOnSuccessListener {
            Toast.makeText(this, "Sesion terminada", Toast.LENGTH_SHORT).show()

                //registramos un evento en Analytics
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                    param(FirebaseAnalytics.Param.SUCCESS, 100)//100 = sign out successfully
                    param(FirebaseAnalytics.Param.METHOD, "sign_out")
                }
            }
            .addOnCompleteListener { tarea ->
                if (tarea.isSuccessful){
                    binding.progressLayout.show()
                    binding.NestScrollView.hide()
                    binding.extendFab.hide()
                } else {
                    Toast.makeText(this, "No se pudo cerrar la sesion", Toast.LENGTH_SHORT).show()

                    //registramos un evento en Analytics
                    firebaseAnalytics.logEvent(FirebaseAnalytics.Event.LOGIN){
                        param(FirebaseAnalytics.Param.SUCCESS, 201)//201 = error sign out
                        param(FirebaseAnalytics.Param.METHOD, "sign_out")
                    }
                }
            }
    }

    override fun getProductSelected(): Producto? = productSelected
}

fun View.hide() { visibility = View.GONE }
fun View.show() { visibility = View.VISIBLE }

fun View.enable() { this.isEnabled = true}
fun View.disable() {this.isEnabled = false}
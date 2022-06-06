package com.example.nilopartner.producto

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.icu.text.SymbolTable
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.ktx.logEvent
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import java.lang.Exception

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

    //para seleccion de imagenes multiples
    private var count =0
    private val uriList = mutableListOf<Uri>()
    private val progressSnackbar: Snackbar by lazy {
        Snackbar.make(binding.root, "", Snackbar.LENGTH_INDEFINITE)
    }

    //para seleccion de imagenes multiples
    private var galleryResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            if (it.data?.clipData != null){
                count = it.data!!.clipData!!.itemCount

                for (i in 0..count - 1){
                    uriList.add(it.data!!.clipData!!.getItemAt(i).uri)
                }
                if (count > 0) uploadImage(0)
            }
        }
    }

    //para seleccion de imagenes multiples
    private fun uploadImage(position: Int) {
        progressSnackbar.apply {
            setText("Subiendo imagen ${position + 1} de $context...")
            show()
        }
        FirebaseAuth.getInstance().currentUser?.let { user ->
            val productoRef =  FirebaseStorage.getInstance().reference
                .child(user.uid)
                .child(Constants.PATH_IMAGE)
                .child(productSelected!!.id!!)
                .child("image${position + 1}")

            productoRef.putFile(uriList[position])
                .addOnSuccessListener {
                    if (position < count - 1){
                        uploadImage(position + 1)
                    } else {
                        progressSnackbar.apply {
                            setText("Imagenes subidas correctamente")
                            duration = Snackbar.LENGTH_SHORT
                            show()
                        }
                    }
                }
                .addOnFailureListener {
                    progressSnackbar.apply {
                        setText("Error al subir la imagen${position + 1}")
                        duration = Snackbar.LENGTH_LONG
                        show()
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
        val adapter = ArrayAdapter<String>(this, android.R.layout.select_dialog_singlechoice)
        adapter.add("Eliminar")
        adapter.add("Añadir fotos")

        MaterialAlertDialogBuilder(this)
            .setAdapter(adapter){ dialogInterface : DialogInterface, position:Int ->
                when(position){
                    0 -> confirmDeleteProducto(producto)
                    1 -> selectMultiplesImages(producto)
                }
            }
            .show()
    }

    private fun selectMultiplesImages(producto: Producto) {
        productSelected = producto
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true) //habilita la seleccion multiple
        galleryResult.launch(intent)
    }

    //
    private fun confirmDeleteProducto(producto: Producto){
        MaterialAlertDialogBuilder(this)
            .setTitle("Eliminar producto?")
            .setMessage("El producto se eliminará permanentemente")
            .setPositiveButton("Eliminar"){ _,_ ->
                val db = FirebaseFirestore.getInstance()
                val productRef = db.collection(Constants.COLL_PRODUCTOS)
                producto.id?.let { id ->
                    producto.imgUrl?.let{ url ->
                        try {
                            val photoRef = FirebaseStorage.getInstance().getReferenceFromUrl(url)

                            //Eliminamos el registro y la foto
                            //FirebaseStorage.getInstance().reference.child(Constants.PATH_IMAGE).child(id)
                            photoRef
                                .delete()
                                .addOnSuccessListener {
                                    deleteProductoFromFirestore(id)
                                }
                                .addOnFailureListener {
                                    if ((it as StorageException).errorCode == StorageException.ERROR_OBJECT_NOT_FOUND){
                                        deleteProductoFromFirestore(id)
                                    } else {
                                        Toast.makeText(this, "Error al eliminar foto", Toast.LENGTH_SHORT).show()
                                    }
                                }
                        } catch (e:Exception){
                            e.printStackTrace()
                            deleteProductoFromFirestore(id)
                        }
                    }
                }
            }
            .setNegativeButton("Cancelar", null)
            //.create()
            .show()
    }

    private fun deleteProductoFromFirestore(productoId: String) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection(Constants.COLL_PRODUCTOS)
        productRef.document(productoId)
            .delete()
            .addOnFailureListener {
                Toast.makeText(this, "Error al eliminar registro", Toast.LENGTH_SHORT).show()
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

    //abrimos gallery
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
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
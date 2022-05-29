package com.example.nilopartner.add

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.DialogFragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.nilopartner.Constants
import com.example.nilopartner.entities.EvenPost
import com.example.nilopartner.entities.Producto
import com.example.nilopartner.R
import com.example.nilopartner.databinding.FragmentDialogAddBinding
import com.example.nilopartner.producto.MainAux
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class AddDialogFragment : DialogFragment(), DialogInterface.OnShowListener {

    private var binding:FragmentDialogAddBinding? = null

    private var positiveButton:Button? = null
    private var negativeButton:Button? = null

    private var producto: Producto? = null

    //crea el dialog de insercion de datos
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        activity?.let { activity ->
            binding = FragmentDialogAddBinding.inflate(LayoutInflater.from(context))

            binding?.let  {
                val builder = AlertDialog.Builder(activity)
                    .setTitle("Agregar producto")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Agregar", null)
                    .setView(it.root)

                val dialog = builder.create()
                dialog.setOnShowListener(this)

                return dialog
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    //muestra el dialog para añadir producto
    override fun onShow(dialogInterface: DialogInterface?) {
        initProduct()
        configButtons()

        val dialog = dialog as? AlertDialog
        dialog?.let {
            positiveButton = it.getButton(Dialog.BUTTON_POSITIVE)
            negativeButton = it.getButton(Dialog.BUTTON_NEGATIVE)

            positiveButton?.setOnClickListener {
                binding?.let {
                    enableUI(false)
                    uploadImage(producto?.id){ eventPost->
                        if (eventPost.isSuccess){
                            if (producto == null) {
                                //si el producto es nulo, lo agrega
                                val producto = Producto(
                                    name = it.etName.text.toString().trim(),
                                    description = it.etDescripcion.text.toString().trim(),
                                    imgUrl = eventPost.photoUrl,
                                    quantity = it.etQuantity.text.toString().toInt(),
                                    price = it.etPrice.text.toString().toDouble()
                                )
                                save(producto, eventPost.documentId!!)

                            } else {
                                //si el producto no es nulo, lo actualiza
                                producto?.apply {
                                    name =  it.etName.text.toString().trim()
                                    description = it.etDescripcion.text.toString().trim()
                                    imgUrl = eventPost.photoUrl
                                    quantity = it.etQuantity.text.toString().toInt()
                                    price = it.etPrice.text.toString().toDouble()

                                    update(this)
                                }
                            }
                        }
                    }
                }
            }
            negativeButton?.setOnClickListener {
                dismiss()
            }
        }
    }

    //si existe el producto, rellena los ET para poder editarlo
    private fun initProduct() {
        producto = (activity as? MainAux)?.getProductSelected()
        producto?.let { producto ->
            binding?.let {
                it.etName.setText(producto.name)
                it.etDescripcion.setText(producto.description)
                it.etQuantity.setText(producto.quantity.toString())
                it.etPrice.setText(producto.price.toString())

                Glide.with(this).load(producto.imgUrl)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.time_lapse)
                    .error(R.drawable.broken_image)
                    .centerCrop().into(it.imgProductPreview)
            }
        }
    }

    //listener de open gallery
    private fun configButtons(){
        binding?.let {
            it.ibProducto.setOnClickListener {
                openGallery()
            }
        }
    }

    //abrimos gallery
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        resultLauncher.launch(intent)
    }

    //sube la imagen seleccionada a Firebase Storage
    private fun uploadImage(productID:String?, callBack:(EvenPost)->Unit){
        val eventPost = EvenPost()
        eventPost.documentId = productID ?: FirebaseFirestore.getInstance()
            .collection(Constants.COLL_PRODUCTOS).document().id

        val storageRef = FirebaseStorage.getInstance().reference.child(Constants.PATH_IMAGE)

        photoSelectedUri?.let { uri->
            binding?.let { binding->
                binding.progressBar.show()
                val photoRef = storageRef.child(eventPost.documentId!!)
                photoRef.putFile(uri)
                    .addOnProgressListener {
                        val progress = (100 * it.bytesTransferred / it.totalByteCount).toInt()
                        it.run {
                            binding.progressBar.progress = progress
                            binding.txtProgress.text = String.format("%s%%", progress)
                        }
                    }
                    .addOnSuccessListener {
                        it.storage.downloadUrl.addOnSuccessListener { downloadUrl ->
                            Log.i("URL", downloadUrl.toString())
                            eventPost.isSuccess = true
                            eventPost.photoUrl = downloadUrl.toString()
                            callBack(eventPost)
                        }
                    }
                    .addOnFailureListener{
                        eventPost.isSuccess = false
                        enableUI(true)
                        Toast.makeText(activity, "Error al subir imagen ${it.message}", Toast.LENGTH_LONG).show()
                        callBack(eventPost)
                    }
            }
        }
    }

    //inserta el producto en Firestore
    private fun save(producto: Producto, documentId:String){
        val db = FirebaseFirestore.getInstance()
        db.collection("productos")
            //.add(producto)
            .document(documentId).set(producto)
            .addOnSuccessListener {
                Toast.makeText(activity, "Producto añadido", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(activity, "Error al insertar ${it.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener {
                enableUI(true)
                binding?.progressBar?.hide()
                dismiss()
            }
    }

    //edita el producto en Firestore
    private fun update(producto: Producto){
        val db = FirebaseFirestore.getInstance()
        producto.id?.let { id ->
            db.collection("productos").document(id).set(producto)
                .addOnSuccessListener {
                    Toast.makeText(activity, "Producto actualizado", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener {
                    Toast.makeText(activity, "Error al actualizar ${it.message}", Toast.LENGTH_SHORT).show()
                }
                .addOnCompleteListener {
                    enableUI(true)
                    binding?.progressBar?.hide()
                    dismiss()
                }
        }
    }

    //bloqueamos el fragment de add/update producto mientras
    //se ejecute el proceso, para evitar multiples adds/updates
    private fun enableUI(enable:Boolean){
        positiveButton?.isEnabled = enable
        negativeButton?.isEnabled = enable
        binding?.let {
            with(it){
                etName.isEnabled = enable
                etDescripcion.isEnabled = enable
                etQuantity.isEnabled = enable
                etPrice.isEnabled = enable
            }
        }
    }

    //seleccion de imagen desde la galeria
    private var photoSelectedUri: Uri? = null
    private val resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()){
        if (it.resultCode == Activity.RESULT_OK){
            photoSelectedUri = it.data?.data

            binding?.imgProductPreview?.setImageURI(photoSelectedUri)
        }
    }

    //desvincula el viewBinding al ser destruido
    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

}
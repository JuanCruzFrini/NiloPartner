package com.example.message

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.nilopartner.Constants
import com.example.nilopartner.R
import com.example.nilopartner.databinding.FragmentChatBinding
import com.example.nilopartner.message.ChatAdapter
import com.example.nilopartner.message.Message
import com.example.nilopartner.message.OnChatListener
import com.example.nilopartner.order.OrderAux
import com.example.order.Order
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

class ChatFragment : Fragment(), OnChatListener {

    private var binding: FragmentChatBinding? = null

    private lateinit var adapter: ChatAdapter

    private var order: Order? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        //binding?.let {
            return binding!!.root//it.root
        //}
        return super.onCreateView(inflater, container, savedInstanceState)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getOrder()

        setUpRecyclerView()
        setUpButtons()
    }

    private fun setUpRecyclerView() {
        adapter = ChatAdapter(mutableListOf(), this)
        binding?.let {
            it.recyclerView.apply {
                layoutManager = LinearLayoutManager(context).also {
                    it.stackFromEnd = true
                }
                adapter = this@ChatFragment.adapter
            }
        }

      /*  (1..20).forEach {
            adapter.addMessage(Message(
                id = it.toString(),
                message = if (it%4 == 0) "Hola como estas".repeat(4) else "Hola como estas?",
                sender = if (it%3 == 0) "tu" else "yo",
                myUid = "yo"))
        }*/
    }

    private fun setUpButtons(){
        binding?.let {
            it.btnSend.setOnClickListener {
                sendMessage()
            }
        }
    }

    private fun sendMessage() {
        binding?.let { binding ->
            order?.let {
                val db = FirebaseDatabase.getInstance()
                val chatRef = db.getReference(Constants.PATH_CHATS).child(it.id)
                val user = FirebaseAuth.getInstance().currentUser
                user?.let {
                    val message =
                        Message(
                        message = binding.etMessage.text.toString().trim(),
                        sender = it.uid
                        )
                    binding.btnSend.isEnabled = false
                    chatRef.push().setValue(message)
                        .addOnSuccessListener {
                            binding.etMessage.setText("")
                        }
                        .addOnCompleteListener {
                            binding.btnSend.isEnabled = true
                        }
                }
            }
        }
    }

    private fun getOrder() {
        order = (activity as? OrderAux)?.getOrderSelected()
        order?.let {
            setUpActionBar()
            setUpRealtimeDatabase()
        }
    }

    private fun setUpRealtimeDatabase() {
        order?.let {
            val db = Firebase.database
            val chatRef = db.getReference(Constants.PATH_CHATS).child(it.id)
            val childListener = object : ChildEventListener{

                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let {
                        adapter.addMessage(it)
                        binding?.recyclerView?.scrollToPosition(adapter.itemCount - 1)
                    }
                }
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                    getMessage(snapshot)?.let {
                        adapter.update(it)
                    }
                }
                override fun onChildRemoved(snapshot: DataSnapshot) {
                    getMessage(snapshot)?.let {
                        adapter.delete(it)
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    binding?.let {
                        Snackbar.make(it.root, "Error al cargar chat.",Snackbar.LENGTH_LONG).show()
                    }
                }
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            }
            chatRef.addChildEventListener(childListener)
        }
    }

    private fun getMessage(snapshot:DataSnapshot) : Message? {
        val message = snapshot.getValue(Message::class.java)
        message?.let { message ->
            snapshot.key?.let { key->
                message.id = key
            }
            FirebaseAuth.getInstance().currentUser?.let { user ->
                message.myUid = user.uid
            }
            return message
        }
        return null
    }

    override fun deleteMessage(message: Message) {
        order?.let {
            val database = Firebase.database
            val messageRef =
                database.getReference(Constants.PATH_CHATS).child(it.id).child(message.id)
            messageRef.removeValue { error, ref ->
                binding?.let {
                    if (error != null) {
                        Snackbar.make(it.root, "Error al borrar mensaje.", Snackbar.LENGTH_LONG)
                            .show()
                    } else {
                        Snackbar.make(it.root, "Mensaje borrado.", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            }
        }
    }

    private fun setUpActionBar() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(true)
            it.supportActionBar?.title = getString(R.string.chat_title)
            setHasOptionsMenu(true)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home){
            activity?.onBackPressed()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroy() {
        (activity as? AppCompatActivity)?.let {
            it.supportActionBar?.setDisplayHomeAsUpEnabled(false)
            it.supportActionBar?.title = getString(R.string.order_title)
            setHasOptionsMenu(false)
        }
        super.onDestroy()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}
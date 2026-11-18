package com.naghiya.koleksibacaan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.naghiya.koleksibacaan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val adapter = BukuAdapter()
    private var listener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val user = auth.currentUser
        if (user == null) {
            keLogin()
            return
        }

        val nama = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.orEmpty()
        binding.tvHalo.text = getString(R.string.halo_pengguna, nama)

        binding.rvBuku.layoutManager = LinearLayoutManager(this)
        binding.rvBuku.adapter = adapter

        binding.fabTambah.setOnClickListener {
            startActivity(Intent(this, BookFormActivity::class.java))
        }
        binding.btnKeluar.setOnClickListener {
            auth.signOut()
            keLogin()
        }
    }

    override fun onStart() {
        super.onStart()
        muatBuku()
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun muatBuku() {
        val uid = auth.currentUser?.uid ?: return
        binding.progressBar.visibility = View.VISIBLE
        binding.tvKosong.visibility = View.GONE

        listener = db.collection("books")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    binding.tvKosong.text = getString(R.string.gagal_muat)
                    binding.tvKosong.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                val daftar = snapshot?.toObjects(Buku::class.java).orEmpty()
                    .sortedBy { it.judul.lowercase() }
                adapter.submitList(daftar)

                binding.tvKosong.text = getString(R.string.daftar_kosong)
                binding.tvKosong.visibility = if (daftar.isEmpty()) View.VISIBLE else View.GONE
            }
    }

    private fun keLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
package com.naghiya.koleksibacaan

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.naghiya.koleksibacaan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()
    private val adapter = BukuAdapter { buku ->
        val intent = Intent(this, BookDetailActivity::class.java)
        intent.putExtra(BookDetailActivity.EXTRA_BOOK_ID, buku.id)
        startActivity(intent)
    }
    private var listener: ListenerRegistration? = null

    private var semuaBuku: List<Buku> = emptyList()
    private var kataKunci: String = ""
    private var filterStatus: String? = null

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
        binding.tvAkunNama.text = nama
        binding.tvAkunEmail.text = user.email.orEmpty()

        binding.rvBuku.layoutManager = LinearLayoutManager(this)
        binding.rvBuku.adapter = adapter

        binding.fabTambah.setOnClickListener {
            startActivity(Intent(this, BookFormActivity::class.java))
        }
        binding.btnKeluar.setOnClickListener {
            auth.signOut()
            keLogin()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_koleksi -> {
                    tampilkanHalaman(akun = false)
                    true
                }
                R.id.nav_akun -> {
                    tampilkanHalaman(akun = true)
                    true
                }
                else -> false
            }
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                binding.searchView.clearFocus()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                kataKunci = newText.orEmpty().trim()
                terapkanFilter()
                return true
            }
        })

        binding.chipGroup.setOnCheckedStateChangeListener { _, ids ->
            filterStatus = when (ids.firstOrNull()) {
                R.id.chipIngin -> getString(R.string.status_ingin_dibaca)
                R.id.chipSedang -> getString(R.string.status_sedang_dibaca)
                R.id.chipSelesai -> getString(R.string.status_selesai)
                else -> null
            }
            terapkanFilter()
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

    private fun tampilkanHalaman(akun: Boolean) {
        binding.layoutDaftar.visibility = if (akun) View.GONE else View.VISIBLE
        binding.layoutAkun.visibility = if (akun) View.VISIBLE else View.GONE
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

                semuaBuku = snapshot?.toObjects(Buku::class.java).orEmpty()
                    .sortedBy { it.judul.lowercase() }
                terapkanFilter()
            }
    }

    private fun terapkanFilter() {
        val hasil = semuaBuku.filter { buku ->
            val cocokStatus = filterStatus == null || buku.status == filterStatus
            val cocokKata = kataKunci.isEmpty() ||
                    buku.judul.contains(kataKunci, ignoreCase = true) ||
                    buku.penulis.contains(kataKunci, ignoreCase = true)
            cocokStatus && cocokKata
        }
        adapter.submitList(hasil)

        if (hasil.isEmpty()) {
            val pesan = if (semuaBuku.isEmpty()) R.string.daftar_kosong else R.string.tidak_ditemukan
            binding.tvKosong.text = getString(pesan)
            binding.tvKosong.visibility = View.VISIBLE
        } else {
            binding.tvKosong.visibility = View.GONE
        }
    }

    private fun keLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
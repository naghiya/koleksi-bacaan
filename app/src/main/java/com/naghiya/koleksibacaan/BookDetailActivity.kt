package com.naghiya.koleksibacaan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.naghiya.koleksibacaan.databinding.ActivityBookDetailBinding

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    private var bookId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra(EXTRA_BOOK_ID).orEmpty()
        if (bookId.isEmpty()) {
            Toast.makeText(this, R.string.buku_tidak_ditemukan, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, BookFormActivity::class.java)
            intent.putExtra(EXTRA_BOOK_ID, bookId)
            startActivity(intent)
        }
    }

    override fun onStart() {
        super.onStart()
        if (bookId.isEmpty()) return

        listener = db.collection("books").document(bookId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, R.string.gagal_muat, Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                val buku = snapshot?.toObject(Buku::class.java)
                if (buku == null) {
                    // Buku sudah dihapus atau tidak ada
                    finish()
                    return@addSnapshotListener
                }
                tampilkan(buku)
            }
    }

    override fun onStop() {
        super.onStop()
        listener?.remove()
        listener = null
    }

    private fun tampilkan(buku: Buku) {
        binding.tvJudul.text = buku.judul
        binding.tvPenulis.text = buku.penulis
        binding.tvKategori.text = buku.kategori
        binding.tvStatus.text = buku.status
        binding.ratingBar.rating = buku.rating
        binding.tvProgres.text =
            getString(R.string.progres_halaman, buku.halamanTerbaca, buku.jumlahHalaman)
        binding.tvLinkInfo.text =
            buku.linkInfo.ifEmpty { getString(R.string.link_kosong) }
    }

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
    }
}
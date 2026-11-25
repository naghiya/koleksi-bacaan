package com.naghiya.koleksibacaan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.naghiya.koleksibacaan.databinding.ActivityBookDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import android.graphics.BitmapFactory
import android.util.Base64
import android.widget.ImageView

class BookDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookDetailBinding
    private val db = FirebaseFirestore.getInstance()
    private var listener: ListenerRegistration? = null
    private var bookId: String = ""

    private var linkInfo: String = ""

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

        binding.btnInfo.setOnClickListener {
            if (linkInfo.isBlank()) {
                Toast.makeText(this, R.string.link_kosong, Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(this, WebViewActivity::class.java)
                intent.putExtra(WebViewActivity.EXTRA_URL, linkInfo)
                startActivity(intent)
            }
        }

        binding.btnEdit.setOnClickListener {
            val intent = Intent(this, BookFormActivity::class.java)
            intent.putExtra(EXTRA_BOOK_ID, bookId)
            startActivity(intent)
        }

        binding.btnHapus.setOnClickListener { konfirmasiHapus() }
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

    private fun konfirmasiHapus() {
        val judul = binding.tvJudul.text.toString()
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_hapus_judul)
            .setMessage(getString(R.string.dialog_hapus_pesan, judul))
            .setNegativeButton(R.string.btn_batal, null)
            .setPositiveButton(R.string.btn_ya_hapus) { _, _ -> hapusBuku() }
            .show()
    }

    private fun hapusBuku() {
        binding.btnHapus.isEnabled = false
        db.collection("books").document(bookId).delete()
            .addOnSuccessListener {
                Toast.makeText(this, R.string.buku_dihapus, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                binding.btnHapus.isEnabled = true
                Toast.makeText(this, R.string.buku_gagal_hapus, Toast.LENGTH_LONG).show()
            }
    }

    private fun tampilkanCover(base64: String) {
        if (base64.isEmpty()) return
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return
        binding.ivCover.imageTintList = null
        binding.ivCover.setPadding(0, 0, 0, 0)
        binding.ivCover.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.ivCover.setImageBitmap(bitmap)
    }
    private fun tampilkan(buku: Buku) {
        linkInfo = buku.linkInfo
        binding.tvJudul.text = buku.judul
        binding.tvPenulis.text = buku.penulis
        binding.tvKategori.text = buku.kategori
        binding.tvStatus.text = buku.status
        binding.ratingBar.rating = buku.rating
        binding.tvProgres.text =
            getString(R.string.progres_halaman, buku.halamanTerbaca, buku.jumlahHalaman)
        binding.tvLinkInfo.text =
            buku.linkInfo.ifEmpty { getString(R.string.link_kosong) }

        val persen = if (buku.jumlahHalaman > 0) buku.halamanTerbaca * 100 / buku.jumlahHalaman else 0
        binding.progressBaca.setProgressCompat(persen, true)
        tampilkanCover(buku.coverBase64)
    }

    companion object {
        const val EXTRA_BOOK_ID = "extra_book_id"
    }
}
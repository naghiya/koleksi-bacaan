package com.naghiya.koleksibacaan

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.naghiya.koleksibacaan.databinding.ActivityBookFormBinding

class BookFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookFormBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Isi pilihan status, defaultnya "Ingin Dibaca"
        val daftarStatus = resources.getStringArray(R.array.daftar_status)
        binding.actStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, daftarStatus)
        )
        binding.actStatus.setText(daftarStatus[0], false)

        binding.btnSimpan.setOnClickListener { simpan() }
    }

    private fun simpan() {
        val userId = auth.currentUser?.uid ?: return

        val judul = binding.etJudul.text.toString().trim()
        val penulis = binding.etPenulis.text.toString().trim()
        val kategori = binding.etKategori.text.toString().trim()
        val jumlahHalaman = binding.etJumlahHalaman.text.toString().toIntOrNull() ?: 0
        val halamanTerbaca = binding.etHalamanTerbaca.text.toString().toIntOrNull() ?: 0
        val status = binding.actStatus.text.toString()
        val rating = binding.ratingBar.rating
        val linkInfo = binding.etLinkInfo.text.toString().trim()

        if (!validasi(judul, penulis, kategori, jumlahHalaman, halamanTerbaca, linkInfo)) return

        val buku = Buku(
            userId = userId,
            judul = judul,
            penulis = penulis,
            kategori = kategori,
            jumlahHalaman = jumlahHalaman,
            halamanTerbaca = halamanTerbaca,
            status = status,
            rating = rating,
            linkInfo = linkInfo
        )

        tampilkanLoading(true)
        db.collection("books")
            .add(buku)
            .addOnSuccessListener {
                tampilkanLoading(false)
                Toast.makeText(this, R.string.buku_tersimpan, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                tampilkanLoading(false)
                Toast.makeText(this, R.string.buku_gagal_simpan, Toast.LENGTH_LONG).show()
            }
    }

    private fun validasi(
        judul: String,
        penulis: String,
        kategori: String,
        jumlahHalaman: Int,
        halamanTerbaca: Int,
        linkInfo: String
    ): Boolean {
        binding.tilJudul.error = null
        binding.tilPenulis.error = null
        binding.tilKategori.error = null
        binding.tilJumlahHalaman.error = null
        binding.tilHalamanTerbaca.error = null
        binding.tilLinkInfo.error = null

        var valid = true
        if (judul.isEmpty()) {
            binding.tilJudul.error = getString(R.string.error_judul_kosong)
            valid = false
        }
        if (penulis.isEmpty()) {
            binding.tilPenulis.error = getString(R.string.error_penulis_kosong)
            valid = false
        }
        if (kategori.isEmpty()) {
            binding.tilKategori.error = getString(R.string.error_kategori_kosong)
            valid = false
        }
        if (jumlahHalaman <= 0) {
            binding.tilJumlahHalaman.error = getString(R.string.error_halaman_kosong)
            valid = false
        }
        if (halamanTerbaca > jumlahHalaman) {
            binding.tilHalamanTerbaca.error = getString(R.string.error_halaman_terbaca)
            valid = false
        }
        if (linkInfo.isNotEmpty() && !Patterns.WEB_URL.matcher(linkInfo).matches()) {
            binding.tilLinkInfo.error = getString(R.string.error_link_tidak_valid)
            valid = false
        }
        return valid
    }

    private fun tampilkanLoading(tampil: Boolean) {
        binding.progressBar.visibility = if (tampil) View.VISIBLE else View.GONE
        binding.btnSimpan.isEnabled = !tampil
    }
}
package com.naghiya.koleksibacaan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Patterns
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.naghiya.koleksibacaan.databinding.ActivityBookFormBinding
import java.io.ByteArrayOutputStream

class BookFormActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookFormBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Kosong = mode tambah, terisi = mode edit
    private var bookId: String = ""
    private val modeEdit get() = bookId.isNotEmpty()

    // Cover disimpan sebagai teks Base64 (gambar yang sudah dikecilkan)
    private var coverBase64: String = ""

    private val pilihGambar =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) prosesGambar(uri)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bookId = intent.getStringExtra(BookDetailActivity.EXTRA_BOOK_ID).orEmpty()

        binding.tvJudulForm.setText(if (modeEdit) R.string.title_edit_buku else R.string.title_tambah_buku)

        val daftarStatus = resources.getStringArray(R.array.daftar_status)
        binding.actStatus.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, daftarStatus)
        )
        binding.actStatus.setText(daftarStatus[0], false)

        binding.btnPilihCover.setOnClickListener {
            pilihGambar.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        if (modeEdit) {
            binding.btnSimpan.setText(R.string.btn_simpan_perubahan)
            muatDataLama()
        }

        binding.btnSimpan.setOnClickListener { simpan() }
    }

    private fun prosesGambar(uri: Uri) {
        try {
            // 1. Baca ukuran gambar dulu, lalu kecilkan saat membuka supaya hemat memori
            val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
            var sampel = 1
            while (batas.outWidth / sampel > 1200 || batas.outHeight / sampel > 1200) sampel *= 2

            val opsi = BitmapFactory.Options().apply { inSampleSize = sampel }
            val asli = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opsi)
            } ?: throw IllegalStateException("Gambar tidak terbaca")

            // 2. Perkecil sisi terpanjang menjadi maksimal 600 piksel
            val skala = 600f / maxOf(asli.width, asli.height)
            val kecil = if (skala < 1f) {
                Bitmap.createScaledBitmap(
                    asli, (asli.width * skala).toInt(), (asli.height * skala).toInt(), true
                )
            } else asli

            // 3. Kompres ke JPEG lalu ubah jadi teks Base64
            val keluaran = ByteArrayOutputStream()
            kecil.compress(Bitmap.CompressFormat.JPEG, 75, keluaran)
            coverBase64 = Base64.encodeToString(keluaran.toByteArray(), Base64.NO_WRAP)

            tampilkanCover(kecil)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.cover_gagal, Toast.LENGTH_LONG).show()
        }
    }

    private fun tampilkanCover(bitmap: Bitmap) {
        binding.ivCover.imageTintList = null
        binding.ivCover.setPadding(0, 0, 0, 0)
        binding.ivCover.scaleType = ImageView.ScaleType.CENTER_CROP
        binding.ivCover.setImageBitmap(bitmap)
        binding.btnPilihCover.setText(R.string.btn_ganti_cover)
    }

    private fun muatDataLama() {
        tampilkanLoading(true)
        db.collection("books").document(bookId).get()
            .addOnSuccessListener { dokumen ->
                tampilkanLoading(false)
                val buku = dokumen.toObject(Buku::class.java)
                if (buku == null) {
                    Toast.makeText(this, R.string.buku_tidak_ditemukan, Toast.LENGTH_SHORT).show()
                    finish()
                    return@addOnSuccessListener
                }
                binding.etJudul.setText(buku.judul)
                binding.etPenulis.setText(buku.penulis)
                binding.etKategori.setText(buku.kategori)
                binding.etJumlahHalaman.setText(buku.jumlahHalaman.toString())
                binding.etHalamanTerbaca.setText(buku.halamanTerbaca.toString())
                binding.actStatus.setText(buku.status, false)
                binding.ratingBar.rating = buku.rating
                binding.etLinkInfo.setText(buku.linkInfo)

                if (buku.coverBase64.isNotEmpty()) {
                    coverBase64 = buku.coverBase64
                    val bytes = Base64.decode(buku.coverBase64, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { tampilkanCover(it) }
                }
            }
            .addOnFailureListener {
                tampilkanLoading(false)
                Toast.makeText(this, R.string.gagal_muat, Toast.LENGTH_LONG).show()
                finish()
            }
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
            linkInfo = linkInfo,
            coverBase64 = coverBase64
        )

        tampilkanLoading(true)
        val tugas = if (modeEdit) {
            db.collection("books").document(bookId).set(buku)
        } else {
            db.collection("books").add(buku)
        }

        tugas
            .addOnSuccessListener {
                tampilkanLoading(false)
                val pesan = if (modeEdit) R.string.buku_diperbarui else R.string.buku_tersimpan
                Toast.makeText(this, pesan, Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                tampilkanLoading(false)
                val pesan = if (modeEdit) R.string.buku_gagal_perbarui else R.string.buku_gagal_simpan
                Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
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
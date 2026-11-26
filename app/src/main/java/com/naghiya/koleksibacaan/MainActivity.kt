package com.naghiya.koleksibacaan

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.naghiya.koleksibacaan.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream
import androidx.appcompat.app.AlertDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import android.widget.EditText

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

    private val prefs by lazy { getSharedPreferences("profil", MODE_PRIVATE) }
    private fun kunciFoto() = "foto_" + (auth.currentUser?.uid ?: "")

    private val pilihFoto =
        registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) simpanFotoProfil(uri)
        }

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

        muatFotoProfil()
        muatDataMahasiswa()
        binding.btnUbahData.setOnClickListener { tampilkanDialogUbahData() }
        binding.cardProfil.setOnClickListener {
            pilihFoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }

        binding.btnUbahNama.setOnClickListener { tampilkanDialogUbahNama() }
        val bukaGaleri = {
            pilihFoto.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            )
        }
        binding.cardAkunFoto.setOnClickListener { bukaGaleri() }
        binding.btnUbahFoto.setOnClickListener { bukaGaleri() }

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

    // ---------- Foto profil (disimpan lokal di HP) ----------

    private fun muatFotoProfil() {
        val base64 = prefs.getString(kunciFoto(), null) ?: return
        val bytes = Base64.decode(base64, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.let { tampilkanFotoProfil(it) }
    }

    private fun tampilkanFotoProfil(bitmap: Bitmap) {
        for (iv in listOf(binding.ivProfil, binding.ivAkunFoto)) {
            iv.imageTintList = null
            iv.setPadding(0, 0, 0, 0)
            iv.scaleType = ImageView.ScaleType.CENTER_CROP
            iv.setImageBitmap(bitmap)
        }
    }

    private fun simpanFotoProfil(uri: Uri) {
        try {
            val batas = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, batas) }
            var sampel = 1
            while (batas.outWidth / sampel > 1000 || batas.outHeight / sampel > 1000) sampel *= 2

            val opsi = BitmapFactory.Options().apply { inSampleSize = sampel }
            val asli = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opsi)
            } ?: throw IllegalStateException("Gambar tidak terbaca")

            val skala = 300f / maxOf(asli.width, asli.height)
            val kecil = if (skala < 1f) {
                Bitmap.createScaledBitmap(
                    asli, (asli.width * skala).toInt(), (asli.height * skala).toInt(), true
                )
            } else asli

            val keluaran = ByteArrayOutputStream()
            kecil.compress(Bitmap.CompressFormat.JPEG, 80, keluaran)
            val base64 = Base64.encodeToString(keluaran.toByteArray(), Base64.NO_WRAP)

            prefs.edit().putString(kunciFoto(), base64).apply()
            tampilkanFotoProfil(kecil)
        } catch (e: Exception) {
            Toast.makeText(this, R.string.cover_gagal, Toast.LENGTH_LONG).show()
        }
    }

    // ---------- Data buku ----------

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
                perbaruiStatistik()
                terapkanFilter()
            }
    }

    private fun perbaruiStatistik() {
        val sedang = getString(R.string.status_sedang_dibaca)
        val selesai = getString(R.string.status_selesai)
        binding.tvStatTotal.text = semuaBuku.size.toString()
        binding.tvStatSedang.text = semuaBuku.count { it.status == sedang }.toString()
        binding.tvStatSelesai.text = semuaBuku.count { it.status == selesai }.toString()
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

    // ---------- Ubah nama ----------

    private fun tampilkanDialogUbahNama() {
        val user = auth.currentUser ?: return
        val tampilan = layoutInflater.inflate(R.layout.dialog_ubah_nama, null)
        val til = tampilan.findViewById<TextInputLayout>(R.id.tilNamaBaru)
        val et = tampilan.findViewById<TextInputEditText>(R.id.etNamaBaru)
        et.setText(user.displayName.orEmpty())
        et.setSelection(et.text?.length ?: 0)

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_nama_judul)
            .setView(tampilan)
            .setNegativeButton(R.string.btn_batal, null)
            .setPositiveButton(R.string.btn_simpan, null)
            .create()

        // Tombol Simpan diatur manual supaya dialog tidak menutup kalau nama kosong
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val namaBaru = et.text.toString().trim()
                til.error = null
                if (namaBaru.isEmpty()) {
                    til.error = getString(R.string.error_nama_kosong)
                } else {
                    simpanNama(user, namaBaru, dialog)
                }
            }
        }
        dialog.show()
    }

    private fun simpanNama(user: FirebaseUser, namaBaru: String, dialog: AlertDialog) {
        val tombol = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
        tombol.isEnabled = false

        val profil = userProfileChangeRequest { displayName = namaBaru }
        user.updateProfile(profil)
            .addOnSuccessListener {
                dialog.dismiss()
                binding.tvHalo.text = getString(R.string.halo_pengguna, namaBaru)
                binding.tvAkunNama.text = namaBaru
                Toast.makeText(this, R.string.nama_diperbarui, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                tombol.isEnabled = true
                Toast.makeText(this, R.string.nama_gagal, Toast.LENGTH_LONG).show()
            }
    }
    // ---------- Data mahasiswa (disimpan lokal per akun) ----------

    private fun kunciNim() = "nim_" + (auth.currentUser?.uid ?: "")
    private fun kunciProdi() = "prodi_" + (auth.currentUser?.uid ?: "")

    private fun muatDataMahasiswa() {
        val kosong = getString(R.string.nim_kosong)
        binding.tvNim.text = prefs.getString(kunciNim(), null)?.takeIf { it.isNotBlank() } ?: kosong
        binding.tvProdi.text = prefs.getString(kunciProdi(), null)?.takeIf { it.isNotBlank() } ?: kosong
    }

    private fun tampilkanDialogUbahData() {
        val tampilan = layoutInflater.inflate(R.layout.dialog_ubah_data, null)
        val etNim = tampilan.findViewById<EditText>(R.id.etNim)
        val etProdi = tampilan.findViewById<EditText>(R.id.etProdi)
        etNim.setText(prefs.getString(kunciNim(), "").orEmpty())
        etProdi.setText(prefs.getString(kunciProdi(), "").orEmpty())

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_data_judul)
            .setView(tampilan)
            .setNegativeButton(R.string.btn_batal, null)
            .setPositiveButton(R.string.btn_simpan) { _, _ ->
                prefs.edit()
                    .putString(kunciNim(), etNim.text.toString().trim())
                    .putString(kunciProdi(), etProdi.text.toString().trim())
                    .apply()
                muatDataMahasiswa()
                Toast.makeText(this, R.string.data_diperbarui, Toast.LENGTH_SHORT).show()
            }
            .show()
    }
    private fun keLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
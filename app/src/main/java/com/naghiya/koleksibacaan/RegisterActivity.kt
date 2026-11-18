package com.naghiya.koleksibacaan

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.userProfileChangeRequest
import com.naghiya.koleksibacaan.databinding.ActivityRegisterBinding

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.btnDaftar.setOnClickListener { daftar() }
        binding.tvMasuk.setOnClickListener { finish() }
    }

    private fun daftar() {
        val nama = binding.etNama.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        val konfirmasi = binding.etKonfirmasi.text.toString()

        if (!validasi(nama, email, password, konfirmasi)) return

        tampilkanLoading(true)
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { hasil ->
                val profil = userProfileChangeRequest { displayName = nama }
                hasil.user?.updateProfile(profil)?.addOnCompleteListener {
                    auth.signOut()
                    tampilkanLoading(false)
                    Toast.makeText(this, R.string.daftar_berhasil, Toast.LENGTH_LONG).show()
                    finish()
                }
            }
            .addOnFailureListener { e ->
                tampilkanLoading(false)
                val pesan = when (e) {
                    is FirebaseAuthUserCollisionException -> R.string.error_email_terpakai
                    is FirebaseNetworkException -> R.string.error_jaringan
                    else -> R.string.daftar_gagal
                }
                Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
            }
    }

    private fun validasi(nama: String, email: String, password: String, konfirmasi: String): Boolean {
        binding.tilNama.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilKonfirmasi.error = null

        var valid = true
        if (nama.isEmpty()) {
            binding.tilNama.error = getString(R.string.error_nama_kosong)
            valid = false
        }
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_kosong)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_tidak_valid)
            valid = false
        }
        if (password.length < 6) {
            binding.tilPassword.error = getString(R.string.error_password_pendek)
            valid = false
        }
        if (konfirmasi != password) {
            binding.tilKonfirmasi.error = getString(R.string.error_konfirmasi_beda)
            valid = false
        }
        return valid
    }

    private fun tampilkanLoading(tampil: Boolean) {
        binding.progressBar.visibility = if (tampil) View.VISIBLE else View.GONE
        binding.btnDaftar.isEnabled = !tampil
    }
}
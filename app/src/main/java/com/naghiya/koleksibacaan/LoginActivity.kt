package com.naghiya.koleksibacaan

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.naghiya.koleksibacaan.databinding.ActivityLoginBinding

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()

        // Sesi tersimpan: kalau sudah login, langsung ke halaman utama
        if (auth.currentUser != null) {
            keHalamanUtama()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnMasuk.setOnClickListener { masuk() }
        binding.tvDaftar.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun masuk() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()

        if (!validasi(email, password)) return

        tampilkanLoading(true)
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener {
                tampilkanLoading(false)
                keHalamanUtama()
            }
            .addOnFailureListener { e ->
                tampilkanLoading(false)
                val pesan = when (e) {
                    is FirebaseAuthInvalidCredentialsException,
                    is FirebaseAuthInvalidUserException -> R.string.error_login_salah
                    is FirebaseNetworkException -> R.string.error_jaringan
                    else -> R.string.login_gagal
                }
                Toast.makeText(this, pesan, Toast.LENGTH_LONG).show()
            }
    }

    private fun validasi(email: String, password: String): Boolean {
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        var valid = true
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_kosong)
            valid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = getString(R.string.error_email_tidak_valid)
            valid = false
        }
        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_kosong)
            valid = false
        }
        return valid
    }

    private fun tampilkanLoading(tampil: Boolean) {
        binding.progressBar.visibility = if (tampil) View.VISIBLE else View.GONE
        binding.btnMasuk.isEnabled = !tampil
    }

    private fun keHalamanUtama() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
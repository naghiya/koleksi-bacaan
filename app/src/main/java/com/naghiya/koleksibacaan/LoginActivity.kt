package com.naghiya.koleksibacaan

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
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
        binding.tvLupa.setOnClickListener { tampilkanDialogLupa() }
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

    private fun tampilkanDialogLupa() {
        val tampilan = layoutInflater.inflate(R.layout.dialog_lupa_password, null)
        val til = tampilan.findViewById<TextInputLayout>(R.id.tilEmailReset)
        val et = tampilan.findViewById<TextInputEditText>(R.id.etEmailReset)
        // Isi otomatis dengan email yang sudah diketik di layar login
        et.setText(binding.etEmail.text.toString().trim())

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(R.string.dialog_reset_judul)
            .setMessage(R.string.dialog_reset_pesan)
            .setView(tampilan)
            .setNegativeButton(R.string.btn_batal, null)
            .setPositiveButton(R.string.btn_kirim_link, null)
            .create()

        // Tombol Kirim diatur manual supaya dialog tidak menutup kalau email salah
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val email = et.text.toString().trim()
                til.error = null
                when {
                    email.isEmpty() -> til.error = getString(R.string.error_email_kosong)
                    !Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                        til.error = getString(R.string.error_email_tidak_valid)
                    else -> kirimLinkReset(email, dialog)
                }
            }
        }
        dialog.show()
    }

    private fun kirimLinkReset(email: String, dialog: AlertDialog) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = false
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                dialog.dismiss()
                Toast.makeText(this, R.string.reset_terkirim, Toast.LENGTH_LONG).show()
            }
            .addOnFailureListener { e ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).isEnabled = true
                val pesan = when (e) {
                    is FirebaseNetworkException -> R.string.error_jaringan
                    else -> R.string.reset_gagal
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
package com.naghiya.koleksibacaan

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.naghiya.koleksibacaan.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            keLogin()
            return
        }

        val nama = user.displayName?.takeIf { it.isNotBlank() } ?: user.email.orEmpty()
        binding.tvHalo.text = getString(R.string.halo_pengguna, nama)

        binding.btnKeluar.setOnClickListener {
            auth.signOut()
            keLogin()
        }
    }

    private fun keLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
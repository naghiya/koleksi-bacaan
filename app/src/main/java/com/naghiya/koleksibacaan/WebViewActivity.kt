package com.naghiya.koleksibacaan

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.naghiya.koleksibacaan.databinding.ActivityWebViewBinding

class WebViewActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWebViewBinding
    private var url: String = ""
    private var adaError = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWebViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        url = rapikanUrl(intent.getStringExtra(EXTRA_URL).orEmpty())

        binding.webView.settings.javaScriptEnabled = true
        binding.webView.settings.domStorageEnabled = true
        binding.webView.settings.loadWithOverviewMode = true
        binding.webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                adaError = false
                binding.progressBar.visibility = View.VISIBLE
                binding.layoutError.visibility = View.GONE
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                binding.progressBar.visibility = View.GONE
                if (adaError) binding.layoutError.visibility = View.VISIBLE
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                // Hanya halaman utama yang dianggap error, bukan gambar atau skrip kecil
                if (request?.isForMainFrame == true) {
                    adaError = true
                    binding.progressBar.visibility = View.GONE
                    binding.layoutError.visibility = View.VISIBLE
                }
            }
        }

        binding.btnCobaLagi.setOnClickListener { binding.webView.loadUrl(url) }

        // Tombol Back: mundur di riwayat halaman web dulu, baru keluar layar
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    finish()
                }
            }
        })

        if (savedInstanceState == null) binding.webView.loadUrl(url)
    }

    // Kalau pengguna lupa mengetik https://, kita tambahkan otomatis
    private fun rapikanUrl(mentah: String): String {
        val bersih = mentah.trim()
        return if (bersih.startsWith("http://") || bersih.startsWith("https://")) {
            bersih
        } else {
            "https://$bersih"
        }
    }

    override fun onDestroy() {
        binding.webView.destroy()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}
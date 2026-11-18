package com.naghiya.koleksibacaan

import com.google.firebase.firestore.DocumentId

data class Buku(
    @DocumentId val id: String = "",
    val userId: String = "",
    val judul: String = "",
    val penulis: String = "",
    val kategori: String = "",
    val jumlahHalaman: Int = 0,
    val halamanTerbaca: Int = 0,
    val status: String = "",
    val rating: Float = 0f,
    val linkInfo: String = ""
)
package com.naghiya.koleksibacaan

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.naghiya.koleksibacaan.databinding.ItemBukuBinding

class BukuAdapter : ListAdapter<Buku, BukuAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemBukuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBukuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val buku = getItem(position)
        val context = holder.itemView.context
        with(holder.binding) {
            tvJudul.text = buku.judul
            tvPenulis.text = buku.penulis
            tvKategori.text = buku.kategori
            tvStatus.text = buku.status
            tvRating.text = context.getString(R.string.rating_format, buku.rating)
            tvProgres.text = context.getString(
                R.string.progres_halaman, buku.halamanTerbaca, buku.jumlahHalaman
            )
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Buku>() {
            override fun areItemsTheSame(a: Buku, b: Buku) = a.id == b.id
            override fun areContentsTheSame(a: Buku, b: Buku) = a == b
        }
    }
}
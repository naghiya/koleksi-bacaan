package com.naghiya.koleksibacaan

import android.content.res.ColorStateList
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.naghiya.koleksibacaan.databinding.ItemBukuBinding

class BukuAdapter(
    private val onItemClick: (Buku) -> Unit
) : ListAdapter<Buku, BukuAdapter.ViewHolder>(DIFF) {

    class ViewHolder(val binding: ItemBukuBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBukuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val buku = getItem(position)
        val context = holder.itemView.context
        holder.itemView.setOnClickListener { onItemClick(buku) }

        with(holder.binding) {
            tvJudul.text = buku.judul
            tvPenulis.text = buku.penulis
            tvKategori.text = buku.kategori
            tvStatus.text = buku.status
            tvRating.text = context.getString(R.string.rating_format, buku.rating)
            tvProgres.text = context.getString(
                R.string.progres_halaman, buku.halamanTerbaca, buku.jumlahHalaman
            )

            val bitmap = if (buku.coverBase64.isNotEmpty()) {
                val bytes = Base64.decode(buku.coverBase64, Base64.DEFAULT)
                val opsi = BitmapFactory.Options().apply { inSampleSize = 4 }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opsi)
            } else null

            if (bitmap != null) {
                ivCover.imageTintList = null
                ivCover.setPadding(0, 0, 0, 0)
                ivCover.scaleType = ImageView.ScaleType.CENTER_CROP
                ivCover.setImageBitmap(bitmap)
            } else {
                val padding = (20 * context.resources.displayMetrics.density).toInt()
                ivCover.setImageResource(R.drawable.ic_book)
                ivCover.imageTintList =
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.kb_primary))
                ivCover.setPadding(padding, padding, padding, padding)
                ivCover.scaleType = ImageView.ScaleType.FIT_CENTER
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<Buku>() {
            override fun areItemsTheSame(a: Buku, b: Buku) = a.id == b.id
            override fun areContentsTheSame(a: Buku, b: Buku) = a == b
        }
    }
}
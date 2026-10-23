package com.example.androidapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.androidapp.R

class ProductAdapter(
    private var productList: List<Product>,
    private val onEditClick: (Product) -> Unit,
    private val onDeleteClick: (Product) -> Unit,
    private val role: String
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgProduct: ImageView = itemView.findViewById(R.id.imgProduct)
        val tvProductName: TextView = itemView.findViewById(R.id.tvProductName)
        val tvProductPrice: TextView = itemView.findViewById(R.id.tvProductPrice)
        val tvProductType: TextView = itemView.findViewById(R.id.tvProductType)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btnEdit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btnDelete)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = productList[position]

        holder.tvProductName.text = "Tên sp: ${product.name}"
        holder.tvProductPrice.text = "Giá sp: ${product.price}đ"
        holder.tvProductType.text = "Loại sp: ${product.type}"

        if (!product.imageUrl.isNullOrEmpty()) {
            com.bumptech.glide.Glide.with(holder.itemView.context)
                .load(product.imageUrl)
                // Sử dụng icon hình ảnh mặc định có sẵn của hệ thống Android (Không sợ bị thiếu file)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_delete)
                .into(holder.imgProduct)
        } else {
            // Nếu không có link ảnh thì hiển thị icon gallery mặc định của hệ thống
            holder.imgProduct.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.btnEdit.setOnClickListener { onEditClick(product) }
        holder.btnDelete.setOnClickListener { onDeleteClick(product) }

        if (role == "USER") {
            holder.btnEdit.visibility = View.GONE   // Ẩn nút hình cây bút
            holder.btnDelete.visibility = View.GONE // Ẩn nút hình thùng rác
        } else {
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.VISIBLE
        }
    }

    override fun getItemCount(): Int = productList.size

    fun updateData(newList: List<Product>) {
        this.productList = newList
        notifyDataSetChanged()
    }
}

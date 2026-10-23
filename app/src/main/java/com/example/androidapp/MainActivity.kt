package com.example.androidapp // Nhớ giữ nguyên tên package của máy bạn nếu dòng này báo đỏ

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtType: EditText
    private lateinit var edtPrice: EditText
    private lateinit var edtProductImageUrl: EditText // Đổi từ Button sang EditText
    private lateinit var btnCancelImage: ImageButton
    private lateinit var btnAdd: Button
    private lateinit var rcvProducts: RecyclerView

    private lateinit var adapter: ProductAdapter
    private var productList = mutableListOf<Product>()
    private val db = FirebaseFirestore.getInstance()
    private var editingProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edtName = findViewById(R.id.edtProductName)
        edtType = findViewById(R.id.edtProductType)
        edtPrice = findViewById(R.id.edtProductPrice)
        edtProductImageUrl = findViewById(R.id.edtProductImageUrl) // Ánh xạ ô nhập link ảnh
        btnCancelImage = findViewById(R.id.btnCancelImage)
        btnAdd = findViewById(R.id.btnAddProduct)
        rcvProducts = findViewById(R.id.rcvProducts)

        val role = intent.getStringExtra("USER_ROLE") ?: "USER"

        if (role == "USER") {
            findViewById<View>(R.id.edtProductName).visibility = View.GONE
            findViewById<View>(R.id.edtProductType).visibility = View.GONE
            findViewById<View>(R.id.edtProductPrice).visibility = View.GONE
            findViewById<View>(R.id.edtProductImageUrl).visibility = View.GONE
            findViewById<View>(R.id.btnAddProduct).visibility = View.GONE
            Toast.makeText(this, "Bạn đang đăng nhập với quyền User (Chỉ xem)", Toast.LENGTH_LONG).show()
        }

        rcvProducts.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(productList,
            onEditClick = { product ->
                edtName.setText(product.name)
                edtType.setText(product.type)
                edtPrice.setText(product.price.toString())
                edtProductImageUrl.setText(product.imageUrl) // Đổ link ảnh cũ lên ô nhập khi Sửa
                editingProductId = product.id
                btnAdd.text = "CẬP NHẬT SẢN PHẨM"
            },
            onDeleteClick = { product -> deleteProductFromFirebase(product.id) },
            role = role
        )
        rcvProducts.adapter = adapter

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        edtProductImageUrl.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (!s.isNullOrEmpty()) {
                    btnCancelImage.visibility = View.VISIBLE
                } else {
                    btnCancelImage.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnCancelImage.setOnClickListener {
            edtProductImageUrl.text.clear()
            btnCancelImage.visibility = View.GONE
        }

        btnAdd.setOnClickListener {
            saveProductDataToFirestore()
        }

        listenToProductChanges()
    }

    private fun saveProductDataToFirestore() {
        val name = edtName.text.toString().trim()
        val type = edtType.text.toString().trim()
        val priceStr = edtPrice.text.toString().trim()
        val imageUrl = edtProductImageUrl.text.toString().trim() // Lấy link ảnh từ ô nhập

        if (name.isEmpty() || type.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toLong()

        if (editingProductId != null) {
            val updatedProduct = Product(id = editingProductId!!, name = name, type = type, price = price, imageUrl = imageUrl)
            db.collection("products").document(editingProductId!!)
                .set(updatedProduct)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    resetForm()
                }
        } else {
            val productId = db.collection("products").document().id
            val product = Product(id = productId, name = name, type = type, price = price, imageUrl = imageUrl)
            db.collection("products").document(productId).set(product)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                    resetForm()
                }
        }
    }

    private fun resetForm() {
        editingProductId = null
        btnAdd.text = "THÊM SẢN PHẨM"
        edtName.text.clear()
        edtType.text.clear()
        edtPrice.text.clear()
        edtProductImageUrl.text.clear()
        btnCancelImage.visibility = View.GONE
    }

    private fun listenToProductChanges() {
        db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    productList.clear()
                    for (document in snapshot.documents) {
                        val product = document.toObject(Product::class.java)
                        if (product != null) productList.add(product)
                    }
                    adapter.updateData(productList)
                }
            }
    }

    private fun deleteProductFromFirebase(id: String) {
        db.collection("products").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa sản phẩm thành công!", Toast.LENGTH_SHORT).show()
            }
    }
}

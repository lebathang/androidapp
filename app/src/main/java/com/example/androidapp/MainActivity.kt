package com.example.androidapp

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var edtName: EditText
    private lateinit var edtType: EditText
    private lateinit var edtPrice: EditText
    private lateinit var btnSelectImage: Button
    private lateinit var btnCancelImage: ImageButton // 1. KHAI BÁO THÊM NÚT X
    private lateinit var btnAdd: Button
    private lateinit var rcvProducts: RecyclerView

    private lateinit var adapter: ProductAdapter
    private var productList = mutableListOf<Product>()
    private val db = FirebaseFirestore.getInstance()

    private var selectedImageUri: Uri? = null
    private var currentImageUrl: String = ""
    private var editingProductId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edtName = findViewById(R.id.edtProductName)
        edtType = findViewById(R.id.edtProductType)
        edtPrice = findViewById(R.id.edtProductPrice)
        btnSelectImage = findViewById(R.id.btnSelectImage)
        btnCancelImage = findViewById(R.id.btnCancelImage) // 2. ÁNH XẠ NÚT X TỪ XML
        btnAdd = findViewById(R.id.btnAddProduct)
        rcvProducts = findViewById(R.id.rcvProducts)

        val btnLogout = findViewById<Button>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        rcvProducts.layoutManager = LinearLayoutManager(this)
        adapter = ProductAdapter(productList,
            onEditClick = { product ->
                edtName.setText(product.name)
                edtType.setText(product.type)
                edtPrice.setText(product.price.toString())
                editingProductId = product.id
                currentImageUrl = product.imageUrl
                btnAdd.text = "CẬP NHẬT SẢN PHẨM"

                // Nếu sản phẩm sửa có ảnh, hiện nút hủy để người dùng có thể xóa ảnh cũ
                if (currentImageUrl.isNotEmpty()) {
                    btnCancelImage.visibility = View.VISIBLE
                }
            },
            onDeleteClick = { product -> deleteProductFromFirebase(product) }
        )
        rcvProducts.adapter = adapter

        // LẮNG NGHE KẾT QUẢ CHỌN ẢNH
        val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                selectedImageUri = uri
                btnSelectImage.text = "Đã chọn 1 ảnh ưng ý"
                btnCancelImage.visibility = View.VISIBLE // 3. HIỆN NÚT X KHI CHỌN ẢNH XONG
            }
        }

        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // 4. BẮT SỰ KIỆN KHI BẤM NÚT X (HỦY CHỌN ẢNH)
        btnCancelImage.setOnClickListener {
            selectedImageUri = null
            currentImageUrl = ""
            btnCancelImage.visibility = View.GONE // Ẩn nút X đi
            btnSelectImage.text = "Chọn hình ảnh sản phẩm" // Đổi chữ nút bấm về lại ban đầu
            Toast.makeText(this, "Đã hủy chọn ảnh", Toast.LENGTH_SHORT).show()
        }

        btnAdd.setOnClickListener {
            checkAndUploadImageFirst()
        }

        listenToProductChanges()
    }

    private fun checkAndUploadImageFirst() {
        val name = edtName.text.toString().trim()
        val type = edtType.text.toString().trim()
        val priceStr = edtPrice.text.toString().trim()

        if (name.isEmpty() || type.isEmpty() || priceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedImageUri != null) {
            Toast.makeText(this, "Đang tải ảnh lên hệ thống...", Toast.LENGTH_SHORT).show()
            val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}.jpg")

            storageRef.putFile(selectedImageUri!!)
                .addOnSuccessListener {
                    storageRef.downloadUrl.addOnSuccessListener { uri: Uri ->
                        currentImageUrl = uri.toString()
                        selectedImageUri = null
                        saveProductDataToFirestore(name, type, priceStr.toLong())
                    }
                }
                .addOnFailureListener { e: Exception ->
                    Toast.makeText(this, "Lỗi up ảnh: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveProductDataToFirestore(name, type, priceStr.toLong())
        }
    }

    private fun saveProductDataToFirestore(name: String, type: String, price: Long) {
        if (editingProductId != null) {
            val updatedProduct = Product(id = editingProductId!!, name = name, type = type, price = price, imageUrl = currentImageUrl)
            db.collection("products").document(editingProductId!!)
                .set(updatedProduct)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    resetForm()
                }
        } else {
            val productId = db.collection("products").document().id
            val product = Product(id = productId, name = name, type = type, price = price, imageUrl = currentImageUrl)
            db.collection("products").document(productId).set(product)
                .addOnSuccessListener {
                    Toast.makeText(this, "Thêm sản phẩm thành công!", Toast.LENGTH_SHORT).show()
                    resetForm()
                }
        }
    }

    private fun resetForm() {
        editingProductId = null
        currentImageUrl = ""
        selectedImageUri = null
        btnSelectImage.text = "Chọn hình ảnh sản phẩm"
        btnCancelImage.visibility = View.GONE // 5. ẨN NÚT X KHI RESET FORM
        btnAdd.text = "THÊM SẢN PHẨM"
        edtName.text.clear()
        edtType.text.clear()
        edtPrice.text.clear()
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

    // Hàm xử lý chức năng XÓA SẢN PHẨM (Xóa cả trên Storage và Firestore)
    private fun deleteProductFromFirebase(product: Product) {
        if (product.imageUrl.isNotEmpty() && product.imageUrl.contains("firebasestorage")) {
            try {
                val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(product.imageUrl)
                storageRef.delete().addOnSuccessListener {
                    deleteProductTextFromFirestore(product.id)
                }.addOnFailureListener {
                    deleteProductTextFromFirestore(product.id)
                }
            } catch (e: Exception) {
                deleteProductTextFromFirestore(product.id)
            }
        } else {
            deleteProductTextFromFirestore(product.id)
        }
    }

    // Hàm phụ xóa tài liệu trên Firestore
    private fun deleteProductTextFromFirestore(id: String) {
        db.collection("products").document(id).delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Đã xóa thành công!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi xóa dữ liệu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}

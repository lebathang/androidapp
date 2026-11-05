package com.example.androidapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val role = intent.getStringExtra("USER_ROLE") ?: "USER"
        if (role == "USER") {
            Toast.makeText(this, "Bạn đang đăng nhập với quyền User (Chỉ xem)", Toast.LENGTH_LONG).show()
        }

        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ProductScreen(role = role, db = db, onLogout = {
                        FirebaseAuth.getInstance().signOut()
                        startActivity(Intent(this@MainActivity, LoginActivity::class.java))
                        finish()
                    })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(role: String, db: FirebaseFirestore, onLogout: () -> Unit) {
    var productList by remember { mutableStateOf(listOf<Product>()) }
    var name by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var priceStr by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var editingProductId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(Unit) {
        val listener = db.collection("products")
            .addSnapshotListener { snapshot, error ->
                if (error == null && snapshot != null) {
                    val list = mutableListOf<Product>()
                    for (document in snapshot.documents) {
                        val product = document.toObject(Product::class.java)
                        if (product != null) list.add(product)
                    }
                    productList = list
                }
            }
        onDispose { listener.remove() }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text("Quản lý sản phẩm (${role})") },
            actions = {
                Button(onClick = onLogout) {
                    Text("Đăng xuất")
                }
            }
        )

        if (role == "ADMIN") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (editingProductId != null) "CẬP NHẬT SẢN PHẨM" else "THÊM SẢN PHẨM MỚI",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Tên sản phẩm") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = type,
                        onValueChange = { type = it },
                        label = { Text("Loại sản phẩm") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = priceStr,
                        onValueChange = { priceStr = it },
                        label = { Text("Giá sản phẩm") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))

                    // Ô nhập URL hình ảnh tích hợp nút xóa nhanh (thay cho TextWatcher và btnCancelImage cũ)
                    OutlinedTextField(
                        value = imageUrl,
                        onValueChange = { imageUrl = it },
                        label = { Text("Link hình ảnh (URL)") },
                        modifier = Modifier.fillMaxWidth(),
                        trailingIcon = {
                            if (imageUrl.isNotEmpty()) {
                                IconButton(onClick = { imageUrl = "" }) {
                                    Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (name.isBlank() || type.isBlank() || priceStr.isBlank()) {
                                return@Button
                            }
                            val price = priceStr.toLongOrNull() ?: 0L

                            if (editingProductId != null) {
                                // Thực hiện Cập nhật
                                val updatedProduct = Product(id = editingProductId!!, name = name, type = type, price = price, imageUrl = imageUrl)
                                db.collection("products").document(editingProductId!!).set(updatedProduct)
                            } else {
                                // Thực hiện Thêm mới
                                val id = db.collection("products").document().id
                                val newProduct = Product(id = id, name = name, type = type, price = price, imageUrl = imageUrl)
                                db.collection("products").document(id).set(newProduct)
                            }

                            // Reset trạng thái Form nhập
                            name = ""
                            type = ""
                            priceStr = ""
                            imageUrl = ""
                            editingProductId = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (editingProductId != null) "CẬP NHẬT SẢN PHẨM" else "THÊM SẢN PHẨM")
                    }
                }
            }
        }

        // Danh sách hiển thị sản phẩm dạng LazyColumn (Thay thế hoàn toàn RecyclerView và Adapter cũ)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(productList) { product ->
                ProductItem(
                    product = product,
                    role = role,
                    onEdit = {
                        name = product.name
                        type = product.type
                        priceStr = product.price.toString()
                        imageUrl = product.imageUrl
                        editingProductId = product.id
                    },
                    onDelete = {
                        db.collection("products").document(product.id).delete()
                    }
                )
            }
        }
    }
}

@Composable
fun ProductItem(product: Product, role: String, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = if (product.imageUrl.isNotBlank()) product.imageUrl.trim() else "https://placeholder.com",
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(64.dp)
                    .padding(4.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(text = "Loại: ${product.type}", style = MaterialTheme.typography.bodyMedium)
                Text(text = "Giá: ${product.price}đ", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }
            if (role == "ADMIN") {
                IconButton(onClick = onEdit) {
                    Icon(imageVector = Icons.Default.Edit, contentDescription = "Sửa", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}
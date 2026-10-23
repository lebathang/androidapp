package com.example.androidapp // Nhớ đổi tên package nếu bị báo đỏ nhé

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var btnRegister: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()

        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        btnRegister = findViewById(R.id.btnRegister)

        // Xử lý nút Đăng nhập
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tài khoản và mật khẩu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    // MẸO PHÂN QUYỀN: Chỉ cho phép tài khoản có chữ "admin" hoặc đuôi @admin.com vào hệ thống
                    if (email.contains("admin")) {
                        Toast.makeText(this, "Đăng nhập Admin thành công!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        // Nếu là user thường, đăng xuất luôn và báo lỗi phân quyền
                        auth.signOut()
                        Toast.makeText(this, "Tài khoản của bạn không có quyền Admin!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Sai tài khoản hoặc mật khẩu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }

        // Xử lý nút Đăng ký (Để bạn tự tạo tài khoản admin test đầu tiên)
        btnRegister.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty() || password.length < 6) {
                Toast.makeText(this, "Mật khẩu đăng ký phải từ 6 ký tự trở lên!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener {
                    Toast.makeText(this, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Đăng ký lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}

package com.example.androidapp

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : ComponentActivity() {

    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        setContent {
            var email by remember { mutableStateOf("") }
            var password by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "Đăng Nhập Hệ Thống", style = MaterialTheme.typography.headlineMedium)
                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email hoặc Tài khoản") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Mật khẩu") },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        val inputEmail = email.trim()
                        val inputPassword = password.trim()

                        if (inputEmail.isEmpty() || inputPassword.isEmpty()) {
                            Toast.makeText(this@LoginActivity, "Vui lòng nhập tài khoản và mật khẩu!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        auth.signInWithEmailAndPassword(inputEmail, inputPassword)
                            .addOnSuccessListener {
                                Toast.makeText(this@LoginActivity, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                                val intent = Intent(this@LoginActivity, MainActivity::class.java)

                                if (inputEmail.contains("admin")) {
                                    intent.putExtra("USER_ROLE", "ADMIN")
                                } else {
                                    intent.putExtra("USER_ROLE", "USER")
                                }

                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@LoginActivity, "Đăng nhập thất bại: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đăng Nhập")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = {
                        val inputEmail = email.trim()
                        val inputPassword = password.trim()

                        if (inputEmail.isEmpty() || inputPassword.isEmpty() || inputPassword.length < 6) {
                            Toast.makeText(this@LoginActivity, "Mật khẩu đăng ký phải từ 6 ký tự trở lên!", Toast.LENGTH_SHORT).show()
                            return@OutlinedButton
                        }

                        auth.createUserWithEmailAndPassword(inputEmail, inputPassword)
                            .addOnSuccessListener {
                                Toast.makeText(this@LoginActivity, "Đăng ký tài khoản thành công!", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this@LoginActivity, "Đăng ký lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Đăng Ký Tài Khoản")
                }
            }
        }
    }
}

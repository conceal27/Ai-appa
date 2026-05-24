package com.ai.companion.presentation

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ai.companion.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        title = "AI陪伴"
    }
}

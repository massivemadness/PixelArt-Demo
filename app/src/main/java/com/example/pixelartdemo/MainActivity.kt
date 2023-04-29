package com.example.pixelartdemo

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.pixelartdemo.ui.PixelArtView

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val pixelArtView = findViewById<PixelArtView>(R.id.pixelArtView)
        val randomColor = findViewById<Button>(R.id.random_color)

        randomColor.setOnClickListener {
            pixelArtView.cellColor = when (pixelArtView.cellColor) {
                Color.WHITE -> Color.RED
                Color.RED -> Color.GREEN
                Color.GREEN -> Color.BLUE
                else -> Color.WHITE
            }
        }
    }
}
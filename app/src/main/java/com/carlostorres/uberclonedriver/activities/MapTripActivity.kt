package com.carlostorres.uberclonedriver.activities

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.carlostorres.uberclonedriver.R
import com.carlostorres.uberclonedriver.databinding.ActivityMapTripBinding

class MapTripActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMapTripBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMapTripBinding.inflate(layoutInflater)
        setContentView(binding.root)

    }
}
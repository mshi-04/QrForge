package com.appvoyager.qrforge.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.appvoyager.qrforge.QrForge
import com.appvoyager.qrforge.QrForgeException
import com.appvoyager.qrforge.sample.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.generateButton.setOnClickListener {
            renderQrCode()
        }

        renderQrCode()
    }

    private fun renderQrCode() {
        val text = binding.qrTextInput.text?.toString().orEmpty()

        try {
            binding.qrImage.setImageBitmap(QrForge.createBitmap(text))
            binding.statusText.setText(R.string.qr_generation_success)
        } catch (error: QrForgeException) {
            binding.qrImage.setImageDrawable(null)
            binding.statusText.text = error.message
        } catch (error: IllegalArgumentException) {
            binding.qrImage.setImageDrawable(null)
            binding.statusText.text = error.message
        }
    }
}

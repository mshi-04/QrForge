package io.github.lambdarc.qrforge.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.github.lambdarc.qrforge.QrGenerationException
import io.github.lambdarc.qrforge.QrGenerator
import io.github.lambdarc.qrforge.sample.databinding.ActivityMainBinding

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
            binding.qrImage.setImageBitmap(QrGenerator.createBitmap(text))
            binding.statusText.setText(R.string.qr_generation_success)
        } catch (_: QrGenerationException.NativeLibraryUnavailable) {
            binding.qrImage.setImageDrawable(null)
            binding.statusText.setText(R.string.qr_native_unavailable)
        } catch (error: QrGenerationException) {
            binding.qrImage.setImageDrawable(null)
            binding.statusText.text = error.message
        } catch (error: IllegalArgumentException) {
            binding.qrImage.setImageDrawable(null)
            binding.statusText.text = error.message
        }
    }
}

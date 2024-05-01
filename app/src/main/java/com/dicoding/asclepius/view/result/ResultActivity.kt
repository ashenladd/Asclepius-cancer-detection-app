package com.dicoding.asclepius.view.result

import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import com.dicoding.asclepius.R
import com.dicoding.asclepius.data.local.entity.ClassificationHistory
import com.dicoding.asclepius.databinding.ActivityResultBinding
import com.dicoding.asclepius.utils.ViewModelFactory
import com.dicoding.asclepius.utils.parseClassificationResult
import com.dicoding.asclepius.view.home.HomeViewModel
import com.dicoding.asclepius.view.home.adapter.HomeAdapter
import com.dicoding.asclepius.view.model.ClassificationResult

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding
    private var data: ClassificationResult? = null

    private val factory by lazy { ViewModelFactory.getInstance(this) }

    private val resultViewModel: ResultViewModel by viewModels() {
        factory
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // TODO: Menampilkan hasil gambar, prediksi, dan confidence score.
        fetchData()
        setupData()
        setupToolbar()
        setupObserver()
    }

    private fun setupObserver() {
        resultViewModel.isSaved.observe(this) {
            showSaved(it)
        }
    }

    private fun showSaved(isSaved: Boolean){
        if (isSaved){
            binding.apply {
                lytResult.toolbar.menu.findItem(R.id.save).isEnabled = false
                lytResult.toolbar.menu.findItem(R.id.save).setIcon(R.drawable.ic_save_unactive_bg)
            }
            Toast.makeText(this, getString(R.string.label_data_saved), Toast.LENGTH_SHORT).show()
        }else{
            binding.apply {
                lytResult.toolbar.menu.findItem(R.id.save).isEnabled = true
                lytResult.toolbar.menu.findItem(R.id.save).setIcon(R.drawable.ic_save_bg)
            }
        }

    }

    private fun setupToolbar() {
        binding.apply {
            lytResult.toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            lytResult.toolbar.title = getString(R.string.result)
            lytResult.toolbar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.save -> {
                        val savedData = ClassificationHistory(
                            imageUri = data?.imageUri.toString(),
                            classifications = data?.classifications.toString(),
                            timestamp = System.currentTimeMillis().toString()
                        )
                        resultViewModel.saveResult(savedData)
                        true
                    }
                    else -> {
                        false
                    }
                }
            }
        }
    }

    private fun setupData() {
        data?.let {
            binding.apply {
                resultImage.setImageURI(it.imageUri)
                resultText.text = it.classifications.parseClassificationResult()
                if(it.classifications.split("\n")[0].contains("non", ignoreCase = true)) {
                    binding.predictionImage.setImageResource(R.drawable.ic_smile)
                }else{
                    binding.predictionImage.setImageResource(R.drawable.ic_sad)
                }
            }
        }
    }

    private fun fetchData() {
        data = if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(
                EXTRA_RESULT,
                ClassificationResult::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_RESULT)
        }
    }

    companion object{
        const val EXTRA_RESULT = "extra_result"
    }

}
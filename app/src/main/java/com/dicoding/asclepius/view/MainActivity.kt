package com.dicoding.asclepius.view

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.dicoding.asclepius.R
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.dicoding.asclepius.helper.ImageClassifierHelper
import com.dicoding.asclepius.view.model.ClassificationResult
import com.dicoding.asclepius.view.result.ResultActivity
import com.yalantis.ucrop.UCrop
import org.tensorflow.lite.task.vision.classifier.Classifications
import java.io.File
import java.text.NumberFormat

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    private var currentImageUri: Uri? = null

    private lateinit var imageClassifierHelper: ImageClassifierHelper

    private val uCropContract = object : ActivityResultContract<List<Uri>, Uri>(){
        override fun createIntent(context: Context, input: List<Uri>): Intent {
            val inputUri = input[0]
            val outputUri = input[1]

            val uCrop = UCrop.of(inputUri, outputUri)
                .withAspectRatio(1f, 1f)
                .withMaxResultSize(1920, 1080)

            return uCrop.getIntent(context)
        }

        override fun parseResult(resultCode: Int, intent: Intent?): Uri {
            if (resultCode == RESULT_OK) {
                return UCrop.getOutput(intent!!)!!
            }
            return Uri.parse("")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClick()
        setupToolbar()
    }

    private fun setupToolbar() {
        binding.apply {
            lytMain.toolbar.setNavigationOnClickListener {
                onBackPressedDispatcher.onBackPressed()
            }
            lytMain.toolbar.title = getString(R.string.label_cancer_detection)
        }
    }

    private fun setupClick() {
        binding.apply {
            galleryButton.setOnClickListener {
                startGallery()
            }
            analyzeButton.setOnClickListener{
                analyzeImage()
            }
        }
    }

    private val launcherGallery = registerForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        val outputUri = Uri.fromFile(File.createTempFile("temp", ".jpg"))

        if (uri != null) {
            cropImage.launch(
                listOf(
                    uri,
                    outputUri
                )
            )
        } else {
            Log.d(
                "Photo Picker",
                "No media selected"
            )
        }
    }

    private val cropImage = registerForActivityResult(uCropContract){ uri : Uri?->
        if (uri != null && uri != Uri.parse("")) {
            currentImageUri = uri
            showImage()
        } else {
            Log.d(
                "Photo Picker",
                "No media selected"
            )
        }
    }

    private fun startGallery() {
        launcherGallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    private fun showImage() {
        currentImageUri?.let {
            Log.d("Image URI", "showImage: $it")
            binding.previewImageView.setImageURI(it)
        }
    }

    private fun analyzeImage() {
        if (currentImageUri == null) {
            showEmptyWarning()
            return
        }
        showLoading(true)
        imageClassifierHelper = ImageClassifierHelper(
            context = this,
            classifierListener = object : ImageClassifierHelper.ClassifierListener {
                override fun onError(error: String) {
                    runOnUiThread {
                        showToast(error)
                        showLoading(false)
                    }
                }

                override fun onResults(results: List<Classifications>?) {
                    runOnUiThread {
                        results?.let { it ->
                            if (it.isNotEmpty() && it[0].categories.isNotEmpty()) {
                                println(it)
                                val sortedCategories =
                                    it[0].categories.sortedByDescending { it?.score }
                                val displayResult =
                                    sortedCategories.joinToString("\n") {
                                        "${it.label} " + NumberFormat.getPercentInstance()
                                            .format(it.score).trim()
                                    }
                                val classificationResult = ClassificationResult(
                                    imageUri = currentImageUri ?: Uri.parse(""),
                                    classifications = displayResult,
                                    timestamp = System.currentTimeMillis().toString()
                                )
                                showLoading(false)
                                moveToResult(classificationResult)
                            } else {
                                showLoading(false)
                                showToast("No result")
                            }
                        }
                    }
                }
            }
        )
        currentImageUri?.let { imageClassifierHelper.classifyStaticImage(it) }
    }

    private fun moveToResult(classificationResult : ClassificationResult) {
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra(ResultActivity.EXTRA_RESULT, classificationResult)
        startActivity(intent)
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun showLoading(isLoading: Boolean) {
        binding.progressIndicator.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showEmptyWarning() {
        showToast(getString(R.string.empty_image_warning))
    }
}
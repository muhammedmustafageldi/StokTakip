package com.swanky.stoktakip.activities

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.InputType
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.widget.addTextChangedListener
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.swanky.stoktakip.R
import com.swanky.stoktakip.databinding.ActivityAddProductBinding
import com.swanky.stoktakip.databinding.LayoutWarningDailogBinding
import com.swanky.stoktakip.services.CustomAlert
import com.swanky.stoktakip.services.FileTransactions
import com.swanky.stoktakip.services.PermissionCallback
import com.swanky.stoktakip.services.PermissionManager
import java.io.File
import java.util.UUID

class AddProductActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddProductBinding
    private lateinit var galleryIntentLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraIntentLauncher: ActivityResultLauncher<Void?>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var imageData: Uri? = null
    private lateinit var category: String
    private lateinit var database: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var temporaryFile: File? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initPhoto()
        initDatabaseObjects()
        getCategoryData()
        stockControl()
        registerLauncher()

        binding.addProductButton.setOnClickListener {
            saveData()
        }
        binding.backButtonAdd.setOnClickListener {
            onBackPressed()
        }
    }

    private fun initDatabaseObjects() {
        database = Firebase.firestore
        storage = Firebase.storage
    }

    private fun initPhoto() {
        // Define default image uri.
        imageData = Uri.parse("android.resource://$packageName/${R.drawable.product}")

        binding.addProductImage.setOnClickListener {
            showGalleryOrCameraAlert()
        }
    }

    private fun getCategoryData() : HashSet<String>{
        val categoryList = HashSet<String>()

        database.collection("Products").get().addOnSuccessListener {results ->
            // Get categories data
            for (document in results){
                val category = document.getString("category") as String
                categoryList.add(category)
            }

            // Init drop down item
            initDropDown(categoryList)

        }.addOnFailureListener {
            println(it.message)
            showErrorDialog()
        }

        return categoryList
    }

    private fun initDropDown(categoryList: HashSet<String>) {
        // Create while preserving LinkedHashSet order
        val orderedCategoryList = LinkedHashSet<String>().apply {
            add("Kategori ekle") // First add the “Add category” item
            addAll(categoryList) // Then add the existing categories
        }

        val arrayAdapter = ArrayAdapter(this, R.layout.dropdown_item, orderedCategoryList.toList())

        binding.autoCompleteText.apply {
            setAdapter(arrayAdapter)
            setOnItemClickListener { _, _, position, _ ->
                // Check keyboard ->
                val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

                if (arrayAdapter.getItem(position) == "Kategori ekle") {
                    // When “Add category” is selected
                    setText("")
                    hint = "Bir kategori ismi girin."
                    inputType = InputType.TYPE_CLASS_TEXT

                    requestFocus()
                    imm.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)

                    // Listen to the change ->
                    addTextChangedListener {
                        category = it.toString()
                    }
                } else {
                    hint = null
                    clearFocus()
                    imm.hideSoftInputFromWindow(this.windowToken, 0)
                    category = adapter.getItem(position).toString()
                    dismissDropDown()
                }
            }
        }
    }

    private fun saveData() {
        val productNameTxt = binding.addProductNameTxt
        val productPriceTxt = binding.addProductPriceTxt
        val quantityTxt = binding.addProductQuantity

        if (editTextValidator(productNameTxt, productPriceTxt)) {
            // Upload image
            setLoadingHolder(loading = true)
            val uuid = UUID.randomUUID()
            val imageName = "$uuid.png"
            val imageRef = storage.reference.child("productImages/").child(imageName)

            imageRef.putFile(imageData!!).addOnSuccessListener {
                // If success create product and upload it.
                imageRef.downloadUrl.addOnSuccessListener {
                    val imageUrl = it.toString()

                    val product = hashMapOf(
                        "category" to category,
                        "imageUrl" to imageUrl,
                        "numberOfSales" to 0,
                        "price" to productPriceTxt.text.toString().toDouble(),
                        "productName" to productNameTxt.text.toString(),
                        "quantity" to quantityTxt.text.toString().toLong(),
                    )

                    database.collection("Products").add(product).addOnSuccessListener {
                        Toast.makeText(this, "Ürün ekleme işlemi tamamlandı.", Toast.LENGTH_SHORT).show()
                        setLoadingHolder(loading = false)
                    }
                }

            }.addOnProgressListener { snapshot ->
                // Calculate progress
                val progress: Double = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount)
                binding.uploadProgress.progress = progress.toInt()

            }.addOnFailureListener {
                Toast.makeText(
                    this,
                    "Fotoğraf sunucuya yüklenirken bir hata oluştu. Lütfen tekrar deneyin.",
                    Toast.LENGTH_SHORT
                ).show()
                setLoadingHolder(loading = false)
            }
        }

    }

    private fun editTextValidator(
        productNameTxt: TextInputEditText,
        priceTxt: TextInputEditText,
    ): Boolean {
        val productName = productNameTxt.text
        val price = priceTxt.text

        return if (productName!!.isEmpty()) {
            productNameTxt.error = "Geçersiz ürün ismi."
            false
        } else if (price!!.isEmpty() || price.toString().toDouble() < 0) {
            priceTxt.error = "Geçersiz fiyat bilgisi."
            false
        } else if (category.isEmpty()) {
            binding.autoCompleteText.error = "Lütfen bir kategori seçimi yapın."
            false
        } else {
            true
        }
    }

    private fun stockControl() {
        val plusButton = binding.addProductPlus
        val minusButton = binding.addProductMinus
        val quantityText = binding.addProductQuantity

        plusButton.setOnClickListener {
            var quantity = Integer.parseInt(quantityText.text.toString())
            quantity += 1
            quantityText.text = quantity.toString()
        }

        minusButton.setOnClickListener {
            var quantity = Integer.parseInt(quantityText.text.toString())
            if (quantity > 0) {
                quantity -= 1
                quantityText.text = quantity.toString()
            } else {
                Toast.makeText(this, "Ürün stok sayısı 0 dan az olamaz.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerLauncher() {
        val fileTransactions = FileTransactions(context = this)

        galleryPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    val intentToGallery =
                        Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                    galleryIntentLauncher.launch(intentToGallery)
                } else {
                    Toast.makeText(this, "Galeri için izin gerekli.", Toast.LENGTH_SHORT).show()
                }
            }

        cameraPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    cameraIntentLauncher.launch(null)
                } else {
                    Toast.makeText(this, "Kamera erişimi için izin gerekli.", Toast.LENGTH_SHORT)
                        .show()
                }
            }

        galleryIntentLauncher =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == RESULT_OK) {
                    val intentFromResult = result.data
                    if (intent != null) {

                        imageData = intentFromResult?.data

                        try {
                            if (Build.VERSION.SDK_INT >= 28) {
                                val source = ImageDecoder.createSource(contentResolver, imageData!!)
                                val imageBitmap = fileTransactions.makeSmallerImage(
                                    ImageDecoder.decodeBitmap(source)
                                )
                                binding.addProductImage.setImageBitmap(imageBitmap)
                            } else {
                                val imageBitmap = fileTransactions.makeSmallerImage(
                                    MediaStore.Images.Media.getBitmap(
                                        contentResolver,
                                        imageData
                                    )
                                )
                                binding.addProductImage.setImageBitmap(imageBitmap)
                            }
                        } catch (e: Exception) {
                            println(e.message)
                            Toast.makeText(
                                this,
                                "Fotoğraf verisi alınırken bir hata gerçekleşti. Tekrar deneyin.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                }
            }

        cameraIntentLauncher =
            registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { result ->
                if (result != null) {
                    temporaryFile?.delete()
                    temporaryFile = fileTransactions.createEmptyFile()
                    temporaryFile?.let {
                        fileTransactions.saveImageToFile(
                            it,
                            fileTransactions.makeSmallerImage(result)
                        )
                        // Take uri of image
                        imageData =
                            FileProvider.getUriForFile(this, "com.swanky.stoktakip.provider", it)
                        binding.addProductImage.setImageURI(imageData)
                    }
                } else {
                    Toast.makeText(
                        this,
                        "Fotoğraf verisi alınırken bir hata gerçekleşti. Tekrar deneyin.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

    }

    private fun showGalleryOrCameraAlert() {
        val customAlert = CustomAlert(
            context = this, "Fotoğraf Seç",
            "Ürünün fotoğrafını değiştirmek için galeriden bir fotoğraf seçin ya da kamera ile yeni bir fotoğraf çekin.",
            "Kamera",
            "Galeri",
            R.drawable.camera
        )

        val alertAndBinding = customAlert.createDialog()

        val alert = alertAndBinding["alertDialog"] as AlertDialog
        val alertBinding = alertAndBinding["dialogBinding"] as LayoutWarningDailogBinding

        alertBinding.buttonPositive.setOnClickListener {
            // Camera selected
            requestCameraPermission()
            alert.cancel()
        }

        alertBinding.buttonNegative.setOnClickListener {
            // Gallery Selected
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // For android version 33+
                requestGalleryPermission(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                // For android version until 33
                requestGalleryPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            alert.cancel()
        }

        alert.show()
    }

    private fun requestGalleryPermission(permissionString: String) {
        PermissionManager(object : PermissionCallback {
            override fun onPermissionGranted() {
                // Permission is granted. Go to gallery
                galleryIntentLauncher.launch(
                    Intent(
                        Intent.ACTION_PICK,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    )
                )
            }

            override fun onPermissionDenied() {
                // Require permission
                galleryPermissionLauncher.launch(permissionString)
            }

            override fun requireRationale() {
                Snackbar.make(
                    binding.root,
                    "Galeri erişimi için izin gerekli.",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("İzin ver") {
                    // Request permission
                    galleryPermissionLauncher.launch(permissionString)
                }.show()
            }

        }).takeGalleryPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun requestCameraPermission() {
        PermissionManager(object : PermissionCallback {
            override fun onPermissionGranted() {
                // Permission is granted go to camera
                cameraIntentLauncher.launch(null)
            }

            override fun onPermissionDenied() {
                // Request Permission
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }

            override fun requireRationale() {
                // Show rationale and request permission
                Snackbar.make(
                    binding.root,
                    "Kamera erişimi için izin gerekli.",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("İzin ver") {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }.show()
            }

        }).takeCameraPermission(this)
    }

    private fun setLoadingHolder(loading: Boolean) {
        // Set layout
        // Define required items
        val loadingLinear = binding.loadingLinearAddPoduct
        val mainConstraint = binding.mainConstAddProduct
        val loadingAnim = binding.productUploadAnim

        if (loading) {
            // Hide main layout and show loading layout
            mainConstraint.visibility = View.GONE
            loadingLinear.visibility = View.VISIBLE
        } else {
            // Hide loading layout and show main layout
            loadingAnim.cancelAnimation()
            loadingLinear.visibility = View.GONE
            mainConstraint.visibility = View.VISIBLE
        }
    }

    private fun showErrorDialog(){
        val alert = CustomAlert(this, "Hata",
            "Kategori verileri yüklenirken bir hata oluştu. Lütfen ana ekrandaki teknik destek seçeneği ile hatayı bize iletin.",
            "Tamam", "", R.drawable.ico_warning)

        val dialogAndBinding = alert.createDialog()
        val alertDialog = dialogAndBinding["alertDialog"] as AlertDialog
        val dialogBinding = dialogAndBinding["dialogBinding"] as LayoutWarningDailogBinding

        // Hide negative button
        dialogBinding.buttonNegative.visibility = View.INVISIBLE

        dialogBinding.buttonPositive.setOnClickListener {
            alertDialog.cancel()
            val backToMainIntent = Intent(this@AddProductActivity, MainActivity :: class.java)
            backToMainIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(backToMainIntent)
        }

        alertDialog.setCancelable(false)
        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        temporaryFile?.delete()
    }


}
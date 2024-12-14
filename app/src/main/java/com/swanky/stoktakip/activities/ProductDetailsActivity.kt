package com.swanky.stoktakip.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.doOnPreDraw
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storage
import com.squareup.picasso.Picasso
import com.swanky.stoktakip.R
import com.swanky.stoktakip.databinding.ActivityProductDetailsBinding
import com.swanky.stoktakip.databinding.BottomSheetUpdateDialogBinding
import com.swanky.stoktakip.databinding.LayoutWarningDailogBinding
import com.swanky.stoktakip.models.Product
import com.swanky.stoktakip.services.CustomAlert
import com.swanky.stoktakip.services.FileTransactions
import com.swanky.stoktakip.services.PermissionCallback
import com.swanky.stoktakip.services.PermissionManager
import java.io.File


class ProductDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductDetailsBinding
    private lateinit var database: FirebaseFirestore
    private lateinit var storageRef: FirebaseStorage
    private lateinit var bottomSheetDialog: BottomSheetDialog
    private lateinit var bottomDialogBinding: BottomSheetUpdateDialogBinding
    private lateinit var galleryIntentLauncher: ActivityResultLauncher<Intent>
    private lateinit var galleryPermissionLauncher: ActivityResultLauncher<String>
    private lateinit var cameraIntentLauncher: ActivityResultLauncher<Void?>
    private lateinit var cameraPermissionLauncher: ActivityResultLauncher<String>
    private var imageData: Uri? = null
    private var temporaryFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val intent = intent
        val product = intent.getSerializableExtra("product") as Product

        setContents(product)
        initDatabaseObjects()
        registerActivityLauncher()

        binding.backButton.setOnClickListener { onBackPressed() }

        binding.detailsUpdateButton.setOnClickListener {
            // Show product details on the bottom sheet dialog
            showBottomSheetDialog(product)
        }

        binding.makeSaleProductButton.setOnClickListener {
            // Go to make sale activity for sale
            val intentToMakeSale = Intent(this, MakeSaleActivity:: class.java)
            intentToMakeSale.putExtra("productToSell", product)
            startActivity(intentToMakeSale)
        }
    }

    private fun initDatabaseObjects() {
        database = Firebase.firestore
        storageRef = Firebase.storage
    }

    @SuppressLint("SetTextI18n")
    private fun setContents(product: Product) {
        ActivityCompat.postponeEnterTransition(this)

        // Put data
        Picasso.get().load(product.imageUrl).into(binding.productDetailsImg)
        binding.productNameTxt.text = product.productName
        binding.productPriceTxt.text = product.price.toString() + " TL"
        binding.productCategoryTxt.text = product.category
        binding.productNumberOfSalesTxt.text = "Bu ürünün toplam satış sayısı: ${product.numberOfSales}"

        // Stock control and put data.
        binding.productStockTxt.text = "Stok Sayısı: ${product.quantity}"

        if (product.quantity in 1 until 3){
            binding.nearlyExhaustedTxt.visibility = View.VISIBLE
        }else if(product.quantity == 0L){
            binding.productDetailsImg.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f)})
            binding.makeSaleProductButton.apply {
                isEnabled = false
                text = "Stokta yok"
            }
            binding.nearlyExhaustedTxt.apply {
                text = "( Tükendi )"
                visibility = View.VISIBLE
                setTextColor(ContextCompat.getColor(this@ProductDetailsActivity, R.color.my_red))
            }
        }

        binding.productDetailsImg.let {
            it.doOnPreDraw {
                ViewCompat.setTransitionName(binding.productDetailsImg, "sharedProductImage")
                ActivityCompat.startPostponedEnterTransition(this)
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun showBottomSheetDialog(product: Product) {
        bottomDialogBinding = BottomSheetUpdateDialogBinding.inflate(LayoutInflater.from(this))

        bottomSheetDialog = BottomSheetDialog(this, R.style.BottomSheetDialogTheme)
        bottomSheetDialog.apply {
            setContentView(bottomDialogBinding.root)
            this.behavior.state = BottomSheetBehavior.STATE_EXPANDED
            setCancelable(true)
        }

        // Put data to elements ->
        val productNameTxt = bottomDialogBinding.bottomSheetProductNameTxt
        val productPriceTxt = bottomDialogBinding.bottomSheetPriceTxt
        val productQuantityTxt = bottomDialogBinding.bottomSheetQuantity

        // Load image of bottom sheet
        val bottomSheetImg = bottomDialogBinding.bottomSheetImg
        Picasso.get().load(product.imageUrl).placeholder(R.drawable.loading)
            .into(bottomSheetImg)
        productNameTxt.setText(product.productName)
        productQuantityTxt.text = product.quantity.toString()
        productPriceTxt.setText(product.price.toString())

        bottomSheetImg.setOnClickListener {
            // Show gallery or camera access alert dialog
            showGalleryOrCameraAlert()
        }

        // Stock Control ->
        stockControl(
            bottomDialogBinding.bottomSheetPlus,
            bottomDialogBinding.bottomSheetMinus,
            productQuantityTxt
        )

        // Update Transaction
        bottomDialogBinding.bottomSheetUpdate.setOnClickListener {
            // Edit text validate transaction
            if (editTextValidator(productNameTxt, productPriceTxt)) {
                // Inputs are valid
                val newProductName = productNameTxt.text.toString()
                val newPrice = productPriceTxt.text.toString().toDouble()
                val newQuantity = productQuantityTxt.text.toString().toLong()
                val applyAllCategory = bottomDialogBinding.bottomSheetCheckBox.isChecked

                // Set new values to object
                product.productName = newProductName
                product.price = newPrice
                product.quantity = newQuantity

                updateProduct(product = product, applyAllCategory = applyAllCategory)
            }
        }

        // Define buttons
        bottomDialogBinding.bottomSheetCancel.setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomDialogBinding.bottomSheetDelete.setOnClickListener {
            showDeleteAlert(product)
        }

        bottomSheetDialog.show()
    }

    private fun stockControl(plusButton: ImageButton, minusButton: ImageButton, quantityText: TextView) {
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

    private fun editTextValidator(productNameTxt: TextInputEditText, priceTxt: TextInputEditText): Boolean {
        val newProductName = productNameTxt.text
        val newPrice = priceTxt.text
        return if (newProductName!!.isEmpty()) {
            productNameTxt.error = "Geçersiz ürün ismi."
            false
        } else if (newPrice!!.isEmpty() || newPrice.toString().toDouble() < 0) {
            priceTxt.error = "Geçersiz fiyat bilgisi."
            false
        } else {
            true
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showDeleteAlert(product: Product) {
        val customAlert = CustomAlert(this,"Ürünü Sil",
            "Seçili ürün depodan silinecek onaylıyor musunuz?\n(Bu işlem geri alınamaz.)",
            "Onayla", "Vazgeç",
        R.drawable.ico_delete)

        val alertAndBinding = customAlert.createDialog()

        val alert = alertAndBinding["alertDialog"] as AlertDialog
        val binding = alertAndBinding["dialogBinding"] as LayoutWarningDailogBinding

        binding.buttonPositive.setOnClickListener {
            deleteProduct(product)
            alert.cancel()
        }

        binding.buttonNegative.setOnClickListener {
            alert.cancel()
        }

        alert.show()
    }

    private fun showGalleryOrCameraAlert(){
        val customAlert = CustomAlert(this,"Fotoğraf Seç",
            "Ürünün fotoğrafını değiştirmek için galeriden bir fotoğraf seçin ya da kamera ile yeni bir fotoğraf çekin.",
            "Kamera",
            "Galeri",
            R.drawable.camera)

        val alertAndBinding = customAlert.createDialog()

        val alert = alertAndBinding["alertDialog"] as AlertDialog
        val alertBinding  = alertAndBinding["dialogBinding"] as LayoutWarningDailogBinding

        alertBinding.buttonPositive.setOnClickListener {
            // Camera selected
            requestCameraPermission(bottomDialogBinding.root)
            alert.cancel()
        }

        alertBinding.buttonNegative.setOnClickListener {
            // Gallery Selected
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU){
                // For android version 33+
                requestGalleryPermission(Manifest.permission.READ_MEDIA_IMAGES, bottomDialogBinding.root)
            }else{
                // For android version until 33
                requestGalleryPermission(Manifest.permission.READ_EXTERNAL_STORAGE, bottomDialogBinding.root)
            }
            alert.cancel()
        }

        alert.show()
    }

    @SuppressLint("SetTextI18n")
    private fun deleteProduct(product: Product) {
        buttonLoading()

        database.collection("Products").document(product.id).delete()
        storageRef.getReferenceFromUrl(product.imageUrl).delete().addOnSuccessListener {
            // Notify recycler view that the product has been deleted
            val deleteIntent = Intent()
            deleteIntent.putExtra("deletedProduct", product)
            setResult(Activity.RESULT_OK, deleteIntent)
            finish()
        }
    }

    private fun requestGalleryPermission(permissionString: String, view: View){
        PermissionManager(object: PermissionCallback{
            override fun onPermissionGranted() {
                // Permission is granted. Go to gallery
                galleryIntentLauncher.launch(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
            }

            override fun onPermissionDenied() {
                // Require permission
                galleryPermissionLauncher.launch(permissionString)
            }

            override fun requireRationale() {
                Snackbar.make(view, "Galeri erişimi için izin gerekli.", Snackbar.LENGTH_INDEFINITE).setAction("İzin ver") {
                    // Request permission
                    galleryPermissionLauncher.launch(permissionString)
                }.show()
            }

        }).takeGalleryPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun requestCameraPermission(view: View){
        PermissionManager(object: PermissionCallback{
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
                Snackbar.make(view, "Kamera erişimi için izin gerekli.", Snackbar.LENGTH_INDEFINITE).setAction("İzin ver"){
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }.show()
            }

        }).takeCameraPermission(this)
    }

    private fun registerActivityLauncher(){
        // Create file transaction
        val fileTransaction = FileTransactions(this)

        galleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if (result){
                val intentToGallery =  Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                galleryIntentLauncher.launch(intentToGallery)
            }else{
                Toast.makeText(this, "Galeri için izin gerekli.", Toast.LENGTH_SHORT).show()
            }
        }

        cameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){result ->
            if (result){
                cameraIntentLauncher.launch(null)
            }else{
                Toast.makeText(this, "Kamera erişimi için izin gerekli.", Toast.LENGTH_SHORT).show()
            }
        }

        galleryIntentLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val intentFromResult = result.data
                if (intent != null) {

                    imageData = intentFromResult?.data

                    try {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source = ImageDecoder.createSource(contentResolver, imageData!!)
                            val imageBitmap = fileTransaction.makeSmallerImage(ImageDecoder.decodeBitmap(source))
                            bottomDialogBinding.bottomSheetImg.setImageBitmap(imageBitmap)
                        } else {
                            val imageBitmap = fileTransaction.makeSmallerImage(MediaStore.Images.Media.getBitmap(contentResolver, imageData))
                            bottomDialogBinding.bottomSheetImg.setImageBitmap(imageBitmap)
                        }
                    } catch (e: Exception) {
                        println(e.message)
                        Toast.makeText(this, "Fotoğraf verisi alınırken bir hata gerçekleşti. Tekrar deneyin.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        cameraIntentLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()){result ->
            if (result != null){
                temporaryFile?.delete()
                temporaryFile = fileTransaction.createEmptyFile()
                temporaryFile?.let {
                    fileTransaction.saveImageToFile(it, fileTransaction.makeSmallerImage(result))
                    // Take uri of image
                    imageData = FileProvider.getUriForFile(this, "com.swanky.stoktakip.provider", it)
                    bottomDialogBinding.bottomSheetImg.setImageURI(imageData)
                }
            }else{
                Toast.makeText(this, "Fotoğraf verisi alınırken bir hata gerçekleşti. Tekrar deneyin.", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun updateProduct(product: Product, applyAllCategory: Boolean) {
        buttonLoading()
        bottomSheetDialog.setCancelable(false)

        val allTasks = arrayListOf<Task<*>>()

        allTasks.add(database.collection("Products").document(product.id)
            .update(
                mapOf(
                    "productName" to product.productName,
                    "price" to product.price,
                    "quantity" to product.quantity
                )
            ))

        if (applyAllCategory){
            val filteredQuery = database.collection("Products").whereEqualTo("category", product.category)
            allTasks.add(filteredQuery.get().addOnSuccessListener { documents ->
                for (document in documents){
                    val documentId = document.id
                    val fieldToUpdate = product.price
                    updateAllCategoryPrice(documentId, fieldToUpdate)
                }
                Toast.makeText(this@ProductDetailsActivity, "Fiyat bilgisi bütün kategoriye uygulandı.", Toast.LENGTH_LONG).show()
            })
        }

        // Update image
        if (imageData != null){
            val imageRef = storageRef.getReferenceFromUrl(product.imageUrl)
            val uploadImageTask = imageRef.putFile(imageData!!)
            allTasks.add(uploadImageTask)
        }


        Tasks.whenAllSuccess<Any>(*allTasks.toTypedArray()).addOnSuccessListener {
            updateProductView(product = product)
        }
    }

    private fun updateProductView(product: Product) {
        bottomSheetDialog.dismiss()

        startAnimationAndChangeTxt(binding.productNameTxt, product.productName)
        startAnimationAndChangeTxt(binding.productPriceTxt,  "${product.price} TL")
        startAnimationAndChangeTxt(binding.productStockTxt, "Stok sayısı: ${product.quantity}")
        // Anim for image ->
        YoYo.with(Techniques.Pulse)
            .delay(400)
            .duration(1000)
            .playOn(binding.productDetailsImg)

        // Change anim data
        binding.productDetailsImg.setImageURI(imageData)

        binding.nearlyExhaustedTxt.visibility = View.GONE
    }

    private fun startAnimationAndChangeTxt(destinationView: TextView, newString: String){
        destinationView.text = newString
        YoYo.with(Techniques.Pulse)
            .delay(400)
            .duration(1000)
            .playOn(destinationView)
    }

    private fun updateAllCategoryPrice(documentId: String, fieldToUpdate: Double){
        val updatedData = mapOf(
            "price" to fieldToUpdate
        )
        database.collection("Products").document(documentId).update(updatedData)
    }

    private fun buttonLoading(){
        bottomDialogBinding.bottomSheetUpdate.apply {
            text = ""
            isEnabled = false
        }
        bottomDialogBinding.bottomSheetProgresBar.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        super.onDestroy()
        temporaryFile?.delete()
    }

}
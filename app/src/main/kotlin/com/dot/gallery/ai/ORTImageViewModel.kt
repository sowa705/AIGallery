package com.dot.gallery.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.app.Application
import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList
import java.util.Collections
import com.dot.gallery.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Inject

class ORTImageViewModel(application: Application) : AndroidViewModel(application) {
    companion object{
        val recalulatedSinceLaunch = MutableLiveData<Boolean>(false);
        public val mcurrentFile = MutableLiveData<String>("")
    }
    private var ortEnv: OrtEnvironment = OrtEnvironment.getEnvironment()
    private var repository: ImageEmbeddingRepository
    var imageList: ArrayList<ImageData> = arrayListOf()
    var progress: MutableLiveData<Double> = MutableLiveData(0.0)

    init {
        val imageEmbeddingDao = ImageEmbeddingDatabase.getDatabase(application).imageEmbeddingDao()
        repository = ImageEmbeddingRepository(imageEmbeddingDao)
    }
    fun getAllMediaFilesOnDevice(contentResolver: ContentResolver): Cursor? {
        try {
            val projection = arrayOf (
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                MediaStore.Images.Media.RELATIVE_PATH,
                MediaStore.Images.Media.DISPLAY_NAME
            )
            val cursor = contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                null
            )
            return cursor
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
    suspend fun getEmbedding(id: Long): FloatArray? {
        val record = repository.getRecord(id) as ImageEmbedding?
        return record?.embedding
    }

    fun generateIndex() {
        if (recalulatedSinceLaunch.value == true) {
            return
        }

        // draw toast
        Toast.makeText(getApplication(), "Generating Index...", Toast.LENGTH_SHORT).show()

        val modelID = R.raw.visual_quant
        val resources = getApplication<Application>().resources
        val model = resources.openRawResource(modelID).readBytes()
        val session = ortEnv.createSession(model)

        viewModelScope.launch(Dispatchers.Main) {
            recalulatedSinceLaunch.value = true
            progress.value = 0.0
            val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val contentResolver: ContentResolver = getApplication<Application>().contentResolver
            val cursor = getAllMediaFilesOnDevice(contentResolver)
            val totalImages = cursor?.count ?: 0
            cursor?.use {
                val idColumn: Int = it.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val dateColumn: Int =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val bucketColumn: Int =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val pathColumn: Int =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)
                val nameColumn: Int =
                    it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                while (it.moveToNext()) {
                    try {
                        val id: Long = it.getLong(idColumn)
                        val date: Long = it.getLong(dateColumn)
                        var bucket = "Others"
                        if (bucketColumn != null)
                            bucket = it.getString(bucketColumn)
                        val path: String = it.getString(pathColumn)
                        val name: String = it.getString(nameColumn)
                        mcurrentFile.value = path + name
                        val record = repository.getRecord(id) as ImageEmbedding?
                        if (record != null) {
                            imageList.add(ImageData(id, record.embedding, date, bucket));
                        } else {
                            try {
                                val imageUri: Uri = Uri.withAppendedPath(uri, id.toString())
                                val inputStream = contentResolver.openInputStream(imageUri)
                                val bytes = inputStream?.readBytes()
                                inputStream?.close()

                                // Can fail to create the image decoder if its not implemented for the image type
                                val bitmap: Bitmap? =
                                    BitmapFactory.decodeByteArray(bytes, 0, bytes?.size ?: 0)
                                bitmap?.let {
                                    val rawBitmap = centerCrop(bitmap, 224)
                                    val inputShape = longArrayOf(1, 3, 224, 224)
                                    val inputName = "pixel_values"
                                    val imgData = preProcess(rawBitmap)
                                    val inputTensor = OnnxTensor.createTensor(ortEnv, imgData, inputShape)

                                    inputTensor.use {
                                        val output =
                                            session?.run(
                                                Collections.singletonMap(
                                                    inputName,
                                                    inputTensor
                                                )
                                            )
                                        output.use {
                                            @Suppress("UNCHECKED_CAST") var rawOutput =
                                                ((output?.get(0)?.value) as Array<FloatArray>)[0]
                                            rawOutput = normalizeL2(rawOutput)
                                            repository.addImageEmbedding(
                                                ImageEmbedding(
                                                    id, date, rawOutput
                                                )
                                            )
                                            imageList.add(ImageData(id, rawOutput, date, bucket))
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    catch (e: Exception) { //suppress all exceptions
                        e.printStackTrace()
                    }
                    // Record created/loaded, update progress
                    progress.value = it.position.toDouble() / totalImages.toDouble()
                }
            }
            cursor?.close()
            session.close()
            progress.setValue(1.0)

            Toast.makeText(getApplication(), "Index Generated, search ready to use", Toast.LENGTH_SHORT).show()
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface ApplicationEntryPoint {
    fun application(): Application
}

class ApplicationEntryPointImpl @Inject constructor(private val application: Application) : ApplicationEntryPoint {
    override fun application(): Application = application
}
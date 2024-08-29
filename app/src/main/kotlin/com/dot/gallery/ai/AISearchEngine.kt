package com.dot.gallery.ai

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import com.dot.gallery.feature_node.domain.model.Media
import dagger.hilt.android.lifecycle.HiltViewModel
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult
import javax.inject.Inject
import kotlin.concurrent.thread

public class AISearchEngine (private val application: Application) {
    val image_vm = ORTImageViewModel(application)
    val search_vm = SearchViewModel(application)
    val text_vm = ORTTextViewModel(application)

    public val index_progress = image_vm.progress
    public fun getImagesProcessed(): Int {
        return image_vm.imageList.size;
    }

    init {
        // new thread
        image_vm.generateIndex()
    }

    suspend fun searchByImage(query: Media, choices: List<Media>): List<BoundExtractedResult<Media>> {
        // find the image in the list
        val imageEmbedding = image_vm.getEmbedding(query.id) ?: return emptyList()

        search_vm.sortByCosineDistance(imageEmbedding, image_vm.imageList)
        val searchResults = search_vm.searchResults

        return searchResults?.map { iter ->
            BoundExtractedResult(
                choices.find { it.id == iter.id } ?:
                Media(
                    id = iter.id,
                    label = "",
                    uri = android.net.Uri.parse(""),
                    path = "",
                    relativePath = "",
                    albumID = -99L,
                    albumLabel = "",
                    timestamp = 0L,
                    fullDate = "",
                    mimeType = "",
                    favorite = 0,
                    trashed = 0),
                query.label,
                (iter.distance*100000).toInt(),
                0) } ?: emptyList()
    }

    fun search(query: String, choices: List<Media>): List<BoundExtractedResult<Media>> {
        val textEmbedding: FloatArray = text_vm.getTextEmbedding(query)

        search_vm.sortByCosineDistance(textEmbedding, image_vm.imageList)
        val searchResults = search_vm.searchResults

        return searchResults?.map { iter ->
            BoundExtractedResult(
                choices.find { it.id == iter.id } ?:
                Media(
                    id = iter.id,
                    label = "",
                    uri = android.net.Uri.parse(""),
                    path = "",
                    relativePath = "",
                    albumID = -99L,
                    albumLabel = "",
                    timestamp = 0L,
                    fullDate = "",
                    mimeType = "",
                    favorite = 0,
                    trashed = 0),
                query,
                (iter.distance*100000).toInt(),
                0) } ?: emptyList()
    }
}

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    var searchResults: List<SearchResult>? = null
    var fromImg2ImgFlag: Boolean = false

    fun sortByCosineDistance(searchEmbedding: FloatArray,
                             imageList: List<ImageData>) {
        val distances = LinkedHashMap<Long, Float>()
        for (i in imageList.indices) {
            val dist = searchEmbedding.dot(imageList[i].embedding)
            distances[imageList[i].id] = dist
        }
        searchResults = distances.toList().sortedBy { (_, value) -> value }.map { SearchResult().apply {
            id = it.first
            distance = it.second
        } }.reversed()
    }

    fun sortByDate(imageList: List<ImageData>) {
        searchResults = imageList.map { SearchResult().apply {
            id = it.id
            distance = it.date.toFloat()
        } }.reversed()
    }
}

class SearchResult {
    var id: Long = 0
    var distance: Float = 0.0f
}
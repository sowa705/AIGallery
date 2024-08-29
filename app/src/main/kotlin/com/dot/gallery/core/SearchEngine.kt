package com.dot.gallery.core

import android.app.Application
import androidx.hilt.navigation.compose.hiltViewModel
import com.dot.gallery.ai.AISearchEngine
import com.dot.gallery.feature_node.domain.model.Media
import me.xdrop.fuzzywuzzy.FuzzySearch
import me.xdrop.fuzzywuzzy.model.BoundExtractedResult
import javax.inject.Inject

class SearchEngine (val ai_search: AISearchEngine) {

    fun search(query: String, choices: List<Media>) : List<BoundExtractedResult<Media>>
    {
        //val text_matches = FuzzySearch.extractSorted(query, choices, { it.toString() }, 60)
        val ai_matches = ai_search.search(query, choices)

        return ai_matches
    }

    suspend fun searchByImage(query: Media, choices: List<Media>) : List<BoundExtractedResult<Media>>
    {
        //val text_matches = FuzzySearch.extractSorted(query, choices, { it.toString() }, 60)
        val ai_matches = ai_search.searchByImage(query, choices)

        return ai_matches
    }
}
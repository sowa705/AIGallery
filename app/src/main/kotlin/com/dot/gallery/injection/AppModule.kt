/*
 * SPDX-FileCopyrightText: 2023 IacobIacob01
 * SPDX-License-Identifier: Apache-2.0
 */

package com.dot.gallery.injection

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import androidx.room.Room
import coil.ImageLoader
import coil.request.ImageRequest
import com.dot.gallery.ai.AISearchEngine
import com.dot.gallery.ai.ApplicationEntryPoint
import com.dot.gallery.ai.ApplicationEntryPointImpl
import com.dot.gallery.core.SearchEngine
import com.dot.gallery.feature_node.data.data_source.InternalDatabase
import com.dot.gallery.feature_node.data.repository.MediaRepositoryImpl
import com.dot.gallery.feature_node.domain.repository.MediaRepository
import com.dot.gallery.feature_node.domain.use_case.MediaUseCases
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideContentResolver(@ApplicationContext context: Context): ContentResolver {
        return context.contentResolver
    }

    @Provides
    @Singleton
    fun provideDatabase(app: Application): InternalDatabase {
        return Room.databaseBuilder(app, InternalDatabase::class.java, InternalDatabase.NAME)
            .build()
    }

    @Provides
    @Singleton
    fun provideMediaUseCases(repository: MediaRepository, @ApplicationContext context: Context): MediaUseCases {
        return MediaUseCases(context, repository)
    }

    @Provides
    @Singleton
    fun provideMediaRepository(
        @ApplicationContext context: Context,
        database: InternalDatabase
    ): MediaRepository {
        return MediaRepositoryImpl(context, database)
    }

    @Provides
    @Singleton
    fun getImageLoader(@ApplicationContext context: Context): ImageLoader = ImageLoader(context)

    @Provides
    fun getImageRequest(@ApplicationContext context: Context): ImageRequest.Builder =
        ImageRequest.Builder(context)

    @Provides
    fun provideApplicationEntryPoint(application: Application): ApplicationEntryPoint {
        return ApplicationEntryPointImpl(application)
    }

    @Provides
    @Singleton
    fun provideSearchEngine(application: Application): SearchEngine {
        val ai_search = AISearchEngine(application)
        return SearchEngine(ai_search)
    }
}

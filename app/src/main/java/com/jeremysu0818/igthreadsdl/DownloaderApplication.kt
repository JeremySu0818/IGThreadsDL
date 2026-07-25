package com.jeremysu0818.igthreadsdl

import android.app.Application
import com.jeremysu0818.igthreadsdl.data.download.AndroidDownloadRepository
import com.jeremysu0818.igthreadsdl.data.resolver.ResolverRepository
import com.jeremysu0818.igthreadsdl.overlay.OverlayCoordinator

class DownloaderApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppGraph.initialize(this)
    }
}

object AppGraph {
    lateinit var application: DownloaderApplication
        private set

    lateinit var resolverRepository: ResolverRepository
        private set

    lateinit var downloadRepository: AndroidDownloadRepository
        private set

    lateinit var overlayCoordinator: OverlayCoordinator
        private set

    fun initialize(app: DownloaderApplication) {
        if (this::application.isInitialized) return
        this.application = app
        resolverRepository = ResolverRepository()
        downloadRepository = AndroidDownloadRepository(app)
        overlayCoordinator = OverlayCoordinator(
            resolverRepository = resolverRepository,
            downloadRepository = downloadRepository,
        )
    }
}

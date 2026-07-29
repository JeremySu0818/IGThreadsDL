package com.jeremysu0818.igthreadsdl.domain.model

enum class MediaPlatform(val value: String) {
    INSTAGRAM("instagram"),
    THREADS("threads"),
}

enum class ManifestType(val value: String) {
    REEL("reel"),
    STORY("story"),
    POST("post"),
    PHOTO("photo"),
    VIDEO("video"),
    CAROUSEL("carousel"),
    UNKNOWN("unknown"),
}

enum class MediaItemType(val value: String) {
    IMAGE("image"),
    VIDEO("video"),
}

data class MediaPreview(
    val imageUrl: String,
    val filename: String?,
)

data class MediaManifest(
    val platform: MediaPlatform,
    val type: ManifestType,
    val author: String?,
    val sourceUrl: String,
    val title: String?,
    val caption: String?,
    val thumbnailUrl: String?,
    val items: List<MediaItem>,
    val isPartial: Boolean = false,
    val warnings: List<String> = emptyList(),
) {
    val previews: List<MediaPreview>
        get() = items.mapNotNull { item ->
            val imageUrl = when (item.type) {
                MediaItemType.IMAGE -> item.downloadUrl
                MediaItemType.VIDEO -> item.thumbnailUrl
            }?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            MediaPreview(
                imageUrl = imageUrl,
                filename = item.filename,
            )
        }.ifEmpty {
            listOfNotNull(
                thumbnailUrl?.takeIf(String::isNotBlank)?.let { imageUrl ->
                    MediaPreview(
                        imageUrl = imageUrl,
                        filename = items.firstOrNull()?.filename,
                    )
                },
            )
        }
}

data class MediaItem(
    val id: String,
    val type: MediaItemType,
    val downloadUrl: String,
    val thumbnailUrl: String?,
    val width: Int?,
    val height: Int?,
    val durationMs: Long?,
    val filename: String,
    val contentLength: Long?,
    val mimeType: String?,
    val requestHeaders: Map<String, String> = emptyMap(),
) {
    val isHls: Boolean
        get() = downloadUrl.substringBefore('?').endsWith(".m3u8", ignoreCase = true) ||
            mimeType.equals("application/vnd.apple.mpegurl", ignoreCase = true) ||
            mimeType.equals("application/x-mpegurl", ignoreCase = true)
}

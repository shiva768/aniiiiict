package com.zelretch.aniiiiict.data.model

data class WorkImage(val recommendedImageUrl: String?, val facebookOgImageUrl: String?) {
    val imageUrl: String?
        get() = facebookOgImageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            if (!url.startsWith("http")) {
                "https://$url"
            } else {
                url
            }
        } ?: recommendedImageUrl?.let { url ->
            if (!url.startsWith("http")) {
                "https://$url"
            } else {
                url
            }
        }
}

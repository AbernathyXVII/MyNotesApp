package com.gabriele.notionlocal.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberSoundPicker(title: String, current: String?, onPicked: (uri: String, name: String?) -> Unit): () -> Unit = {}

package com.dv.moneym.data.llmmodels

/**
 * Creates the production [ModelFileStore] rooted at [rootDir] (typically
 * `dbPlatform.appFilesDirectory`). Models live under `$rootDir/models/`.
 */
expect fun createModelFileStore(rootDir: String): ModelFileStore

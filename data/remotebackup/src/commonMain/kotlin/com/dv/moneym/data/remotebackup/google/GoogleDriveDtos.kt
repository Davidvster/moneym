package com.dv.moneym.data.remotebackup.google

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class DriveFileDto(
    val id: String,
    val name: String,
    val size: String? = null,
    val modifiedTime: String? = null,
    val appProperties: Map<String, String>? = null,
)

@Serializable
internal data class DriveFileListDto(
    val files: List<DriveFileDto> = emptyList(),
    val nextPageToken: String? = null,
)

@Serializable
internal data class DriveFileMetadataRequest(
    val name: String,
    val parents: List<String> = listOf("appDataFolder"),
    val mimeType: String = "application/octet-stream",
    val appProperties: Map<String, String>? = null,
)

@Serializable
internal data class DriveErrorWrapper(
    val error: DriveError? = null,
)

@Serializable
internal data class DriveError(
    val code: Int = 0,
    val message: String = "",
    val status: String? = null,
    val errors: List<DriveErrorDetail> = emptyList(),
)

@Serializable
internal data class DriveErrorDetail(
    val domain: String? = null,
    val reason: String? = null,
    val message: String? = null,
)

@Serializable
internal data class DriveAboutDto(
    val storageQuota: DriveStorageQuotaDto? = null,
)

@Serializable
internal data class DriveStorageQuotaDto(
    val limit: String? = null,
    val usage: String? = null,
    @SerialName("usageInDrive") val usageInDrive: String? = null,
)

package com.yefeng.h5_offline_engine

data class DataDownloadModel(
    val remoteUrl: String,
    val localDir: String,
    val remoteVersion: String,
    val type: Int
)
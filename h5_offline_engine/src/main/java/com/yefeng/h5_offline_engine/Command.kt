package com.yefeng.h5_offline_engine

object Command {
    fun getExtractCmd(archivePath: String, outPath: String): String {
        return String.format("7z x '%s' '-o%s' -aoa", archivePath, outPath)
    }

    fun getCompressCmd(filePath: String, outPath: String, type: String): String {
        return String.format("7z a -t%s '%s' '%s'", type, outPath, filePath)
    }
}
package com.anatawa12.bintray2Central

sealed class DirectoryEntry(val isDirectory: Boolean) {
    data class Directory(override val name: String) : DirectoryEntry(true)
    data class File(override val name: String) : DirectoryEntry(false)
    abstract val name: String
    val isFile = !isDirectory
}

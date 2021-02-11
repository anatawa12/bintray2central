package com.anatawa12.bintray2Central

sealed class Entry {
    data class Directory(val relativePath: String, val entries: List<DirectoryEntry>) : Entry()
    data class File(val relativePath: String, val content: ByteArray) : Entry() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as File

            if (relativePath != other.relativePath) return false
            if (!content.contentEquals(other.content)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = relativePath.hashCode()
            result = 31 * result + content.contentHashCode()
            return result
        }
    }
}

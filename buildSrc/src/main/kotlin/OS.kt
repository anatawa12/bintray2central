
enum class OS {
    WINDOWS,
    LINUX,
    MAC,
    ;
    companion object {
        val current by lazy {
            val operationSystem = System.getProperty("os.name").toLowerCase()
            when {
                operationSystem.contains("win") -> WINDOWS
                operationSystem.contains("nix") || operationSystem.contains("nux") || operationSystem.contains("aix") -> LINUX
                operationSystem.contains("mac") -> MAC
                else -> error("unsupported")
            }
        }
    }
}

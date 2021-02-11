import com.github.gino0631.icns.IcnsBuilder
import com.github.gino0631.icns.IcnsType
import org.apache.commons.imaging.ImageFormats
import org.apache.commons.imaging.Imaging
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.awt.Image
import java.awt.RenderingHints
import java.awt.Transparency
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO

open class GenerateIcns : DefaultTask() {
    @InputFile
    lateinit var sourcePng: File
    @OutputFile
    lateinit var destIcns: File

    @TaskAction
    fun run() {
        val img = ImageIO.read(sourcePng)
        check(img.width == img.height) { "must be square" }
        val resolutions = img.width.getLowerResolutionsForIcons()
        IcnsBuilder.getInstance().use { builder ->
            for (resolution in resolutions) {
                val newImg = changeResolution(resolution, resolution, img)
                val baos = ByteArrayOutputStream()
                ImageIO.write(newImg, "PNG", baos)
                builder.add(icnsTypeByResolution(resolution), ByteArrayInputStream(baos.toByteArray()))
            }
            builder.build().use { icns ->
                destIcns.parentFile.mkdirs()
                destIcns.outputStream().use { icns.writeTo(it) }
            }
        }
    }

    private fun icnsTypeByResolution(resolution: Int): IcnsType = when (resolution) {
        16 -> IcnsType.ICNS_16x16_JPEG_PNG_IMAGE
        32 -> IcnsType.ICNS_32x32_JPEG_PNG_IMAGE
        64 -> IcnsType.ICNS_64x64_JPEG_PNG_IMAGE
        128 -> IcnsType.ICNS_128x128_JPEG_PNG_IMAGE
        256 -> IcnsType.ICNS_256x256_JPEG_PNG_IMAGE
        512 -> IcnsType.ICNS_512x512_JPEG_PNG_IMAGE
        1024 -> IcnsType.ICNS_1024x1024_2X_JPEG_PNG_IMAGE
        else -> error("invalid resolution: $resolution")
    }

    private fun changeResolution(width: Int, height: Int, srcImg: Image): BufferedImage {
        val resizedImg = BufferedImage(width, height, Transparency.TRANSLUCENT)
        val g2 = resizedImg.createGraphics()
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g2.drawImage(srcImg, 0, 0, width, height, null)
        g2.dispose()
        return resizedImg
    }

    private fun Int.getLowerResolutionsForIcons(): List<Int> {
        for ((i, resolution) in resolutions.asReversed().withIndex()) {
            if (resolution == this) return resolutions.subList(0, resolutions.size - i)
        }
        error("not valid resolution: $this")
    }

    private val resolutions = listOf(
        16,
        32,
        64,
        128,
        256,
        512,
        1024,
    )
}

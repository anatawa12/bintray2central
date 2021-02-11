import org.apache.commons.imaging.ImageFormats
import org.apache.commons.imaging.Imaging
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.imageio.ImageIO

open class GenerateIco : DefaultTask() {
    @InputFile
    lateinit var sourcePng: File
    @OutputFile
    lateinit var destIcns: File

    @TaskAction
    fun run() {
        val img = ImageIO.read(sourcePng)
        Imaging.writeImage(img, destIcns, ImageFormats.ICO, null)
    }
}

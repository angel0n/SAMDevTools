package info.angelon.samdevtools.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.isFile
import com.intellij.openapi.vfs.writeBytes
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZiparFonte : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            val project = e.project ?: return
            val virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
            val basePath = project.basePath ?: return
            val baseDir = LocalFileSystem.getInstance().findFileByPath(basePath) ?: return

            // Usar Task.Backgroundable para mostrar progresso
            ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Zipando fonte...", true) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = false
                    indicator.fraction = 0.0

                    var packageName: String = virtualFile.path
                    var className: String? = null

                    if (virtualFile.isFile) {
                        packageName = virtualFile.path.substringBeforeLast("/")
                        className = virtualFile.path.substringAfterLast("/").substringBeforeLast(".")
                    }

                    indicator.text = "Criando estrutura temporária..."
                    indicator.fraction = 0.1

                    // Executar WriteCommandAction
                    WriteCommandAction.runWriteCommandAction(project) {
                        val tempDir = baseDir.createChildDirectory(this, "temp")
                        val srcDir = tempDir.createChildDirectory(null, "src")
                        val mainDir = srcDir.createChildDirectory(null, "main")

                        indicator.text = "Copiando arquivos Java..."
                        indicator.fraction = 0.2

                        val packageDir = LocalFileSystem.getInstance().findFileByPath(packageName)
                        var files = packageDir?.children ?: emptyArray()

                        if (className != null) {
                            files = files.filter { file -> file.name.startsWith(className) }.toTypedArray()
                        }

                        val totalFiles = files.size
                        var processedFiles = 0

                        for (file in files) {
                            indicator.text = "Copiando: ${file.name}"
                            val dirs = file.path.replace(basePath + "/src/main/", "").split("/")
                            var index = 0
                            var newDir = mainDir
                            for (dir in dirs) {
                                if (file.isDirectory) {
                                    newDir = newDir.createChildDirectory(this, dir)
                                } else {
                                    if (index == dirs.size - 1) {
                                        var groovyFile = newDir.createChildData(this, dir)
                                        groovyFile.writeBytes(file.contentsToByteArray())
                                    } else {
                                        newDir = newDir.createChildDirectory(this, dir)
                                    }
                                }
                                index++
                            }
                            processedFiles++
                            indicator.fraction = 0.2 + (0.4 * processedFiles / totalFiles)
                        }

                        indicator.text = "Copiando recursos..."
                        indicator.fraction = 0.6

                        val resourcePath = packageName.replace("java", "resources")
                        val resourceDir = LocalFileSystem.getInstance().findFileByPath(resourcePath)

                        if (resourceDir != null) {
                            var resourceFiles = resourceDir.children ?: emptyArray()

                            if (className != null) {
                                resourceFiles = resourceFiles.filter { file -> file.name.startsWith(className) }.toTypedArray()
                            }

                            val totalResources = resourceFiles.size
                            var processedResources = 0

                            for (file in resourceFiles) {
                                indicator.text = "Copiando recurso: ${file.name}"
                                val dirs = file.path.replace(basePath + "/src/main/", "").split("/")
                                var index = 0
                                var newDir = mainDir
                                for (dir in dirs) {
                                    if (file.isDirectory) {
                                        if (newDir.findChild(dir) == null) {
                                            newDir = newDir.createChildDirectory(this, dir)
                                        } else {
                                            newDir = newDir.findChild(dir)!!
                                        }
                                    } else {
                                        if (index == dirs.size - 1) {
                                            var groovyFile = newDir.createChildData(this, dir)
                                            groovyFile.writeBytes(file.contentsToByteArray())
                                        } else {
                                            if (newDir.findChild(dir) == null) {
                                                newDir = newDir.createChildDirectory(this, dir)
                                            } else {
                                                newDir = newDir.findChild(dir)!!
                                            }
                                        }
                                    }
                                    index++
                                }
                                processedResources++
                                indicator.fraction = 0.6 + (0.2 * processedResources / totalResources)
                            }
                        }

                        indicator.text = "Criando arquivo ZIP..."
                        indicator.fraction = 0.8

                        val tempFile = File(tempDir.path)
                        val timeStamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
                        val zipName = "$className-$timeStamp.zip"
                        val zipFile = File(project.basePath, zipName)
                        zipFolder(tempFile, zipFile, indicator)

                        indicator.text = "Limpando arquivos temporários..."
                        indicator.fraction = 0.95

                        tempDir.delete(this)

                        indicator.fraction = 1.0
                        indicator.text = "Concluído!"
                    }
                }

                override fun onSuccess() {
                    Messages.showInfoMessage(project, "Fonte zipado com sucesso!", "Sucesso")
                }

                override fun onThrowable(error: Throwable) {
                    error.printStackTrace()
                    Messages.showErrorDialog("Não foi possível zipar o fonte: ${error.message}", "Erro")
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
            Messages.showErrorDialog("Não foi possível zipar o fonte.", "Erro")
        }
    }

    fun zipFolder(folder: File, zipFile: File, indicator: ProgressIndicator? = null) {
        ZipOutputStream(FileOutputStream(zipFile)).use { zip ->
            zipFolderRecursive(folder, folder, zip, indicator)
        }
    }

    fun zipFolderRecursive(root: File, file: File, zip: ZipOutputStream, indicator: ProgressIndicator? = null) {
        if (file.isDirectory) {
            file.listFiles()?.forEach {
                zipFolderRecursive(root, it, zip, indicator)
            }
        } else {
            indicator?.text2 = "Compactando: ${file.name}"
            val entryName = root.toPath().relativize(file.toPath()).toString().replace("\\", "/")
            zip.putNextEntry(ZipEntry(entryName))
            zip.write(file.readBytes())
            zip.closeEntry()
        }
    }
}
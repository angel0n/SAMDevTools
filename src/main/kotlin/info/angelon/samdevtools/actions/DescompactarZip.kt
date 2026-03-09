package info.angelon.samdevtools.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

class DescompactarZip : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val basePath = project.basePath ?: return

        // Criar descritor para selecionar apenas arquivos ZIP
        val descriptor = FileChooserDescriptor(
            true,  // chooseFiles
            false, // chooseFolders
            false, // chooseJars
            false, // chooseJarsAsFiles
            false, // chooseJarContents
            false  // chooseMultiple
        ).withFileFilter { file -> file.extension?.lowercase() == "zip" }
            .withTitle("Selecionar Arquivo ZIP")
            .withDescription("Selecione um arquivo ZIP para descompactar na pasta src do projeto")

        // Abrir diálogo de seleção
        FileChooser.chooseFile(descriptor, project, null) { selectedFile ->
            if (selectedFile != null) {
                descompactarZip(selectedFile, basePath, project)
            }
        }
    }

    private fun descompactarZip(zipFile: VirtualFile, basePath: String, project: com.intellij.openapi.project.Project) {
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "Descompactando arquivo...", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = false
                indicator.fraction = 0.0
                indicator.text = "Preparando descompactação..."

                try {
                    val file = File(zipFile.path)
                    val srcPath = basePath
                    val srcDir = File(srcPath)

                    // Verificar se a pasta src existe
                    if (!srcDir.exists()) {
                        throw IllegalStateException("Pasta 'src' não encontrada no projeto")
                    }

                    indicator.text = "Lendo arquivo ZIP..."
                    indicator.fraction = 0.1

                    // Contar total de entradas para calcular progresso
                    val totalEntries = countZipEntries(file)
                    var processedEntries = 0

                    indicator.text = "Extraindo arquivos..."
                    indicator.fraction = 0.2

                    // Descompactar
                    ZipInputStream(FileInputStream(file)).use { zip ->
                        var entry = zip.nextEntry

                        while (entry != null) {
                            indicator.text = "Extraindo: ${entry.name}"
                            indicator.text2 = "${processedEntries + 1} de $totalEntries"

                            val outputFile = File(srcPath, entry.name)

                            if (entry.isDirectory) {
                                outputFile.mkdirs()
                            } else {
                                // Criar diretórios pai se necessário
                                outputFile.parentFile?.mkdirs()

                                // Extrair arquivo
                                FileOutputStream(outputFile).use { output ->
                                    zip.copyTo(output)
                                }
                            }

                            zip.closeEntry()
                            processedEntries++
                            indicator.fraction = 0.2 + (0.7 * processedEntries / totalEntries)

                            entry = zip.nextEntry
                        }
                    }

                    indicator.text = "Atualizando sistema de arquivos..."
                    indicator.fraction = 0.9

                    // Atualizar VFS do IntelliJ
                    WriteCommandAction.runWriteCommandAction(project) {
                        val srcVirtualFile = LocalFileSystem.getInstance().findFileByPath(srcPath)
                        srcVirtualFile?.refresh(false, true)
                    }

                    indicator.fraction = 1.0
                    indicator.text = "Concluído!"

                } catch (e: Exception) {
                    throw e
                }
            }

            override fun onSuccess() {
                Messages.showInfoMessage(
                    project,
                    "Arquivo descompactado com sucesso na pasta src!",
                    "Sucesso"
                )
            }

            override fun onThrowable(error: Throwable) {
                error.printStackTrace()
                Messages.showErrorDialog(
                    "Não foi possível descompactar o arquivo: ${error.message}",
                    "Erro"
                )
            }
        })
    }

    private fun countZipEntries(file: File): Int {
        var count = 0
        ZipInputStream(FileInputStream(file)).use { zip ->
            while (zip.nextEntry != null) {
                count++
                zip.closeEntry()
            }
        }
        return count
    }
}
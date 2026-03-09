package info.angelon.samdevtools.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

class NovoRelatorio : AnAction() {

    override fun actionPerformed(e: AnActionEvent) {
        try {
            val project = e.project ?: return
            val baseJavaDir = findSrcMain(project) ?: return
            val baseResourceDir = findSrcResources(project) ?: return

            val packageClassName = Messages.showInputDialog(
                project,
                "Package + Classe:",
                "Novo Relatório",
                Messages.getQuestionIcon()
            ) ?: return

            val packageName = packageClassName.substringBeforeLast(".")
            val className = packageClassName.substringAfterLast(".")

            WriteCommandAction.runWriteCommandAction(project){
                val packagePath = packageName.replace(".", "/")
                val packageDir = createDirectories(baseJavaDir, packagePath)
                val resourceDir = createDirectories(baseResourceDir, packagePath)

                val javaFileName = "$className.groovy"
                val htmlFileName = "$className.html"

                if (packageDir.findChild(javaFileName) != null) {
                    Messages.showErrorDialog("Arquivo groovy já existe", "Erro")
                    return@runWriteCommandAction
                }

                if (resourceDir.findChild(htmlFileName) != null) {
                    Messages.showErrorDialog("Arquivo html já existe", "Erro")
                    return@runWriteCommandAction
                }

                val javaFile = packageDir.createChildData(this, javaFileName)
                val htmlFile = resourceDir.createChildData(this, htmlFileName)

                val javaTemplate = getJavaTemplate(packageName, className)
                val htmlTemplate = getHtmlTemplate()

                javaFile.setBinaryContent(javaTemplate.toByteArray())
                htmlFile.setBinaryContent(htmlTemplate.toByteArray())
            }
        } catch (e: Exception) {
            Messages.showErrorDialog("Não foi possível criar o Relatorio", "Erro")
        }
    }

    private fun createDirectories(base: VirtualFile, path: String): VirtualFile {
        var current = base
        for (folder in path.split("/")) {
            current = current.findChild(folder)
                ?: current.createChildDirectory(this, folder)
        }
        return current
    }

    private fun findSrcMain(project: Project): VirtualFile? {
        val basePath = project.basePath ?: return null
        val srcMainPath = "$basePath/src/main/java"
        return LocalFileSystem.getInstance().findFileByPath(srcMainPath)
    }

    private fun findSrcResources(project: Project): VirtualFile? {
        val basePath = project.basePath ?: return null
        val srcMainPath = "$basePath/src/main/resources"
        return LocalFileSystem.getInstance().findFileByPath(srcMainPath)
    }

    private fun getJavaTemplate(packageName: String, className: String): String {
        return """
                package $packageName;
                
                import sam.server.samdev.relatorio.RelatorioBase
                import sam.server.samdev.relatorio.DadosParaDownload
                import br.com.multitec.utils.Utils
                
                class $className extends RelatorioBase {

                    @Override
                    String getNomeTarefa() {
                        return "$className"
                    }
                    
                    @Override
                    Map<String, Object> criarValoresIniciais() {
                        Map<String, Object> filtrosDefault = new HashMap<String, Object>();
                        return Utils.map("filtros", filtrosDefault);
                    }

                    @Override
                    DadosParaDownload executar() {
                         return gerarPDF()
                    }
                }
            """.trimIndent()
    }

    private fun getHtmlTemplate(): String {
        return """
                <html>
	                <template>
                        <div class="row q-gutter-y-md">
                        </div>
                    </template>
                    <script>
                        const classe = {
                            data : {},
                            onLoad : function() {},
                            methods : {}
                        }
                    </script>
                    <style>
                    </style>
                </html>
            """.trimIndent()
    }
}

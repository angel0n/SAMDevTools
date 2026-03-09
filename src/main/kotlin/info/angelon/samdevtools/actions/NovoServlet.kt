package info.angelon.samdevtools.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile

class NovoServlet : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        try {
            val project = e.project ?: return
            val baseJavaDir = findSrcMain(project) ?: return

            val packageClassName = Messages.showInputDialog(
                project,
                "Package + Classe:",
                "Nova Formula",
                Messages.getQuestionIcon()
            ) ?: return

            val packageName = packageClassName.substringBeforeLast(".")
            val className = packageClassName.substringAfterLast(".")

            WriteCommandAction.runWriteCommandAction(project){
                val packagePath = packageName.replace(".", "/")
                val packageDir = createDirectories(baseJavaDir, packagePath)

                val javaFileName = "$className.groovy"

                if (packageDir.findChild(javaFileName) != null) {
                    Messages.showErrorDialog("Arquivo groovy já existe", "Erro")
                    return@runWriteCommandAction
                }

                val javaFile = packageDir.createChildData(this, javaFileName)

                val javaTemplate = getJavaTemplate(packageName, className)

                javaFile.setBinaryContent(javaTemplate.toByteArray())
            }
        }catch (ex: Exception){
            Messages.showErrorDialog("Não foi possível criar o Servlet", "Erro")
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

    private fun getJavaTemplate(packageName: String, className: String): String {
        return """
                package $packageName;
                
                import sam.server.samdev.relatorio.ServletBase
                import org.springframework.http.MediaType
                import org.springframework.http.ResponseEntity
                import br.com.multitec.utils.jackson.JSonMapperCreator
                import com.fasterxml.jackson.core.type.TypeReference
                import br.com.multitec.utils.collections.TableMap

                class $className extends  ServletBase{

                    @Override
                    String getNome() throws Exception {
                        return "$className"
                    }

                    @Override
                    ResponseEntity<Object> executar() throws Exception {
                        String req = httpServletRequest.getReader().lines().collect(Collectors.joining());
                        TableMap body = JSonMapperCreator.create().read(req, new TypeReference<TableMap>() {});
                        
                        return ResponseEntity.ok()
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(null);
                    }
                }
            """.trimIndent()
    }
}
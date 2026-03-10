package info.angelon.samdevtools.actions.sampalm

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.util.Locale
import java.util.Locale.getDefault

class NovaFeature : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val baseDir = findSrcMain(project) ?: return

        val featureName = Messages.showInputDialog(
            project,
            "Feature name: ",
            "Nova Feature",
            Messages.getQuestionIcon()
        ) ?: return

        var featureNameLower = featureName.lowercase(getDefault())
        var featureNameCamel = featureName.substring(0, 1).uppercase(getDefault()) + featureName.substring(1)

        WriteCommandAction.runWriteCommandAction(project){
            var featureDir = baseDir.createChildDirectory(this, featureNameLower)
            var dataDir = featureDir.createChildDirectory(this, "data")
            var domainDir = featureDir.createChildDirectory(this, "domain")
            var viewsDir = featureDir.createChildDirectory(this, "views")

            var dtosDir = domainDir.createChildDirectory(this, "dtos")
            var useCaseDir = domainDir.createChildDirectory(this, "usecase")

            var useCaseFile = useCaseDir.createChildData(this, "${featureNameLower}_usecase.dart")
            var dataSourceFile = dataDir.createChildData(this, "${featureNameLower}_data_source.dart")
            var stateFile = domainDir.createChildData(this, "${featureNameLower}_state.dart")

            var mainFile = viewsDir.createChildData(this, "${featureNameLower}.dart")
            var providerFile = viewsDir.createChildData(this, "${featureNameLower}_provider.dart")
            var screenFile = viewsDir.createChildData(this, "${featureNameLower}_screen.dart")

            dataSourceFile.setBinaryContent(getTemplateDataSource(featureNameCamel).toByteArray())
            stateFile.setBinaryContent(getTemplateState(featureNameCamel).toByteArray())
            mainFile.setBinaryContent(getTemplateMain(featureNameCamel,featureNameLower).toByteArray())
            providerFile.setBinaryContent(getTemplateProvider(featureNameCamel,featureNameLower).toByteArray())
            screenFile.setBinaryContent(getTemplateScreen(featureNameCamel,featureNameLower).toByteArray())
            useCaseFile.setBinaryContent(getTemplateUseCase(featureNameCamel,featureNameLower).toByteArray())
        }
    }

    private fun findSrcMain(project: Project): VirtualFile? {
        val basePath = project.basePath ?: return null
        val srcMainPath = "$basePath/lib/features"
        return LocalFileSystem.getInstance().findFileByPath(srcMainPath)
    }

    private fun getTemplateDataSource(nameCamelCase: String): String {
        return """
            import 'package:sam_palm/core/services/api_service.dart';
            
            class ${nameCamelCase}DataSource{}
        """.trimIndent()
    }

    private fun getTemplateUseCase(nameCamelCase: String, nameLower: String): String {
        return """
            import 'package:dartz/dartz.dart';
            import 'package:sam_palm/core/exceptions/failures.dart';
            import 'package:sam_palm/features/${nameLower}/data/${nameLower}_data_source.dart';
            
            class ${nameCamelCase}UseCase {
                late ${nameCamelCase}DataSource ${nameLower}dataSource;

                ${nameCamelCase}UseCase() {
                    ${nameLower}dataSource = ${nameCamelCase}DataSource();
                }
                
                Future<Either<Failure, void>> call() async {
                    return Left(Failure(""));
                }
            }
        """.trimIndent()
    }

    private fun getTemplateState(nameCamelCase: String): String {
        return """
            import 'package:flutter/widgets.dart';
            
            class ${nameCamelCase}State{}
        """.trimIndent()
    }

    private fun getTemplateProvider(nameCamelCase: String, nameLower: String): String {
        return """
            import 'package:flutter/material.dart';
            import 'package:sam_palm/core/componentes/dialogs/sam_alert_dialog.dart';
            import 'package:sam_palm/core/globalstates/app_state.dart';
            import 'package:sam_palm/features/${nameLower}/domain/${nameLower}_state.dart';
            
            class ${nameCamelCase}Provider extends ChangeNotifier{
                ${nameCamelCase}Provider();
                
                bool _isLoading = false;
                String? _erroMessage;
                
                final ${nameCamelCase}State _state = ${nameCamelCase}State();
                ${nameCamelCase}State get state => _state;
                
                bool get isLoading => _isLoading;
                String? get erroMessage => _erroMessage;
                
                Future<void> executar() async {
                    try{
                    
                    }catch (error) {
                      samAlertDialog(context: AppState.instance.globalContext, title: "Ops...", descricao: error.toString().replaceAll("Exception: ", ""));
                    } finally {
                      _isLoading = false;
                      notifyListeners();
                    }
                }
            }
        """.trimIndent()
    }

    private fun getTemplateScreen(nameCamelCase: String, nameLower: String): String {
        return """
            import 'package:flutter/material.dart';
            import 'package:provider/provider.dart';
            import 'package:sam_palm/core/componentes/header/header_default.dart';
            import 'package:sam_palm/features/${nameLower}/views/${nameLower}_provider.dart';
               
            class ${nameCamelCase}Screen extends StatefulWidget {
              const ${nameCamelCase}Screen({super.key});

              @override
              State<${nameCamelCase}Screen> createState() => _${nameCamelCase}ScreenState();
            }
            
            class _${nameCamelCase}ScreenState extends State<${nameCamelCase}Screen> {
                @override
                Widget build(BuildContext context) {
                    final provider = Provider.of<${nameCamelCase}Provider>(context);
                    
                    return Scaffold(
                        body: SingleChildScrollView(
                            child: Column(
                                mainAxisAlignment: MainAxisAlignment.start,
                                crossAxisAlignment: CrossAxisAlignment.start,
                                children: [
                                    HeaderDefault(
                                        title: ModalRoute.of(context)!.settings.arguments as String,
                                        retornaAnterior: true,
                                    ),
                                ]
                            ),
                        ),
                    );
                }
            }
        """.trimIndent()
    }

    private fun getTemplateMain(nameCamelCase: String, nameLower: String): String {
        return """
            import 'package:flutter/material.dart';
            import 'package:provider/provider.dart';
            import 'package:sam_palm/features/${nameLower}/views/${nameLower}_provider.dart';
            import 'package:sam_palm/features/${nameLower}/views/${nameLower}_screen.dart';

            class ${nameCamelCase} extends StatelessWidget {
              const ${nameCamelCase}({super.key});

              @override
              Widget build(BuildContext context) {
                return MultiProvider(
                  providers: [ChangeNotifierProvider(create: (context) => ${nameCamelCase}Provider())],
                  child: const ${nameCamelCase}Screen(),
                );
              }
            }
        """.trimIndent()
    }
}
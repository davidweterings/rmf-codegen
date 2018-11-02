package io.vrap.codegen.kt.languages

import io.vrap.codegen.kt.languages.java.JavaBaseTypes
import io.vrap.codegen.kt.languages.java.SpringRestTemplateModule
import io.vrap.codegen.kt.languages.java.groovy.dsl.GroovyDslModule
import io.vrap.codegen.kt.languages.java.model.JavaModelModule
import io.vrap.codegen.kt.languages.java.plantuml.PlantUmlModule
import io.vrap.codegen.kt.languages.java.spring.resttemplate.SpringWebClientModule
import io.vrap.rmf.codegen.kt.CodeGeneratorConfig
import io.vrap.rmf.codegen.kt.di.GeneratorComponent
import io.vrap.rmf.codegen.kt.di.GeneratorModule
import org.eclipse.emf.common.util.URI
import org.junit.Test


class TestCodeGenerator {


    val generatorConfig = CodeGeneratorConfig(
            ramlFileLocation = URI.createFileURI("../api-spec/import-storage-api.raml")
    )

    @Test
    fun generateJavaModels(){

        val generatorModule = GeneratorModule(generatorConfig, JavaBaseTypes)
        val generatorComponent = GeneratorComponent(generatorModule, JavaModelModule())
        generatorComponent.generateFiles()
    }

    @Test
    fun generateGroovyDsl(){
        val generatorModule = GeneratorModule(generatorConfig, JavaBaseTypes)
        val generatorComponent = GeneratorComponent(generatorModule, GroovyDslModule())
        generatorComponent.generateFiles()
    }

    @Test
    fun generateSpringRestTemplate(){
        val generatorModule = GeneratorModule(generatorConfig, JavaBaseTypes)
        val generatorComponent = GeneratorComponent(generatorModule, SpringRestTemplateModule())
        generatorComponent.generateFiles()
    }

    @Test
    fun generateSpringWebClient(){
        val generatorModule = GeneratorModule(generatorConfig, JavaBaseTypes)
        val generatorComponent = GeneratorComponent(generatorModule, SpringWebClientModule())
        generatorComponent.generateFiles()
    }

    @Test
    fun generatePlantUmlDiagram(){
        val generatorModule = GeneratorModule(generatorConfig, JavaBaseTypes)
        val generatorComponent = GeneratorComponent(generatorModule, PlantUmlModule())
        generatorComponent.generateFiles()
    }
}
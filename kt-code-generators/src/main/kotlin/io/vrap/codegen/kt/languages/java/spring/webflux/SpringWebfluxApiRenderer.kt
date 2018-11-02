package io.vrap.codegen.kt.languages.java.spring.resttemplate

import com.google.inject.Inject
import io.vrap.codegen.kt.languages.java.JavaSubTemplates
import io.vrap.codegen.kt.languages.java.extensions.EObjectTypeExtensions
import io.vrap.codegen.kt.languages.java.extensions.fullClassName
import io.vrap.codegen.kt.languages.java.extensions.simpleName
import io.vrap.codegen.kt.languages.java.extensions.toJavaComment
import io.vrap.rmf.codegen.common.generator.core.ResourceCollection
import io.vrap.rmf.codegen.kt.io.TemplateFile
import io.vrap.rmf.codegen.kt.rendring.ResourceCollectionRenderer
import io.vrap.rmf.codegen.kt.rendring.utils.escapeAll
import io.vrap.rmf.codegen.kt.rendring.utils.keepIndentation
import io.vrap.rmf.codegen.kt.types.PackageProvider
import io.vrap.rmf.codegen.kt.types.VrapObjectType
import io.vrap.rmf.codegen.kt.types.VrapTypeProvider
import io.vrap.rmf.raml.model.resources.Method
import io.vrap.rmf.raml.model.resources.Resource
import io.vrap.rmf.raml.model.responses.Response
import io.vrap.rmf.raml.model.types.AnyType
import io.vrap.rmf.raml.model.types.impl.TypesFactoryImpl

class SpringWebfluxApiRenderer @Inject constructor(val packageProvider: PackageProvider, override val vrapTypeProvider: VrapTypeProvider) : ResourceCollectionRenderer, EObjectTypeExtensions {


    override fun render(type: ResourceCollection): TemplateFile {

        val vrapType = vrapTypeProvider.doSwitch(type.sample) as VrapObjectType

        val content = """
            |package ${vrapType.`package`};
            |
            |import org.springframework.web.bind.annotation.PathVariable;
            |import org.springframework.web.bind.annotation.RequestBody;
            |import org.springframework.web.bind.annotation.RequestMapping;
            |import org.springframework.web.bind.annotation.RequestMethod;
            |import reactor.core.publisher.Mono;
            |
            |${type.sample.toJavaComment().escapeAll()}
            |${JavaSubTemplates.generatedAnnotation.escapeAll()}
            |public interface ${vrapType.simpleClassName.replace("Requests", "Api")} {
            |
            |    <${type.methods()}>
            |
            |}
        """.trimMargin().keepIndentation()
        return TemplateFile(
                relativePath = "${vrapType.`package`}.${vrapType.simpleClassName}".replace(".", "/") + ".java",
                content = content
        )
    }


    fun ResourceCollection.methods(): String {
        return this.resources
                .flatMap { resource -> resource.methods.map { javaBody(resource, it) } }
                .joinToString(separator = "\n\n")
    }

    fun javaBody(resource: Resource, method: Method): String {
        val methodReturnType = vrapTypeProvider.doSwitch(method.retyurnType())
        val body = """
            |${method.toJavaComment().escapeAll()}
            |@RequestMapping(value = "${resource.fullUri.template}", method = RequestMethod.${method.method.name})
            |public Mono\<${methodReturnType.fullClassName().escapeAll()}\> ${method.method.name.toLowerCase()}(${methodParameters(resource, method)});
            """.trimMargin()
        return body
    }

    fun methodParameters(resource: Resource, method: Method): String {

        val paramList = resource.allUriParameters
                .map { "@PathVariable(\"${it.name}\") ${it.type.toVrapType().simpleName()} ${it.name}" }
                .toMutableList()

        if (method.bodies?.isNotEmpty() ?: false) {
            paramList.add("@RequestBody ${method.bodies[0].type.toVrapType().fullClassName().escapeAll()} body")
        }
        return paramList.joinToString(separator = ", ")
    }

    fun Method.retyurnType(): AnyType {
        return this.responses
                .filter { it.isSuccessfull() }
                .filter { it.bodies?.isNotEmpty() ?: false }
                .map { it.bodies[0].type }
                .firstOrNull()
                ?: TypesFactoryImpl.eINSTANCE.createNilType()
    }


    fun Response.isSuccessfull(): Boolean = this.statusCode.toInt() in (200..299)

}
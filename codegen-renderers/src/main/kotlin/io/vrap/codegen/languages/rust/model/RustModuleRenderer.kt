package io.vrap.codegen.languages.rust.model

import com.google.inject.Inject
import io.vrap.codegen.languages.java.extensions.simpleName
import io.vrap.codegen.languages.java.extensions.toComment
import io.vrap.codegen.languages.php.extensions.forcedLiteralEscape
import io.vrap.rmf.codegen.io.TemplateFile
import io.vrap.rmf.codegen.rendring.FileProducer
import io.vrap.rmf.codegen.rendring.utils.escapeAll
import io.vrap.rmf.codegen.rendring.utils.keepIndentation
import io.vrap.rmf.codegen.types.VrapEnumType
import io.vrap.rmf.codegen.types.VrapObjectType
import io.vrap.rmf.codegen.types.VrapTypeProvider
import io.vrap.rmf.raml.model.types.*
import io.vrap.rmf.raml.model.util.StringCaseFormat

class RustModuleRenderer @Inject constructor(override val vrapTypeProvider: VrapTypeProvider) : RustObjectTypeExtensions, RustEObjectTypeExtensions, FileProducer {

    @Inject
    lateinit var allAnyTypes: MutableList<AnyType>


    override fun produceFiles(): List<TemplateFile> {
        return allAnyTypes.groupBy { s(it) }
                .map { entry: Map.Entry<String, List<AnyType>> -> buildModule(entry.key, entry.value) }
                .toList()
    }

    private fun s(it: AnyType): String {
        val t = it.toVrapType()
        return when (t) {
            is VrapObjectType -> t.`package`
            is VrapEnumType -> t.`package`
            else -> ""
        }
    }


    fun buildModule(modName: String, types: List<AnyType>): TemplateFile {
        var moduleName = modName
        if (moduleName.endsWith("Type") && !moduleName.endsWith(("ProductType"))) {
            moduleName = moduleName.replace("Type", "cttype")
        }

        val content = """
           |//Generated file, please do not change
           |
           |${getImportsForModule(moduleName, types)}
           |use chrono::{DateTime, NaiveDate, Utc};
           |use serde_json;
           |use serde::{Deserialize, Serialize};
           |use std::collections::HashMap;
           |
           |${types.map { it.renderAnyType() }.joinToString(separator = "\n")}
       """.trimMargin().keepIndentation()

        return TemplateFile(content, moduleName.toLowerCase().replace(".", "/") + ".rs")

    }

    fun AnyType.renderAnyType(): String {
        return when (this) {
            is ObjectType -> this.renderObjectType()
            is StringType -> this.renderStringType()
            is UnionType -> ""
            else -> throw IllegalArgumentException("unhandled case ${this.javaClass}")

        }
    }

    fun ObjectType.renderObjectType(): String {
        val vrapType = this.toVrapType() as VrapObjectType

        if (this.patternSpec() != "") {
            return """
            |${this.toComment().escapeAll()}
            |pub type ${vrapType.simpleClassName} = ${this.patternSpec()};
            """.trimMargin()
        } else {
            return """
            |${this.toComment().escapeAll()}
            |#[derive(Serialize, Deserialize, Debug)]
            |#[serde(rename_all = "camelCase")]
            |pub struct ${vrapType.simpleClassName} {
            |   <${this.constructorBlock()}>
            |}
            """.trimMargin()
        }
    }

    fun ObjectType.constructorBlock():String{
        return if(this.allProperties.filter { !it.isPatternProperty() }.isNotEmpty())
            """
            |${this.objectFields().escapeAll()}
            """.trimMargin()
        else ""
    }

    fun ObjectType.superConstructor(): String {
        return if (this.type != null)
            "\nsuper(${(
                    this.type as ObjectType)
                    .properties
                    .filter { !it.isPatternProperty() }
                    .sortedWith(PropertiesComparator)
                    .map {
                        if (it.name != this.discriminator())
                            it.name
                        else if ((it.type as StringType).enum.isNotEmpty())
                            "${it.type.toVrapType().simpleRustName()}.${this.discriminatorValueOrDefault().enumValueName()}"
                        else
                            "'${this.discriminatorValueOrDefault()}'"
                    }
                    .joinToString(separator = ",")
            })\n"
        else ""
    }

    fun ObjectType.objectFields(): String {
        return this.allProperties
                .filter { !it.isPatternProperty() }
                .filter {
                    it.name != this.discriminator() || this.properties.map { it.name }.contains(it.name)

                }
                .sortedWith(PropertiesComparator)
                .map {
                    val comment: String = it.type.toComment()
                    val commentNotEmpty: Boolean = comment.isNotBlank()
                    var fieldName: String = StringCaseFormat.LOWER_UNDERSCORE_CASE.apply(it.name)
                    if (fieldName == "type") {
                        fieldName = "r#type"
                    }
                    """
                    |${if (commentNotEmpty) {
                        comment + "\n"
                    } else ""}pub $fieldName: ${if (!it.required) "Option<" else ""}${it.type.toVrapType().simpleRustName()}${if (!it.required) ">" else ""},""".trimMargin()

                }
                .joinToString(separator = "\n")
    }

    fun StringType.renderStringType(): String {
        val vrapType = this.toVrapType() as VrapEnumType

        return """
        |${this.toComment().escapeAll()}
        |#[derive(Serialize, Deserialize, Debug)]
        |pub enum ${vrapType.simpleClassName} {
        |   <${this.enumFields()}>
        |}
        |
        |${this.enumImpl(vrapType.simpleClassName)}
        """.trimMargin()
    }

    fun StringType.enumFields(): String = this.enumJsonNames()
            .map { StringCaseFormat.UPPER_CAMEL_CASE.apply(it.enumValueName()) }
            .joinToString(separator = ",\n")

    fun StringType.enumImpl(enumName: String): String {

        val fromStringEnums = this.enumJsonNames()
            .map { "\"$it\" =\\> Some($enumName::${StringCaseFormat.UPPER_CAMEL_CASE.apply(it.enumValueName())})," }
            .joinToString(separator = "\n")

        val toStringEnums = this.enumJsonNames()
            .map { "$enumName::${StringCaseFormat.UPPER_CAMEL_CASE.apply(it.enumValueName())} =\\> \"$it\"," }
            .joinToString(separator = "\n")

        return """
        |impl $enumName {
        |    pub fn from_str(s: &str) -\> Option\<$enumName\> {
        |        match s {
        |            <$fromStringEnums>
        |            _ =\> None,
        |        }
        |    }

        |    pub fn as_str(&self) -\> &'static str {
        |        match *self {
        |           <$toStringEnums>
        |        }
        |    }
        |}
        """.trimIndent()

    }


    fun StringType.enumJsonNames() = this.enum?.filter { it is StringInstance }
            ?.map { it as StringInstance }
            ?.map { it.value }
            ?.filterNotNull() ?: listOf()

    fun String.enumValueName(): String {
        return StringCaseFormat.UPPER_UNDERSCORE_CASE.apply(this)
    }

    fun Property.isPatternProperty() = this.name.startsWith("/") && this.name.endsWith("/")

    /**
     * If the handled is a map type then the map args should be specified
     */
    fun ObjectType.patternSpec(): String? {
        return this.allProperties
                .filter { it.isPatternProperty() }
                .firstOrNull()
                .let {
                    if (it != null) "HashMap\\<String, ${it.type.toVrapType().simpleRustName().escapeAll()}\\>" else ""
                }
    }

    /**
     * in typescript optional properties should come after required ones
     */
    object PropertiesComparator : Comparator<Property> {
        override fun compare(o1: Property, o2: Property): Int {
            return if (o1.required && !o2.required) {
                -1
            } else if (!o1.required && o2.required) {
                +1
            } else if((o1.type?.name?:"").compareTo(o2.type?.name?:"") != 0){
                (o1.type?.name?:"").compareTo(o2.type?.name?:"")
            }
            else {
                o1.name.compareTo(o2.name)
            }
        }
    }
}

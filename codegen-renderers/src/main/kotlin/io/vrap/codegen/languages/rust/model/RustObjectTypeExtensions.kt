package io.vrap.codegen.languages.rust.model

import io.vrap.rmf.codegen.types.*
import io.vrap.rmf.raml.model.types.AnyType
import io.vrap.rmf.raml.model.types.ObjectType
import java.lang.IllegalStateException
import java.nio.file.Path
import java.nio.file.Paths

interface RustObjectTypeExtensions : io.vrap.codegen.languages.ExtensionsBase {

    fun getImportsForModule(moduleName: String, types: List<AnyType>): String {

        return types
                .filter { it is ObjectType }
                .map { it as ObjectType }
                .flatMap { it.getDependencies() }
                .distinct()
                .filter {
                    when (it) {
                        is VrapObjectType -> it.`package` != moduleName && it.`package` != "io.sphere.api.models"
                        is VrapEnumType -> it.`package` != moduleName && it.`package` != "io.sphere.api.models"
                        else -> false
                    }
                }
                .map {
                    when (it) {
                        is VrapObjectType -> "use super::${it.`package`.toLowerCase().replace("io.sphere.api.models.", "").replace("type", "cttype").replace("productcttype", "producttype")}::${it.simpleClassName};"
                        is VrapEnumType -> "use super::${it.`package`.toLowerCase().replace("io.sphere.api.models.", "").replace("type", "cttype").replace("productcttype", "producttype")}::${it.simpleClassName};"
                        else -> throw IllegalStateException("Unhandled case $it")
                    }
                }
                .joinToString(separator = "\n")
    }


    fun relativisePaths(currentModule:String, targetModule : String) : String {
        val currentRelative :Path = Paths.get(currentModule.replace(".","/"))
        val targetRelative : Path = Paths.get(targetModule.replace(".","/"))
        return currentRelative.relativize(targetRelative).toString().replaceFirst("../","./")
    }

    private fun ObjectType.getDependencies(): List<VrapType> {


        val result = this.properties
                .map { it.type }
                //If the subtypes are in the same package they should be imported
                .plus(this.subTypes)
                .plus(this.type)
                .filterNotNull()
                .map { vrapTypeProvider.doSwitch(it) }
                .map { toVrapType(it) }
                .filterNotNull()
                .filter { it !is VrapScalarType }

        return result
    }
}


private fun toVrapType(vrapType: VrapType): VrapType? {
    return when (vrapType) {
        is VrapObjectType -> vrapType
        is VrapEnumType -> vrapType
        is VrapArrayType -> {
            toVrapType(vrapType.itemType)
        }
        else -> null

    }
}

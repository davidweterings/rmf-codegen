package io.vrap.codegen.languages.rust.model

import org.eclipse.emf.ecore.EObject

interface RustEObjectTypeExtensions : io.vrap.codegen.languages.ExtensionsBase {
    fun EObject.toVrapType() = vrapTypeProvider.doSwitch(this)

}

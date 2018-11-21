
package io.vrap.codegen.kt.languages.php.model

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import io.vrap.rmf.codegen.kt.rendring.FileProducer
import io.vrap.rmf.codegen.kt.rendring.ObjectTypeRenderer
import io.vrap.rmf.codegen.kt.rendring.MethodRenderer

class PhpModelModule: AbstractModule() {

    override fun configure() {
        val objectTypeBinder = Multibinder.newSetBinder(binder(), ObjectTypeRenderer::class.java)
        objectTypeBinder.addBinding().to(PhpObjectTypeRenderer::class.java)
        objectTypeBinder.addBinding().to(PhpCollectionRenderer::class.java)

        val fileBinder = Multibinder.newSetBinder(binder(), FileProducer::class.java)
        fileBinder.addBinding().to(PhpFileProducer::class.java)

        val resourceBinder = Multibinder.newSetBinder(binder(), MethodRenderer::class.java)
        resourceBinder.addBinding().to(PhpMethodRenderer::class.java)
    }
}
package io.vrap.codegen.kt.languages.java.spring.resttemplate

import com.google.inject.AbstractModule
import com.google.inject.multibindings.Multibinder
import io.vrap.rmf.codegen.kt.rendring.ResourceCollectionRenderer

class SpingRestTemplateModule : AbstractModule() {
    override fun configure() {
        val resourceCollectionBinder = Multibinder.newSetBinder(binder(), ResourceCollectionRenderer::class.java)
        resourceCollectionBinder.addBinding().to(SpringRestTemplateRenderer::class.java)
    }
}
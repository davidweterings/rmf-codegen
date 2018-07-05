package io.vrap.rmf.codegen.common.generator.builder;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.squareup.javapoet.*;
import io.reactivex.Maybe;
import io.reactivex.Single;
import io.vrap.rmf.codegen.common.generator.core.CodeGenerator;
import io.vrap.rmf.codegen.common.generator.core.GenerationResult;
import io.vrap.rmf.codegen.common.generator.core.GeneratorConfig;
import io.vrap.rmf.raml.model.modules.Api;
import io.vrap.rmf.raml.model.types.ObjectType;

import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * This code generator generates java interface to provide a Groovy DSL for creating instances.
 * An example with a Person type:
 * <code>
 *     Person max = person {
 *         name = "Max"
 *         age = 23
 *     }
 * </code>
 */
public class GroovyBuilderDslCodeGenerator extends CodeGenerator {
    private final Converter<String, String> methodNameMapper =
            CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);

    public GroovyBuilderDslCodeGenerator(final GeneratorConfig generatorConfig, final Api api) {
        super(generatorConfig, api);
    }

    @Override
    public Single<GenerationResult> generateStub() {
        final Single<GenerationResult> generationResult = getTypes()
                .filter(ObjectType.class::isInstance)
                .cast(ObjectType.class)
                .flatMapMaybe(this::transformToJavaFile)
                .doOnNext(javaFile -> javaFile.writeTo(getOutputFolder()))
                .map(JavaFile::toJavaFileObject)
                .map(javaFile -> getPath(javaFile, getOutputFolder()))
                .toList()
                .map(GenerationResult::of);

        return generationResult;

    }

    private Path getPath(JavaFileObject javaFile, Path outputFolder) {
        return Paths.get(outputFolder.toString(), javaFile.getName());
    }

    public Maybe<JavaFile> transformToJavaFile(final ObjectType objectType) {
        final String builderName = objectType.getName() + "Dsl";
        final TypeSpec.Builder interfaceBuilder = TypeSpec.interfaceBuilder(builderName)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Provides a Groovy DSL to build instances of this type.\n");
        final String name = methodNameMapper.convert(objectType.getName());
        final ClassName typeName = (ClassName) getTypeNameSwitch().doSwitch(objectType);

        final TypeName closureType = ParameterizedTypeName.get(ClassName.get("groovy.lang", "Closure"), typeName);
        final AnnotationSpec delegatesToAnnotation = AnnotationSpec.builder(ClassName.get("groovy.lang", "DelegatesTo"))
                .addMember("value", "$T.class", typeName)
                .build();
        final ParameterSpec closureParameter = ParameterSpec.builder(closureType, "closure", Modifier.FINAL)
                .addAnnotation(delegatesToAnnotation)
                .build();
        final MethodSpec builderMethod = MethodSpec.methodBuilder(name)
                .addJavadoc("Create a new instance of this type.\n\n")
                .addJavadoc("@param closure the closure to initialize the fields of the new instance\n")
                .addJavadoc("@return new instance intialized via the given closure\n")
                .returns(typeName)
                .addModifiers(Modifier.PUBLIC, Modifier.DEFAULT)
                .addParameter(closureParameter)
                .addCode("final $T $L = new $T();\n", typeName, name, typeName)
                .addCode("closure.setDelegate($L);\n", name)
                .addCode("closure.call();\n")
                .addCode("return $L;\n", name)
                .build();
        interfaceBuilder.addMethod(builderMethod);
        final TypeSpec typeSpec = interfaceBuilder.build();
        return Maybe.just(JavaFile.builder(typeName.packageName(), typeSpec).build());
    }
}

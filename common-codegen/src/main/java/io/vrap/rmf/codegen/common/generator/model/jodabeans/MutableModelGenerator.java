package io.vrap.rmf.codegen.common.generator.model.jodabeans;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.squareup.javapoet.*;
import io.reactivex.Single;
import io.vrap.rmf.codegen.common.generator.core.CodeGenerator;
import io.vrap.rmf.codegen.common.generator.core.GenerationResult;
import io.vrap.rmf.codegen.common.generator.core.GeneratorConfig;
import io.vrap.rmf.raml.model.modules.Api;
import io.vrap.rmf.raml.model.types.*;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.joda.beans.Bean;
import org.joda.beans.gen.BeanCodeGen;
import org.joda.beans.gen.BeanDefinition;
import org.joda.beans.gen.PropertyDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.lang.model.element.Modifier;
import javax.tools.JavaFileObject;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.vrap.rmf.codegen.common.generator.util.CodeGeneratorUtil.*;

public class MutableModelGenerator extends CodeGenerator {

    protected final Logger LOGGER = LoggerFactory.getLogger(getClass());


    public MutableModelGenerator(GeneratorConfig generatorConfig, Api api) {
        super(generatorConfig, api);
    }


    public Path getPaths(JavaFileObject javaFile, Path outputFolder) {
        return Paths.get(outputFolder.toString(), javaFile.getName());
    }


    protected Stream<AnnotationSpec> getDiscriminatorAnnotations(final ObjectType objectType) {
        if (StringUtils.isEmpty(objectType.getDiscriminator()) || objectType.getSubTypes().isEmpty()) {
            return Stream.empty();
        }
        final AnnotationSpec jsonTypeInfoAnnotation = AnnotationSpec.builder(JsonTypeInfo.class)
                .addMember("use", "$T.NAME", JsonTypeInfo.Id.class)
                .addMember("include", "$T.PROPERTY", JsonTypeInfo.As.class)
                .addMember("property", "$S", objectType.getDiscriminator())
                .addMember("visible", "true")
                .build();

        CodeBlock.Builder annotationBodyBuilder = CodeBlock.builder();
        List<ObjectType> children = getSubtypes(objectType).collect(Collectors.toList());
        if (!children.isEmpty()) {
            for (int i = 0; i < children.size(); i++) {
                annotationBodyBuilder.add(
                        (i == 0 ? "{" : ",") +
                                "\n@$T(value = $T.class, name = $S)"
                                + (i == children.size() - 1 ? "\n}" : "")
                        , JsonSubTypes.Type.class, getClassName(getPackagePrefix(), children.get(i)), children.get(i).getDiscriminatorValue());
            }
        }
        final AnnotationSpec jsonSubTypesAnnotation = AnnotationSpec.builder(JsonSubTypes.class)
                .addMember("value", annotationBodyBuilder.build())
                .build();

        return Stream.of(jsonSubTypesAnnotation, jsonTypeInfoAnnotation);
    }

    protected Stream<AnnotationSpec> getAdditionalTypeAnnotations(final ObjectType objectType) {
        final AnnotationSpec annotationSpec = AnnotationSpec.builder(BeanDefinition.class)
                .addMember("style", "\"lighter\"")
                .build();
        return Stream.of(annotationSpec);
    }


    public Single<GenerationResult> generateStub() {

        final Single<GenerationResult> generationResult = getTypes().map(this::transformToJavaFile)
                .doOnNext(javaFile -> javaFile.writeTo(getOutputFolder()))
                .map(JavaFile::toJavaFileObject)
                .map(javaFile -> getPaths(javaFile, getOutputFolder()))
                .toList()
                .map(GenerationResult::of)
                .doOnSuccess(generationResult1 -> BeanCodeGen.main(new String[]{"-R", "-style=minimal", getOutputFolder().toAbsolutePath().toString()}));
        return generationResult;
    }


    public JavaFile transformToJavaFile(final AnyType anyType) {
        final TypeSpec resultTypeSpec = buildTypeSpec(anyType);
        if (resultTypeSpec == null) {
            return null;
        }
        final JavaFile javaFile = JavaFile.builder(getObjectPackage(getPackagePrefix(), anyType), resultTypeSpec).build();
        return javaFile;
    }


    private TypeSpec buildTypeSpec(final AnyType anyType) {
        if (getCustomTypeMapping().get(anyType.getName()) != null) {
            return null;
        } else if (anyType instanceof ObjectType) {
            return buildTypeSpecForObjectType(((ObjectType) anyType));
        } else if (anyType instanceof StringType) {
            return buildTypeSpecForStringType((StringType) anyType);
        } else throw new RuntimeException("unhandled type " + anyType);
    }

    private TypeSpec buildTypeSpecForStringType(final StringType stringType) {
        if (!stringType.getEnum().isEmpty()) {
            TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(getClassName(getPackagePrefix(), stringType)).addModifiers(Modifier.PUBLIC);
            stringType.getEnum()
                    .stream()
                    .map(StringInstance.class::cast)
                    .forEach(value -> enumBuilder.addEnumConstant(value.getValue()));
            enumBuilder.addJavadoc(getJavaDocProcessor().markDownToJavaDoc(stringType));
            return enumBuilder.build();
        } else {
            final AnnotationSpec annotationSpec = AnnotationSpec.builder(BeanDefinition.class)
                    .addMember("style", "\"minimal\"")
                    .build();

            TypeSpec typeSpec = TypeSpec.classBuilder(getClassName(getPackagePrefix(), stringType))
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addSuperinterface(Bean.class)
                    .addJavadoc(getJavaDocProcessor().markDownToJavaDoc(stringType))
                    .addAnnotation(annotationSpec)
                    .build();
            return typeSpec;
        }
    }

    private TypeSpec buildTypeSpecForObjectType(final ObjectType objectType) {


        // object.getPropertyType() return the supper type
        final ClassName interfaceName = getClassName(getPackagePrefix(), objectType);
        final TypeSpec.Builder typeSpecBuilder = TypeSpec.classBuilder(interfaceName)
                .addJavadoc(getJavaDocProcessor().markDownToJavaDoc(objectType))
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

        Stream.concat(getAdditionalTypeAnnotations(objectType), getDiscriminatorAnnotations(objectType)).forEach(typeSpecBuilder::addAnnotation);


        //Add super interfaces
        Optional.ofNullable(objectType.getType())
                .map(anyType -> getClassName(getPackagePrefix(), anyType))
                .map(typeSpecBuilder::superclass)
                .orElse(null);
        typeSpecBuilder.addSuperinterface(Bean.class);
        //Add Properties
        objectType.getProperties().stream()
                .map(property -> mapField(objectType, property))
                .forEach(typeSpecBuilder::addField);
        return typeSpecBuilder.build();
    }


    private FieldSpec mapField(final ObjectType objectType, Property property) {

        if (property.getName().startsWith("/")) {
            return mapFieldWithPattern(objectType, property);
        }
        return mapField(property);
    }

    private FieldSpec mapFieldWithPattern(final ObjectType objectType, Property property) {
        final ClassName keyType = ClassName.get(String.class);
        final TypeName valueType = getTypeNameSwitch().doSwitch(property.getType());

        String reaKeyType;
        try {
            reaKeyType = (String) ((PropertyValue) ((EObjectContainmentEList) objectType.getAnnotation("asMap").getValue().getValue()).get(0)).getValue().getValue();
        } catch (Exception e) {
            LOGGER.error("Error while parsing key for asMap annotation in object " + objectType, e);
            reaKeyType = "string";
        }

        final FieldSpec valuesGetter = FieldSpec.builder(ParameterizedTypeName.get(ClassName.get(Map.class), keyType, valueType), "values")
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(JsonAnySetter.class)
                .build();

        return valuesGetter;
    }

    private FieldSpec mapField(final Property property) {

        final FieldSpec.Builder fieldSpecBuilder = FieldSpec.builder(getTypeNameSwitch().doSwitch(property.getType()), property.getName());
        if (!property.getRequired()) {
            fieldSpecBuilder.addAnnotation(Nullable.class);
        }

        final AnnotationSpec jsonAnnotationSpec = AnnotationSpec.builder(JsonProperty.class)
                .addMember("value", "$S", property.getName())
                .build();

        final AnnotationSpec propertyAnnotationSpec = AnnotationSpec.builder(PropertyDefinition.class)
                .build();

        fieldSpecBuilder
                .addModifiers(Modifier.PRIVATE)
                .addAnnotation(jsonAnnotationSpec)
                .addAnnotation(propertyAnnotationSpec);

        return fieldSpecBuilder.build();
    }


}

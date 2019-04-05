package io.vrap.codegen.languages.rust.model

import io.vrap.rmf.codegen.types.LanguageBaseTypes
import io.vrap.rmf.codegen.types.VrapScalarType

object RustBaseTypes : LanguageBaseTypes(

    objectType = nativeRustType("serde_json::Value"),
    integerType = nativeRustType("u32"),
    longType = nativeRustType("u64"),
    doubleType = nativeRustType("f64"),
    stringType = nativeRustType("String"),
    booleanType = nativeRustType("bool"),
    dateTimeType = nativeRustType("DateTime<Utc>"),
    dateOnlyType = nativeRustType("NaiveDate"),
    timeOnlyType = nativeRustType("String")
)

fun nativeRustType(typeName: String): VrapScalarType = VrapScalarType(typeName)

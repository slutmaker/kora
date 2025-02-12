package ru.tinkoff.kora.http.client.symbol.processor

import com.google.devtools.ksp.getDeclaredFunctions
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.*
import ru.tinkoff.kora.http.client.common.declarative.DeclarativeHttpClientConfig
import ru.tinkoff.kora.http.client.common.declarative.HttpClientOperationConfig
import java.time.Duration

class ConfigClassGenerator {
    fun generate(declaration: KSClassDeclaration): TypeSpec {
        val functions = declaration.getDeclaredFunctions().map { it.simpleName.asString() }

        val typeName = declaration.configName()

        val tb = TypeSpec.classBuilder(typeName)
            .addModifiers(KModifier.DATA)
            .addProperty(PropertySpec.builder("url", String::class).initializer("url").build())
            .addReturnFun("url", String::class.asTypeName())
            .addProperty(PropertySpec.builder("requestTimeout", Int::class.asTypeName().copy(true)).initializer("requestTimeout").build())
            .addReturnFun("requestTimeout", Int::class.asTypeName().copy(true))

        functions.forEach { function ->
            tb.addProperty(PropertySpec.builder("${function}Config", HttpClientOperationConfig::class.asTypeName().copy(true)).initializer("${function}Config").build())
        }
        val constructor = FunSpec.constructorBuilder()
            .addParameter("url", String::class)
            .addParameter("requestTimeout", Int::class.asTypeName().copy(true))
        functions.forEach { function ->
            constructor.addParameter("${function}Config", HttpClientOperationConfig::class.asTypeName().copy(true))
        }

        val secondConstructor = FunSpec.constructorBuilder()
            .addParameter("url", String::class)
            .addParameter("requestTimeout", Duration::class.asTypeName().copy(true))
        functions.forEach { function ->
            secondConstructor.addParameter("${function}Config", HttpClientOperationConfig::class.asTypeName().copy(true))
        }
        val args = functions.map { function -> "${function}Config" }.joinToString(", ", "url, requestTimeout?.toMillisPart(), ")
        secondConstructor.callThisConstructor(args)

        tb.primaryConstructor(constructor.build())
        tb.addFunction(secondConstructor.build())
        tb.addSuperinterface(DeclarativeHttpClientConfig::class)
        return tb.build()
    }

    private fun TypeSpec.Builder.addReturnFun( name: String, typeName: TypeName): TypeSpec.Builder {
        val funSpec = FunSpec.builder(name)
            .addCode("return %L", name)
            .returns(typeName)
            .addModifiers(KModifier.OVERRIDE)
        this.addFunction(funSpec.build())
        return this
    }
}

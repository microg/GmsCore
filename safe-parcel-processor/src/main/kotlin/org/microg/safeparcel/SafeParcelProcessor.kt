/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.safeparcel

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.Messager
import javax.annotation.processing.RoundEnvironment
import javax.annotation.processing.SupportedAnnotationTypes
import javax.annotation.processing.SupportedSourceVersion
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.ElementKind
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.tools.Diagnostic

const val SafeParcelable = "com.google.android.gms.common.internal.safeparcel.SafeParcelable"
const val SafeParcelReader = "com.google.android.gms.common.internal.safeparcel.SafeParcelReader"
const val SafeParcelWriter = "com.google.android.gms.common.internal.safeparcel.SafeParcelWriter"
const val SafeParcelableCreatorAndWriter = "com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter"

const val Field = "java.lang.reflect.Field"

const val Log = "android.util.Log"
const val Parcel = "android.os.Parcel"

val NATIVE_SUPPORTED_TYPES = setOf(
    "int", "byte", "short", "boolean", "long", "float", "double",
    "java.lang.Boolean", "java.lang.Byte", "java.lang.Char", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double",
    "java.lang.String", "android.os.Bundle"
)

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("$SafeParcelable.Class")
class SafeParcelProcessor : AbstractProcessor() {
    override fun process(set: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        val safeParcelableClassTypeElement = set.firstOrNull() ?: return false
        classes@ for (classElement in roundEnvironment.getElementsAnnotatedWith(safeParcelableClassTypeElement)) {
            val clazz = ClassInfo(classElement)
            if (clazz.check(processingEnv.messager)) {
                processingEnv.filer.createSourceFile(clazz.fullCreatorName, clazz.classElement).openWriter().use { it.write(clazz.generateCreator()) }
            }
        }
        return false
    }
}

class ClassInfo(val classElement: Element) {
    val name = classElement.simpleName.toString()
    val fullName = classElement.toString()
    val packageName = fullName.split(".").dropLast(1).joinToString(".")

    val creatorName = "$name\$000Creator"
    val fullCreatorName = "$packageName.$creatorName"

    val fields = classElement.enclosedElements
        .filter { it.kind == ElementKind.FIELD && !it.modifiers.contains(Modifier.STATIC) }
        .filterIsInstance<VariableElement>()
        .filter { it.annotationMirrors.any { it.annotationType.toString() == "$SafeParcelable.Field" } }
        .map { FieldInfo(this, it) }
    val constructor = classElement.enclosedElements
        .filter { it.kind == ElementKind.CONSTRUCTOR }
        .filterIsInstance<ExecutableElement>()
        .filter { it.annotationMirrors.any { it.annotationType.toString() == "$SafeParcelable.Constructor" } || it.parameters.isEmpty() }
        .let { if (it.size == 2) it.first { it.parameters.isNotEmpty() } else it.firstOrNull() }
        ?.let { ConstructorInfo(this, it) }

    fun check(messager: Messager): Boolean {
        fun note(message: String) = messager.printMessage(Diagnostic.Kind.NOTE, message)
        fun error(message: String) = messager.printMessage(Diagnostic.Kind.ERROR, message)
        if (constructor == null) {
            error("No suitable constructor found for $fullName")
            return false
        }
        if (constructor.parameters.any { it.annotationMirrors.none { it.annotationType.toString() == "$SafeParcelable.Param" } }) {
            error("Tagged constructor for $fullName has parameters without @Param.")
            return false
        }
        if (constructor.fieldIds.any { id -> fields.none { it.id == id } }) {
            error("Constructor for $fullName has parameters with @Param value without matching @Field.")
            return false
        }
        if (constructor.isPrivate) {
            note("Using reflection to construct $fullName from parcel. Consider providing a suitable package-visible constructor for improved performance.")
        }
        for (field in fields) {
            if (field.type !in NATIVE_SUPPORTED_TYPES) {
                error("Field ${field.name} in $fullName has unsupported type.")
                return false
            }
            if (field.isPrivate) {
                note("Using reflection when accessing ${field.name} in $fullName. Consider adding it to the @Constructor and making the field package-visible for improved performance.")
            }
        }
        return true
    }

    fun generateCreator(): String {
        if (constructor == null) throw IllegalStateException("Can't create Creator for class without constructor")
        fun List<String>.linesToString(prefix: String = "") = joinToString("\n                            $prefix")
        val variableDeclarations = fields.map { it.variableDeclaration }.linesToString()
        val setVariablesDefault = fields.map { it.setVariableDefault }.linesToString()
        val readVariablesFromParcel = fields.map { it.readVariableFromParcelCase }.linesToString("        ")
        val writeVariableToParcel = fields.map { it.writeVariableToParcel }.linesToString()
        val setFieldsFromVariables = fields.filter { it.id !in constructor.fieldIds }.flatMap { it.setFieldFromVariable }.linesToString()
        val invokeConstructor = constructor.invocation.linesToString()
        val setVariablesFromFields = fields.flatMap { it.setVariableFromField }.linesToString()
        val file = """
                package $packageName;

                //@javax.annotation.processing.Generated // Not supported by Android
                @androidx.annotation.Keep
                @org.microg.gms.common.Hide
                public class $creatorName implements $SafeParcelableCreatorAndWriter<$fullName> {
                    @Override
                    public $fullName createFromParcel($Parcel parcel) {
                        int end = $SafeParcelReader.readObjectHeader(parcel);
                        $fullName object;
                        try {
                            $variableDeclarations
                            $setVariablesDefault
                            while (parcel.dataPosition() < end) {
                                int header = $SafeParcelReader.readHeader(parcel);
                                int fieldId = $SafeParcelReader.getFieldId(header);
                                switch (fieldId) {
                                    $readVariablesFromParcel
                                    default:
                                        $Log.d("SafeParcel", String.format("Unknown field id %d in %s, skipping.", fieldId, "$fullName"));
                                        $SafeParcelReader.skip(parcel, header);
                                }
                            }
                            $invokeConstructor
                            $setFieldsFromVariables
                        } catch (Exception e) {
                            throw new RuntimeException(String.format("Error reading %s", "$fullName"), e);
                        }
                        if (parcel.dataPosition() > end) {
                            throw new RuntimeException(String.format("Overread allowed size end=%d", end));
                        }
                        return object;
                    }

                    @Override
                    public void writeToParcel($fullName object, $Parcel parcel, int flags) {
                        int start = $SafeParcelWriter.writeObjectHeader(parcel);
                        try {
                            $variableDeclarations
                            $setVariablesFromFields
                            $writeVariableToParcel
                        } catch (Exception e) {
                            throw new RuntimeException(String.format("Error writing %s", "$fullName"), e);
                        }
                        $SafeParcelWriter.finishObjectHeader(parcel, start);
                    }

                    @Override
                    public $fullName[] newArray(int size) {
                        return new $fullName[size];
                    }
                }
            """.trimIndent()
        return file
    }
}

class ConstructorInfo(val clazz: ClassInfo, val constructorElement: ExecutableElement) {
    val isPrivate by lazy { constructorElement.modifiers.contains(Modifier.PRIVATE) }
    val parameters by lazy { constructorElement.parameters }
    val fieldIds by lazy {
        parameters.map {
            it.annotationMirrors
                .first { it.annotationType.toString() == "$SafeParcelable.Param" }
                .elementValues
                .filter { it.key.simpleName.toString() == "value" }
                .firstNotNullOfOrNull { it.value }
                .toString()
        }
    }
    val argTypes by lazy { fieldIds.map { id -> clazz.fields.first { it.id == id }.type } }
    val args by lazy { fieldIds.map { id -> clazz.fields.first { it.id == id }.variableName } }
    val invocation by lazy {
        if (isPrivate) {
            listOf(
                "Constructor<${clazz.fullName}> constructor = ${clazz.fullName}.class.getConstructor(${argTypes.map { "$it.class" }.joinToString(", ")});",
                "constructor.setAccessible(true);",
                "object = constructor.newInstance(${args.joinToString(", ")});"
            )
        } else {
            listOf("object = new ${clazz.fullName}(${args.joinToString(", ")});")
        }
    }
}

class FieldInfo(val clazz: ClassInfo, val fieldElement: VariableElement) {
    val name by lazy { fieldElement.simpleName.toString() }
    val type by lazy { fieldElement.asType().toString() }
    val isPrivate by lazy { fieldElement.modifiers.contains(Modifier.PRIVATE) }

    val id by lazy {
        fieldElement.annotationMirrors
            .first { it.annotationType.toString() == "$SafeParcelable.Field" }
            .elementValues
            .filter { it.key.simpleName.toString() == "value" }
            .firstNotNullOfOrNull { it.value.value }
            .toString()
    }
    val mayNull by lazy {
        fieldElement.annotationMirrors
            .first { it.annotationType.toString() == "$SafeParcelable.Field" }
            .elementValues
            .filter { it.key.simpleName.toString() == "mayNull" }
            .firstNotNullOfOrNull { it.value.value }
            ?.toString()
            ?.toBoolean() == true
    }

    val variableName by lazy { "_$name\$000" }
    val variableDeclaration by lazy { "$type $variableName;" }
    val defaultValue by lazy {
        when (type) {
            "boolean" -> "false"
            "byte", "char", "short", "int", "long", "float", "double" -> "0"
            else -> "null"
        }
    }
    val setVariableDefault by lazy { "$variableName = $defaultValue;" }

    val readVariableFromParcel by lazy {
        when (type) {
            "int" -> "$variableName = $SafeParcelReader.readInt(parcel, header)"
            "byte" -> "$variableName = $SafeParcelReader.readByte(parcel, header)"
            "short" -> "$variableName = $SafeParcelReader.readShort(parcel, header)"
            "boolean" -> "$variableName = $SafeParcelReader.readBool(parcel, header)"
            "long" -> "$variableName = $SafeParcelReader.readLong(parcel, header)"
            "float" -> "$variableName = $SafeParcelReader.readFloat(parcel, header)"
            "double" -> "$variableName = $SafeParcelReader.readDouble(parcel, header)"
            "java.lang.String" -> "$variableName = $SafeParcelReader.readString(parcel, header)"
            "android.os.Bundle" -> "$variableName = $SafeParcelReader.readBundle(parcel, header, ${clazz.fullName}.class.getClassLoader())"
            else -> "$SafeParcelReader.skip(parcel, header)"
        }
    }
    val readVariableFromParcelCase by lazy { "case $id: $readVariableFromParcel; break;" }
    val writeVariableToParcel by lazy {
        when (type) {
            "boolean", "byte", "char", "short", "int", "long", "float", "double",
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Char", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double" ->
                "$SafeParcelWriter.write(parcel, $id, $variableName);"

            else -> "$SafeParcelWriter.write(parcel, $id, $variableName, $mayNull);"
        }
    }

    val reflectionFieldName by lazy { "_${name}\$field" }
    val reflectionFieldGetter by lazy {
        when (type) {
            "boolean" -> "$reflectionFieldName.getBoolean"
            "byte" -> "$reflectionFieldName.getByte"
            "char" -> "$reflectionFieldName.getChar"
            "short" -> "$reflectionFieldName.getShort"
            "int" -> "$reflectionFieldName.getInt"
            "long" -> "$reflectionFieldName.getLong"
            "float" -> "$reflectionFieldName.getFloat"
            "double" -> "$reflectionFieldName.getDouble"
            else -> "(${type}) $reflectionFieldName.get"
        }
    }
    val reflectionFieldSetter by lazy {
        when (type) {
            "boolean" -> "$reflectionFieldName.setBoolean"
            "byte" -> "$reflectionFieldName.setByte"
            "char" -> "$reflectionFieldName.setChar"
            "short" -> "$reflectionFieldName.setShort"
            "int" -> "$reflectionFieldName.setInt"
            "long" -> "$reflectionFieldName.setLong"
            "float" -> "$reflectionFieldName.setFloat"
            "double" -> "$reflectionFieldName.setDouble"
            else -> "$reflectionFieldName.set"
        }
    }

    val setVariableFromField by lazy {
        if (isPrivate) {
            listOf(
                "$Field $reflectionFieldName = ${clazz.fullName}.class.getDeclaredField(\"$name\");",
                "$reflectionFieldName.setAccessible(true);",
                "$variableName = $reflectionFieldGetter(object);"
            )
        } else {
            listOf("$variableName = object.$name;")
        }
    }
    val setFieldFromVariable by lazy {
        if (isPrivate) {
            listOf(
                "$Field $reflectionFieldName = ${clazz.fullName}.class.getDeclaredField(\"$name\");",
                "$reflectionFieldName.setAccessible(true);",
                "$reflectionFieldSetter(object, $variableName);"
            )
        } else {
            listOf("object.$name = $variableName;")
        }
    }
}
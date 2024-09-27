/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.safeparcel

import javax.annotation.processing.AbstractProcessor
import javax.annotation.processing.ProcessingEnvironment
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
const val Constructor = "java.lang.reflect.Constructor"

const val Log = "android.util.Log"
const val Parcel = "android.os.Parcel"
const val Parcelable = "android.os.Parcelable"
const val IInterface = "android.os.IInterface"

val NATIVE_SUPPORTED_TYPES = setOf(
    "int", "byte", "short", "boolean", "long", "float", "double",
    "java.lang.Boolean", "java.lang.Byte", "java.lang.Char", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double",
    "java.lang.String", "android.os.Bundle", "android.os.IBinder",
    "int[]", "byte[]", "byte[][]", "float[]", "java.lang.String[]",
    "java.util.List<java.lang.String>", "java.util.ArrayList<java.lang.String>",

//    "java.util.List<java.lang.Integer>", "java.util.List<java.lang.Boolean>",
//    "java.util.ArrayList<java.lang.Integer>", "java.util.ArrayList<java.lang.Boolean>",
//    "java.util.List<java.lang.Long>", "java.util.List<java.lang.Float>", "java.util.List<java.lang.Float>",
//    "java.util.ArrayList<java.lang.Long>", "java.util.ArrayList<java.lang.Float>", "java.util.ArrayList<java.lang.Float>",
)

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("$SafeParcelable.Class")
class SafeParcelProcessor : AbstractProcessor() {
    override fun process(set: Set<TypeElement>, roundEnvironment: RoundEnvironment): Boolean {
        val safeParcelableClassTypeElement = set.firstOrNull() ?: return false

        classes@ for (classElement in roundEnvironment.getElementsAnnotatedWith(safeParcelableClassTypeElement)) {
            val clazz = ClassInfo(classElement)
            if (clazz.check(processingEnv)) {
                processingEnv.filer.createSourceFile(clazz.fullCreatorName, clazz.classElement).openWriter().use { it.write(clazz.generateCreator()) }
            }
        }
        return false
    }
}

class ClassInfo(val classElement: Element) {
    val name = classElement.simpleName.toString()
    val fullName = classElement.toString()
    val packageName = let {
        var upmostClassElement = classElement
        while (upmostClassElement.enclosingElement != null) upmostClassElement = upmostClassElement.enclosingElement
        upmostClassElement.toString()
    }

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

    fun check(processingEnv: ProcessingEnvironment): Boolean {
        fun note(message: String) = processingEnv.messager.printMessage(Diagnostic.Kind.NOTE, message)
        fun error(message: String) = processingEnv.messager.printMessage(Diagnostic.Kind.ERROR, message)
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
                val typeName = field.listItemType ?: field.type
                val type = runCatching { processingEnv.typeUtils.getDeclaredType(processingEnv.elementUtils.getTypeElement(typeName)) }.getOrNull()
                val parcelable = processingEnv.typeUtils.getDeclaredType(processingEnv.elementUtils.getTypeElement(Parcelable))
                val iinterface = processingEnv.typeUtils.getDeclaredType(processingEnv.elementUtils.getTypeElement(IInterface))
                if (type != null && processingEnv.typeUtils.isAssignable(type, parcelable)) {
                    field.isParcelable = true
                } else if (type != null && processingEnv.typeUtils.isAssignable(type, iinterface)) {
                    field.isIInterface = true
                } else {
                    error("Field ${field.name} in $fullName has unsupported type ${if (typeName != field.type) "$typeName if ${field.type}" else field.type}.")
                    return false
                }
            }
            val readReflect = field.isPrivate && field.getter == null
            val writeReflect = field.isPrivate && !constructor.fieldIds.contains(field.id)
            when {
                readReflect && writeReflect -> note("Using reflection when accessing ${field.name} in $fullName. Consider adding it to the @Constructor and a getter to the annotation for improved performance.")
                writeReflect -> note("Using reflection when writing ${field.name} in $fullName. Consider adding it to the @Constructor for improved performance.")
                readReflect -> note("Using reflection when reading ${field.name} in $fullName. Consider adding a getter to the annotation for improved performance.")
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
                "$Constructor<${clazz.fullName}> constructor = ${clazz.fullName}.class.getConstructor(${argTypes.map { "$it.class" }.joinToString(", ")});",
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
    val type by lazy {
        fieldElement.annotationMirrors
            .first { it.annotationType.toString() == "$SafeParcelable.Field" }
            .elementValues
            .filter { it.key.simpleName.toString() == "type" }
            .firstNotNullOfOrNull { it.value.value }
            ?.toString()?.takeIf { it.isNotEmpty() }
            ?: fieldElement.asType().toString()
    }
    var isParcelable: Boolean = false
    var isIInterface: Boolean = false
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
    val useValueParcel by lazy {
        fieldElement.annotationMirrors
            .first { it.annotationType.toString() == "$SafeParcelable.Field" }
            .elementValues
            .filter { it.key.simpleName.toString() == "useValueParcel" }
            .firstNotNullOfOrNull { it.value.value }
            ?.toString()
            ?.toBoolean() == true
    }
    val getter by lazy {
        fieldElement.annotationMirrors
            .first { it.annotationType.toString() == "$SafeParcelable.Field" }
            .elementValues
            .filter { it.key.simpleName.toString() == "getter" }
            .firstNotNullOfOrNull { it.value.value }
            ?.toString()?.takeIf { it.isNotEmpty() }
            ?.replace("\$object", "object")
            ?.replace("\$type", type)
            ?: fieldElement.annotationMirrors
                .first { it.annotationType.toString() == "$SafeParcelable.Field" }
                .elementValues
                .filter { it.key.simpleName.toString() == "getterName" }
                .firstNotNullOfOrNull { it.value.value }
                ?.toString()?.takeIf { it.isNotEmpty() }
                ?.let { "object.$it()" }
    }
    val subType by lazy {
        fieldElement.annotationMirrors
            .first { it.annotationType.toString() == "$SafeParcelable.Field" }
            .elementValues
            .filter { it.key.simpleName.toString() == "subClass" }
            .firstNotNullOfOrNull { it.value.value }
            ?.toString()?.takeIf { it.isNotEmpty() }
    }
    val isList by lazy { type.startsWith("java.util.List<") || type.startsWith("java.util.ArrayList<") }
    val isArray by lazy { type.endsWith("[]") }
    val listItemType by lazy {
        subType ?: when {
            type.startsWith("java.util.List<") -> type.substring(15, type.length - 1)
            type.startsWith("java.util.ArrayList<") -> type.substring(20, type.length - 1)
            type.endsWith("[]") -> type.substring(0, type.length - 2)
            else -> null
        }
    }

    val variableName by lazy { "_$name\$000" }
    val variableDeclaration by lazy { "$type $variableName;" }
    val defaultValue by lazy {
        fieldElement.annotationMirrors
            .first { it.annotationType.toString() == "$SafeParcelable.Field" }
            .elementValues
            .filter { it.key.simpleName.toString() == "defaultValue" }
            .firstNotNullOfOrNull { it.value.value }
            ?.toString()?.takeIf { it.isNotEmpty() }
            ?: when (type) {
                "boolean" -> "false"
                "byte", "char", "short", "int", "long", "float", "double" -> "0"
                else -> "null"
            }
    }
    val setVariableDefault by lazy { "$variableName = $defaultValue;" }

    val readVariableFromParcel by lazy {
        when (type) {
            "int", "java.lang.Integer" -> "$variableName = $SafeParcelReader.readInt(parcel, header)"
            "byte", "java.lang.Byte" -> "$variableName = $SafeParcelReader.readByte(parcel, header)"
            "short", "java.lang.Short" -> "$variableName = $SafeParcelReader.readShort(parcel, header)"
            "boolean", "java.lang.Boolean" -> "$variableName = $SafeParcelReader.readBool(parcel, header)"
            "long", "java.lang.Long" -> "$variableName = $SafeParcelReader.readLong(parcel, header)"
            "float", "java.lang.Float" -> "$variableName = $SafeParcelReader.readFloat(parcel, header)"
            "double", "java.lang.Double" -> "$variableName = $SafeParcelReader.readDouble(parcel, header)"
            "java.lang.String" -> "$variableName = $SafeParcelReader.readString(parcel, header)"
            "android.os.Bundle" -> "$variableName = $SafeParcelReader.readBundle(parcel, header, ${clazz.fullName}.class.getClassLoader())"
            "android.os.IBinder" -> "$variableName = $SafeParcelReader.readBinder(parcel, header)"
            "java.lang.String[]" -> "$variableName = $SafeParcelReader.readStringArray(parcel, header)"
            "byte[]" -> "$variableName = $SafeParcelReader.readByteArray(parcel, header)"
            "byte[][]" -> "$variableName = $SafeParcelReader.readByteArrayArray(parcel, header)"
            "float[]" -> "$variableName = $SafeParcelReader.readFloatArray(parcel, header)"
            "int[]" -> "$variableName = $SafeParcelReader.readIntArray(parcel, header)"
            "java.util.List<java.lang.String>", "java.util.ArrayList<java.lang.String>" -> when {
                !useValueParcel -> "$variableName = $SafeParcelReader.readStringList(parcel, header)"
                else -> "$variableName = $SafeParcelReader.readList(parcel, header, String.class.getClassLoader())"
            }
//            "java.util.List<java.lang.Integer>", "java.util.ArrayList<java.lang.Integer>" -> "$variableName = $SafeParcelReader.readIntegerList(parcel, header)"
//            "java.util.List<java.lang.Boolean>", "java.util.ArrayList<java.lang.Boolean>" -> "$variableName = $SafeParcelReader.readBooleanList(parcel, header)"
//            "java.util.List<java.lang.Long>", "java.util.ArrayList<java.lang.Long>" -> "$variableName = $SafeParcelReader.readLongList(parcel, header)"
//            "java.util.List<java.lang.Float>", "java.util.ArrayList<java.lang.Float>" -> "$variableName = $SafeParcelReader.readFloatList(parcel, header)"
//            "java.util.List<java.lang.Double>", "java.util.ArrayList<java.lang.Double>" -> "$variableName = $SafeParcelReader.readDoubleList(parcel, header)"
            else -> when {
                isList && isParcelable && !useValueParcel -> "$variableName = $SafeParcelReader.readParcelableList(parcel, header, $listItemType.CREATOR)"
                isArray && isParcelable -> "$variableName = $SafeParcelReader.readParcelableArray(parcel, header, $listItemType.CREATOR)"
                isList -> "$variableName = $SafeParcelReader.readList(parcel, header, $listItemType.class.getClassLoader())"
                isParcelable -> "$variableName = $SafeParcelReader.readParcelable(parcel, header, $type.CREATOR)"
                !isList && isIInterface -> "$variableName = $type.Stub.asInterface($SafeParcelReader.readBinder(parcel, header))"
                else -> throw UnsupportedOperationException("Field $name in ${clazz.fullName} has unsupported type $type.")
            }
        }
    }
    val readVariableFromParcelCase by lazy { "case $id: $readVariableFromParcel; break;" }
    val writeVariableToParcel by lazy {
        when (type) {
            "boolean", "byte", "char", "short", "int", "long", "float", "double",
            "java.lang.Boolean", "java.lang.Byte", "java.lang.Char", "java.lang.Short", "java.lang.Integer", "java.lang.Long", "java.lang.Float", "java.lang.Double" ->
                "$SafeParcelWriter.write(parcel, $id, $variableName);"

            "java.util.List<java.lang.String>", "java.util.ArrayList<java.lang.String>" -> when {
                !useValueParcel -> "$SafeParcelWriter.writeStringList(parcel, $id, $variableName, $mayNull);"
                else -> "$SafeParcelWriter.write(parcel, $id, $variableName, $mayNull);"
            }

            else -> when {
                isParcelable -> "$SafeParcelWriter.write(parcel, $id, $variableName, flags, $mayNull);"
                isIInterface -> "$SafeParcelWriter.write(parcel, $id, $variableName.asBinder(), $mayNull);"
                else -> "$SafeParcelWriter.write(parcel, $id, $variableName, $mayNull);"
            }
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
        when {
            getter != null -> listOf("$variableName = $getter;")
            isPrivate -> listOf(
                "$Field $reflectionFieldName = ${clazz.fullName}.class.getDeclaredField(\"$name\");",
                "$reflectionFieldName.setAccessible(true);",
                "$variableName = $reflectionFieldGetter(object);"
            )

            else -> listOf("$variableName = object.$name;")
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
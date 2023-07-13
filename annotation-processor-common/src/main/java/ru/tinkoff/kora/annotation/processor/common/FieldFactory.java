package ru.tinkoff.kora.annotation.processor.common;

import com.squareup.javapoet.*;

import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class FieldFactory {
    private final Types types;
    private final Elements elements;
    private final TypeSpec.Builder builder;
    private final MethodSpec.Builder constructor;
    private final Map<FieldKey, String> fields = new HashMap<>();
    private final String prefix;

    public String get(TypeName mapperType, Set<String> resultMapperTag) {
        return this.fields.get(new FieldKey(mapperType, resultMapperTag));
    }

    public String get(ClassName mapperType, TypeMirror mappedType, Element element) {
        var type = ParameterizedTypeName.get(mapperType, TypeName.get(mappedType).box());
        var tags = TagUtils.parseTagValue(element);
        var key = new FieldKey(type, tags);
        return this.fields.get(key);
    }

    public String get(ClassName mapperType, CommonUtils.MappingData mappingData, TypeMirror type) {
        var mapperClass = mappingData.mapperClass();
        if (mapperClass == null) {
            var mapperTypeName = ParameterizedTypeName.get(mapperType, TypeName.get(type).box());
            return this.fields.get(new FieldKey(mapperTypeName, mappingData.mapperTags()));
        }
        var mapperTypeName = TypeName.get(mapperClass);
        return this.fields.get(new FieldKey(mapperTypeName, mappingData.mapperTags()));
    }

//    public String get(TypeMirror mapperClass, Set<String> resultMapperTag) {
//        var mapperType = TypeName.get(mapperClass);
//        return this.fields.get(new FieldKey(mapperType, resultMapperTag));
//    }

    record FieldKey(TypeName typeName, Set<String> tags) {}

    public FieldFactory(Types types, Elements elements, TypeSpec.Builder builder, MethodSpec.Builder constructor, String prefix) {
        this.types = types;
        this.elements = elements;
        this.builder = builder;
        this.constructor = constructor;
        this.prefix = prefix;
    }

    public String add(TypeName typeName, Set<String> tags) {
        var key = new FieldKey(typeName, tags);
        var existed = this.fields.get(key);
        if (existed != null) {
            return existed;
        }
        var name = this.prefix + (this.fields.size() + 1);
        this.fields.put(key, name);
        this.builder.addField(typeName, name, Modifier.PRIVATE, Modifier.FINAL);
        var parameter = ParameterSpec.builder(typeName, name);
        var tag = CommonUtils.toTagAnnotation(tags);
        if (tag != null) {
            parameter.addAnnotation(tag);
        }
        this.constructor.addParameter(parameter.build());
        this.constructor.addStatement("this.$N = $N", name, name);
        return name;
    }

    public String add(TypeName typeName, CodeBlock initializer) {
        var key = new FieldKey(typeName, Set.of());
        var existed = this.fields.get(key);
        if (existed != null) {
            return existed;
        }
        var name = this.prefix + (this.fields.size() + 1);
        this.fields.put(key, name);
        this.builder.addField(typeName, name, Modifier.PRIVATE, Modifier.FINAL);
        this.constructor.addStatement("this.$N = $L", name, initializer);
        return name;
    }

    public String add(TypeMirror typeMirror, Set<String> tags) {
        var typeName = TypeName.get(typeMirror);
        var key = new FieldKey(typeName, tags);
        var existed = this.fields.get(key);
        if (existed != null) {
            return existed;
        }
        var name = this.prefix + (this.fields.size() + 1);
        this.fields.put(key, name);
        this.builder.addField(typeName, name, Modifier.PRIVATE, Modifier.FINAL);
        if (tags.isEmpty() && typeMirror.getKind() == TypeKind.DECLARED && CommonUtils.hasDefaultConstructorAndFinal(this.types, typeMirror)) {
            this.constructor.addStatement("this.$N = new $T()", name, typeName);
        } else {
            this.constructor.addParameter(typeName, name);
            this.constructor.addStatement("this.$N = $N", name, name);
        }
        return name;
    }

    public String add(@Nullable CommonUtils.MappingData mapping, TypeName defaultType) {
        var tags = mapping == null
            ? Set.<String>of()
            : mapping.mapperTags();
        var typeName = mapping == null
            ? defaultType
            : TypeName.get(Objects.requireNonNull(mapping.mapperClass()));
        var typeElement = typeName instanceof ParameterizedTypeName ptn
            ? this.elements.getTypeElement(ptn.rawType.canonicalName())
            : this.elements.getTypeElement(((ClassName) typeName).canonicalName());

        var key = new FieldKey(typeName, tags);
        var existed = this.fields.get(key);
        if (existed != null) {
            return existed;
        }
        var name = this.prefix + (this.fields.size() + 1);
        this.fields.put(key, name);
        this.builder.addField(typeName, name, Modifier.PRIVATE, Modifier.FINAL);
        if (tags.isEmpty() && CommonUtils.hasDefaultConstructorAndFinal(typeElement)) {
            this.constructor.addStatement("this.$N = new $T()", name, typeName);
        } else {
            var parameter = ParameterSpec.builder(typeName, name);
            var tag = CommonUtils.toTagAnnotation(tags);
            if (tag != null) {
                parameter.addAnnotation(tag);
            }
            this.constructor.addParameter(parameter.build());
            this.constructor.addStatement("this.$N = $N", name, name);
        }
        return name;
    }
}

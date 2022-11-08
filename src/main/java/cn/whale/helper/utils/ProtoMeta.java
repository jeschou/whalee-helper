package cn.whale.helper.utils;

import com.squareup.wire.schema.Field;
import com.squareup.wire.schema.Location;
import com.squareup.wire.schema.internal.parser.*;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * parse proto file and its imports
 */
public class ProtoMeta {
    /**
     * base path of all protos (the project root)
     */
    String base;
    /**
     * all message, enum. <br>
     * and message enum from imported proto (with proto package prefix, e.g.  whale.authority.proto.PlatformType)
     */
    public Map<String, TypeElement> typeMaps = new HashMap<>();

    public Map<String, ProtoFileElement> elementFileMaps = new HashMap<>();

    public ProtoMeta(String base) {
        this.base = base;
    }


    public ProtoFileElement parse(String protoPath) {
        try {
            Location loc = new Location(base, protoPath, 1, 1);
            String text = Utils.readText(new File(loc.getBase(), loc.getPath()));
            ProtoFileElement pf = ProtoParser.Companion.parse(loc, text);
            // this is  different with parseDep
            pf.getTypes().forEach(e -> typeMaps.put(e.getName(), e));
            for (String imp : pf.getImports()) {
                parseDep(imp);
            }
            return pf;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void parseDep(String protoPath) {
        try {
            Location loc = new Location(base, protoPath, 1, 1);
            String text = Utils.readText(new File(loc.getBase(), loc.getPath()));
            ProtoFileElement pf = ProtoParser.Companion.parse(loc, text);
            // this is  different with parse
            pf.getTypes().forEach(e -> {
                String externalName = pf.getPackageName() + "." + e.getName();
                typeMaps.put(externalName, e);
                elementFileMaps.put(externalName, pf);
            });
            for (String imp : pf.getImports()) {
                parseDep(imp);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * get a nested map (with default value), represent the logical structure of TypeElement
     *
     * @param typeElement
     * @return
     */
    public Map<String, Object> toJsonMap(TypeElement typeElement) {
        Map<String, Object> map = new HashMap<>();
        if (typeElement instanceof MessageElement) {
            for (FieldElement field : ((MessageElement) typeElement).getFields()) {
                // find in nested type first
                TypeElement fieldType = getNested((MessageElement) typeElement, field.getType());
                if (fieldType == null) {
                    // find in declared types
                    fieldType = typeMaps.get(field.getType());
                }
                Object val = null;
                if (fieldType != null) {
                    if (fieldType instanceof EnumElement) {
                        // take second enum as default value
                        val = ((EnumElement) fieldType).getConstants().get(1).getName();
                    } else {
                        if (fieldType == typeElement) {
                            // 防止递归
                            val = null;
                        } else {
                            val = toJsonMap(fieldType);
                        }
                    }
                } else {
                    val = getDefaultVal(field.getType(), field.getName());
                }
                if (field.getLabel() == Field.Label.REPEATED) {
                    // if array type, wrap it
                    if (val == null) {
                        val = new Object[0];
                    } else {
                        val = new Object[]{val};
                    }
                }
                map.put(field.getName(), val);
            }
        }
        return map;
    }

    static TypeElement getNested(MessageElement me, String typeName) {
        for (TypeElement te : me.getNestedTypes()) {
            if (te.getName().equals(typeName)) {
                return te;
            }
        }
        return null;
    }

    static Object getDefaultVal(String typename, String fieldName) {
        Object val = defaultValue.get(fieldName);
        if (val != null) {
            return val;
        }
        if (fieldName.endsWith("_time")) {
            return System.currentTimeMillis() / 1000;
        } else if ("CCC".equalsIgnoreCase(fieldName)) {
            return "+86";
        } else if (fieldName.startsWith("is_") && typename.startsWith("int")) {
            return 0;
        } else if (typename.startsWith("map<")) {
            return new HashMap<>();
        }
        return defaultValue.get(typename);
    }

    static Map<String, Object> defaultValue = new HashMap<>() {
        {

            // default value by fieldType
            put("string", "Hello");
            put("int32", 10);
            put("int64", 10);
            put("float", 10.0);
            put("bool", false);
            put("map<string,string>", new HashMap<>());
            ///// default value by fieldName
            put("company_id", "c53ac779-662f-4e2b-a63d-5fd351f0ef51");
            put("user_id", "3604652527122445056");
            put("offset", 0);
            put("phone", "13764784776");
            put("platform", "Alivia");
            put("is_delete", 0);
            put("is_deleted", 0);
            put("bind_zone_id", 3605190);
            put("client_id", "alivia");
            put("client_secret", "s&WZ^410^s");
        }
    };


}
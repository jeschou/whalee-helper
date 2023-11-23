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

    public Map<String, String> locationPkgMap = new HashMap<>();

    public ProtoMeta(String base) {
        this.base = base;
    }


    public ProtoFileElement parse(String protoPath) {
        ConfigUtil.loadDefaultValueCfg();
        try {
            Location loc = new Location(base, protoPath, 1, 1);
            String text = Utils.readText(new File(loc.getBase(), loc.getPath()));
            ProtoFileElement pf = ProtoParser.Companion.parse(loc, text);
            locationPkgMap.put(protoPath, pf.getPackageName());
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
            locationPkgMap.put(protoPath, pf.getPackageName());
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
                    if (fieldType == null && Character.isUpperCase(field.getType().charAt(0))) {
                        String belongPkg = locationPkgMap.get(field.getLocation().getPath());
                        if (belongPkg != null) {
                            fieldType = typeMaps.get(belongPkg + "." + field.getType());
                        }
                    }

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
                    val = ConfigUtil.getDefaultVal(field.getType(), field.getName());
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
}
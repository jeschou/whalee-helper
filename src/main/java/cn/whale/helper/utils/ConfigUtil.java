package cn.whale.helper.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class ConfigUtil {


    public static Object getDefaultVal(String typename, String fieldName) {
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
            put("integer", 10);
            put("int32", 10);
            put("int64", 10);
            put("float", 10.0);
            put("bool", false);
            put("boolean", false);
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

    public static Properties loadProperties() {
        Properties properties = new Properties();
        File f = new File(System.getProperty("user.home"), ".whalee-helper.properties");
        if (!f.isFile()) {
            return properties;
        }
        try {
            properties.load(new FileInputStream(f));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }


    public static void loadDefaultValueCfg() {
        Properties properties = loadProperties();

        properties.forEach((k, v) -> {
            if (!(k instanceof String) || !(v instanceof String)) {
                return;
            }
            if (!k.toString().startsWith("grpc.default.")) {
                return;
            }
            String ks = k.toString().substring(13).trim();
            String vs = v.toString().trim();
            if (vs.startsWith("\"") && vs.endsWith("\"")) {
                defaultValue.put(ks, Utils.substrBetweenQuote(vs));
            } else if (vs.equals("true") || vs.equals("false")) {
                defaultValue.put(ks, Boolean.parseBoolean(vs));
            } else {
                try {
                    double d = Double.parseDouble(vs);
                    if (d == (long) d) {
                        defaultValue.put(ks, (long) d);
                    } else {
                        defaultValue.put(ks, d);
                    }
                } catch (NumberFormatException e) {
                    defaultValue.put(ks, vs);
                }
            }
        });
    }
}

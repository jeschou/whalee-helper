package cn.whale.helper.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.util.List;

public class NacosUtil {
    static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static List<DbConfig> getDbConfigList() throws Exception {
        URL url = new URL("http://nacos-dev.meetwhale.com:8848/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-develop");
        DbConfig dbconfig = mapper.readValue(url, DbConfig.class);
        dbconfig.serviceName = "default";
        return List.of(dbconfig);
    }
}

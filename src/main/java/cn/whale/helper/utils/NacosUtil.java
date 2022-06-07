package cn.whale.helper.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URL;
import java.util.List;

public class NacosUtil {
    static ObjectMapper mapper = new ObjectMapper();

    public static List<DbConfig> getDbConfigList() throws Exception {
        URL url = new URL("https://nacos.meetwhale.com/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-develop");
        DbConfig dbconfig = mapper.readValue(url, DbConfig.class);
        dbconfig.serviceName = "default";
        return List.of(dbconfig);
    }
}

package cn.whale.helper.utils;

import java.net.URL;
import java.util.List;

public class NacosUtil {

    public static List<DbConfig> getDbConfigList() throws Exception {
        URL url = new URL("http://nacos-dev.meetwhale.com:8848/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-develop");
        DbConfig dbconfig = Utils.mapper.readValue(url, DbConfig.class);
        dbconfig.serviceName = "default";
        return List.of(dbconfig);
    }
}

package cn.whale.helper.utils;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NacosUtil {

    public static List<DbConfig> getDbConfigList() throws Exception {
        List<DbConfig> list = new ArrayList<>();
        URL url = new URL("http://nacos-dev.meetwhale.com:8848/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-develop");
        DbConfig develop = Utils.mapper.readValue(url, DbConfig.class);
        develop.serviceName = "develop";
        list.add(develop);
        URL url2 = new URL("http://nacos-dev.meetwhale.com:8848/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-test");
        DbConfig test = Utils.mapper.readValue(url2, DbConfig.class);
        test.serviceName = "test";
        list.add(test);
        Properties properties = Utils.loadProperties();
        properties.forEach((k, v) -> {
            if (!(k instanceof String) || !(v instanceof String)) {
                return;
            }
            String kstr = k.toString();
            String serviceName = Utils.substringAfter(kstr, "repo.dbconfig.");
            if (kstr.equals(serviceName)) {
                return;
            }
            try {
                DbConfig dbconfig0 = Utils.mapper.readValue(v.toString(), DbConfig.class);
                dbconfig0.serviceName = serviceName;
                list.add(dbconfig0);
            } catch (JsonProcessingException e) {
                e.printStackTrace();
            }
        });
        return list;
    }
}

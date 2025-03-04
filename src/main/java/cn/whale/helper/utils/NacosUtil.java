package cn.whale.helper.utils;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NacosUtil {

    static final String NACOS_HOST = "http://nacos-dev.meetwhale.com:8848";

    public static List<DbConfig> getDbConfigList() throws Exception {
        List<DbConfig> list = new ArrayList<>();
        InputStream is = Utils.openURLNoProxy(NACOS_HOST + "/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-develop");
        DbConfig develop = Utils.mapper.readValue(is, DbConfig.class);
        develop.serviceName = "develop";
        list.add(develop);
        is = Utils.openURLNoProxy("http://nacos-dev.meetwhale.com:8848/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-test");
        DbConfig test = Utils.mapper.readValue(is, DbConfig.class);
        test.serviceName = "test";
        list.add(test);
        Properties properties = ConfigUtil.loadProperties();
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

    public static String getIconFontAlivia() throws Exception {
        return Utils.trimToEmpty(Utils.readUrlAsTextNoProxy(NACOS_HOST + "/nacos/v1/cs/configs?dataId=iconfont.alivia&group=SITEVAR&namespaceId=develop&tenant=develop"));
    }
}

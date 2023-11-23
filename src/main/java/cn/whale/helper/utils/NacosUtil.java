package cn.whale.helper.utils;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.InputStream;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class NacosUtil {

    private static InputStream connect(String url0) throws Exception {
        URL url = new URL(url0);
        URLConnection uc = url.openConnection(Proxy.NO_PROXY);
        uc.setConnectTimeout(5 * 1000);
        uc.setReadTimeout(5 * 1000);
        uc.connect();
        return uc.getInputStream();
    }

    public static List<DbConfig> getDbConfigList() throws Exception {
        List<DbConfig> list = new ArrayList<>();
        InputStream is = connect("http://nacos-dev.meetwhale.com:8848/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-develop");
        DbConfig develop = Utils.mapper.readValue(is, DbConfig.class);
        develop.serviceName = "develop";
        list.add(develop);
        is = connect("http://nacos-dev.meetwhale.com:8848/nacos/v1/cs/configs?dataId=pg-config&group=DEFAULT&tenant=frameworks-test");
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
}

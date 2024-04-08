package cn.whale.helper.utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;

public class I18nUtil {

    static final String url = "https://oss-whale-alivia.meetwhale.com/i18/develop/zh-CN/custom-alivia-components/web/components.json";
    static String lastModified = "";
    static Map<String, String> map = new HashMap<>();

    public static void initLoad() {
        try {
            URLConnection uc = Utils.connectURLNoProxy(url);
            lastModified = uc.getHeaderField("Last-Modified");
            map = Utils.mapper.readValue(uc.getInputStream(), HashMap.class);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getTranslation(String key) throws IOException {
        URLConnection uc = Utils.connectURLNoProxy(url);
        String lastModified0 = uc.getHeaderField("Last-Modified");
        if (lastModified0.equals(lastModified)) {
            ((HttpURLConnection) uc).disconnect();
            return map.get(key);
        }
        map = Utils.mapper.readValue(uc.getInputStream(), HashMap.class);
        return map.get(key);
    }
}

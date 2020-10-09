package cn.whale.helper.utils;

import com.google.gson.Gson;

import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EtcdUtil {
    static final String etcdkeeper_host = "http://etcdkeeper.meetwhale.com:12000/v3";
    static final String etcd_host = "etcd-pro.meetwhale.com:5379";
    static final String path = "/develop/config/pgsql";

    static Gson gson = new Gson();


    static long sessionAliveUntil = 0;
    static String sessionId = "";

    public static String getEtcdKeeperSessionId() throws Exception {
        if (Utils.isNotEmpty(sessionId) && sessionAliveUntil > System.currentTimeMillis()) {
            return sessionId;
        }
        sessionId = null;
        URL url = new URL(etcdkeeper_host + "/connect");
        URLConnection conn = url.openConnection();
        conn.setDoInput(true);
        conn.setDoOutput(true);

        conn.connect();
        OutputStream os = conn.getOutputStream();
        os.write(("host=" + etcd_host).getBytes());
        Utils.safeClose(os);
        Utils.readText(conn.getInputStream());
        Map<String, List<String>> headerMap = conn.getHeaderFields();
        List<String> cookies = headerMap.get("Set-Cookie");
        if (cookies == null || cookies.size() == 0) {
            return null;
        }
        String[] segs = cookies.get(0).split(";");
        for (String str : segs) {
            str = str.trim();
            String[] strs = str.split("=");
            if (strs.length != 2) {
                continue;
            }
            if ("_etcdkeeper_session".equals(strs[0])) {
                sessionId = strs[1];
            } else if ("Max-Age".equals(strs[0])) {
                sessionAliveUntil = System.currentTimeMillis() + Integer.parseInt(strs[1]) * 1000;
            }
        }

        return sessionId;
    }

    public static List<DbConfig> getDbConfigList() throws Exception {
        List<DbConfig> list = new ArrayList<>();
        URL url = new URL(etcdkeeper_host + "/getpath?prefix=true&key=" + URLEncoder.encode(path, "UTF-8"));
        URLConnection conn = url.openConnection();
        conn.addRequestProperty("Cookie", "etcd-endpoint=" + etcd_host + "; _etcdkeeper_session=" + getEtcdKeeperSessionId());
        conn.connect();
        String resp = Utils.readText(conn.getInputStream());
        VirtualNode virtualNode = gson.fromJson(resp, VirtualNode.class);
        for (KV kv : virtualNode.node.nodes) {
            String serviceName = Utils.substringAfterLast(kv.key, "/");
            if ("db_host".equals(serviceName)) {
                continue;
            }
            DbConfig dbConfig = gson.fromJson(kv.value, DbConfig.class);
            dbConfig.serviceName = serviceName;
            list.add(dbConfig);
        }
        return list;
    }

    public static class VirtualNode {
        public Node node;
    }

    public static class Node {
        public KV[] nodes;
    }

    public static class KV {
        public String key;
        public String value;
    }

}

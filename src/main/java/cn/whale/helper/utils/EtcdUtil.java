package cn.whale.helper.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.etcd.jetcd.ByteSequence;
import io.etcd.jetcd.Client;
import io.etcd.jetcd.KeyValue;
import io.etcd.jetcd.kv.GetResponse;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.resolver.URIResolverLoader;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class EtcdUtil {
    static final String etcd_endpoint = "http://etcd-pro.meetwhale.com:5379";
    static final String path = "/develop/config/pgsql";
    static Charset UTF_8 = StandardCharsets.UTF_8;

    static ObjectMapper mapper = new ObjectMapper();

    static ByteSequence BS(String s) {
        return ByteSequence.from(s, UTF_8);
    }
    static Set<String> skipCfg = new HashSet<>(Arrays.asList("db_host", "clickhouse", "hive"));

    public static List<DbConfig> getDbConfigList() throws Exception {
        new io.etcd.jetcd.resolver.SmartNameResolver(null, null, URIResolverLoader.defaultLoader());
        List<DbConfig> list = new ArrayList<>();
        try (Client etcdClient = Client.builder().endpoints(etcd_endpoint).build()) {
            GetResponse getResponse = etcdClient.getKVClient().get(BS(path), GetOption.newBuilder().withPrefix(BS(path)).build()).get();
            for (KeyValue kv : getResponse.getKvs()) {
                if (kv.getValue().isEmpty()) continue;

                try {
                    DbConfig cfg = mapper.readValue(kv.getValue().getBytes(), DbConfig.class);
                    cfg.serviceName = Utils.substringAfterLast(kv.getKey().toString(UTF_8), "/");
                    if (skipCfg.contains(cfg.serviceName)) {
                        continue;
                    }
                    list.add(cfg);
                } catch (Exception e) {
                    //e.printStackTrace();
                }
            }
        }
        return list;
    }

}

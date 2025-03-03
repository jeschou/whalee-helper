package cn.whale.helper.action;

public class TestOpenplatformGrpcAction extends TestGrpcAction {

    static ClusterInfo DevClusterInfo = new ClusterInfo("openplatform-pro", "10.9.255.109:31455", new String[]{
            "x-envoy-original-dst-host: ",
            "lang: zh-CN"
    });

    @Override
    protected ClusterInfo getClusterInfo() {
        return DevClusterInfo;
    }
}

package cn.whale.helper.action;

public class TestDevGrpcAction extends TestGrpcAction {

    static ClusterInfo DevClusterInfo = new ClusterInfo("product-dev", "10.200.2.110:31138", new String[]{
            "x-envoy-original-dst-host: ",
            "lang: zh-CN"
    });

    @Override
    protected ClusterInfo getClusterInfo() {
        return DevClusterInfo;
    }
}

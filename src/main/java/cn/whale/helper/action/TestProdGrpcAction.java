package cn.whale.helper.action;

public class TestProdGrpcAction extends TestGrpcAction {

    static ClusterInfo ProdClusterInfo = new ClusterInfo("product-prod", "10.168.0.161:32668", new String[]{
            "x-envoy-original-dst-host: ",
            "lang: zh-CN"
    });

    @Override
    protected ClusterInfo getClusterInfo() {
        return ProdClusterInfo;
    }
}

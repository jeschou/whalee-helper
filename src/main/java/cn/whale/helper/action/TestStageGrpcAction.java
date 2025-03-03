package cn.whale.helper.action;

public class TestStageGrpcAction extends TestGrpcAction {

    static ClusterInfo StageClusterInfo = new ClusterInfo("product-stage", "10.202.0.222:32282", new String[]{
            "x-envoy-original-dst-host: ",
            "lang: zh-CN"
    });

    @Override
    protected ClusterInfo getClusterInfo() {
        return StageClusterInfo;
    }
}

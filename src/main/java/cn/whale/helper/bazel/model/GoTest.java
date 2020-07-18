package cn.whale.helper.bazel.model;

import cn.whale.helper.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GoTest extends Def {
    public GoTest(List<String> srcs, String embed, List<String> deps) {
        super("go_test");
        params.add(new Param("name", "go_default_test"));
        params.add(new ListParam("srcs", srcs));
        if (Utils.isNotEmpty(embed)) {
            params.add(new Param("embed", embed));
        }
        params.add(new ListParam("visibility", newArrayList("//visibility:public")));
        if (deps.size() > 0) {
            params.add(new ListParam("deps", deps));
        }
    }

    static List<String> newArrayList(String... args) {
        return new ArrayList<>(Arrays.asList(args));
    }
}



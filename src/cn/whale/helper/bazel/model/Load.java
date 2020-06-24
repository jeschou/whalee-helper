package cn.whale.helper.bazel.model;

import cn.whale.helper.utils.Utils;

import java.util.ArrayList;
import java.util.List;

class Load {
    String bzl = "";
    List<Def> defs = new ArrayList<>();

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("load(");
        sb.append(Utils.quote(bzl));

        for (Def d : defs) {
            sb.append(", ").append(Utils.quote(d.name));
        }
        sb.append(")");
        return sb.toString();
    }

    public void addDef(Def def) {
        defs.add(def);
        def.loadRef = this;
    }
}

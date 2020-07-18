package cn.whale.helper.bazel.model;

import cn.whale.helper.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static cn.whale.helper.bazel.model.BazelBuild.INDENT;

public class Def {
    public Load loadRef;
    public String name = "";
    public List<Param> params = new ArrayList<>();
    String comment = "";

    public Def(String s) {
        this.name = s;
    }

    public Param getParamByName(String pn) {
        for (Param p : params) {
            if (p.name.equals(pn)) {
                return p;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (Utils.isNotEmpty(comment)) {
            sb.append(comment).append("\n");
        }
        sb.append(name).append("(\n");
        String indent = INDENT;
        for (Param p : params) {
            sb.append(indent).append(p.toString(indent)).append(",\n");
        }
        sb.append(")");
        return sb.toString();
    }

}

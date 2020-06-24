package cn.whale.helper.bazel.model;

import cn.whale.helper.utils.Utils;

import java.util.ArrayList;
import java.util.List;

import static cn.whale.helper.bazel.model.BazelBuild.INDENT;

public class ListParam extends Param {
    public List<String> values = new ArrayList<>();
    boolean originalMultiLine = false;

    public ListParam() {

    }

    public ListParam(String name, List<String> values) {
        super(name, "");
        this.values = values;
    }

    public String toString(String baseIndent) {
        StringBuilder sb = new StringBuilder();
        sb.append(name).append(" ").append("= [");
        if (originalMultiLine || values.size() > 1) {
            sb.append("\n");
            for (String val : values) {
                sb.append(baseIndent).append(INDENT).append(Utils.quote(val)).append(",\n");
            }
            sb.append(baseIndent);
        } else if (values.size() == 1) {
            sb.append(Utils.quote(values.get(0)));
        }
        sb.append("]");
        return sb.toString();
    }
}

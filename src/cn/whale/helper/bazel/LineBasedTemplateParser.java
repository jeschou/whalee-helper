package cn.whale.helper.bazel;

import cn.whale.helper.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LineBasedTemplateParser {
    static String FULL_LINE_PARAM_START = "$!{";
    static String NORMAL_PARAM_START = "${";
    static String PARAM_END = "}";

    private List<LineSeg> lines;
    private Map<String, List<LineSeg>> paramMap = new HashMap<>();

    public void loadTemplate(InputStream inputStream) throws IOException {
        lines = new ArrayList<>();
        for (String line : Utils.readLines(inputStream)) {
            lines.add(new LineSeg(line));
        }
    }

    public void parse(Map<String, String> params) {
        params.forEach((k, v) -> {
            List<LineSeg> ps = paramMap.get(k);
            if (ps != null) {
                for (LineSeg ls : ps) {
                    ls.replace(v);
                }
            }
        });
    }

    public String getResult() {
        StringBuilder stringBuilder = new StringBuilder();
        for (LineSeg lineSeg : lines) {
            if (lineSeg.shouldRemove) {
                continue;
            }
            stringBuilder.append(lineSeg.line).append("\n");
        }
        return stringBuilder.toString();
    }

    class LineSeg {


        String line;
        boolean fullLine;
        String paramName;

        boolean replaced;
        boolean shouldRemove = true;


        public LineSeg(String line) {
            this.line = line;
            int idx = line.indexOf(FULL_LINE_PARAM_START);
            if (idx == -1) {
                idx = line.indexOf(NORMAL_PARAM_START);
            } else {
                fullLine = true;
            }
            if (idx == -1) {
                return;
            }
            int idx2 = line.indexOf(PARAM_END, idx);
            if (idx2 == -1) {
                return;
            }
            paramName = line.substring(idx + (fullLine ? FULL_LINE_PARAM_START.length() : NORMAL_PARAM_START.length()), idx2);
            paramMap.computeIfAbsent(paramName, s -> new ArrayList<>()).add(this);
        }

        public void replace(String str) {
            if (replaced) return;
            if (fullLine) {
                if (Utils.isNotEmpty(str)) {
                    line = line.replace(FULL_LINE_PARAM_START + paramName + PARAM_END, str);
                    shouldRemove = false;
                }
            } else {
                line = line.replace(NORMAL_PARAM_START + paramName + PARAM_END, str);
                shouldRemove = false;
            }
            replaced = true;
        }
    }
}



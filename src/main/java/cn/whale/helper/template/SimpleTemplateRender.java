package cn.whale.helper.template;

import cn.whale.helper.utils.Utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static cn.whale.helper.template.TemplateParam.*;

public class SimpleTemplateRender {
    /**
     * 必须参数, 否则删除当前行
     */
    static String FULL_LINE_PARAM_START = "$!{";
    /**
     * 必须参数, 否则删除当前行. 但渲染输出内容为空
     */
    static String TEST_LINE_PARAM_START = "$t{";
    /**
     * 普通参数
     */
    static String NORMAL_PARAM_START = "${";
    static String PARAM_END = "}";


    private List<TemplateSection> sections;
    private Map<String, Set<TemplateSection>> paramMap = new HashMap<>();

    static List<TemplateParam> parseAllParam(String line) {
        int idx = 0;
        List<TemplateParam> params = new ArrayList<>();
        while (true) {
            TemplateParam param = parseParam(line, idx, FULL_LINE_PARAM_START, PARAM_TYPE_REQUIRED);
            if (param == null) {
                param = parseParam(line, idx, TEST_LINE_PARAM_START, PARAM_TYPE_TEST);
            }
            if (param == null) {
                param = parseParam(line, idx, NORMAL_PARAM_START, PARAM_TYPE_NORMAL);
            }
            if (param == null) {
                break;
            }
            idx = param.end + 1;
            params.add(param);
        }
        return params;
    }

    static TemplateParam parseParam(String line, int idx0, String prefix, int type) {
        int idx = line.indexOf(prefix, idx0);
        if (idx == -1) {
            return null;
        }
        int idx2 = line.indexOf(PARAM_END, idx + 1);
        if (idx2 == -1) return null;
        TemplateParam param = new TemplateParam();
        param.name = line.substring(idx + prefix.length(), idx2);
        param.type = type;
        param.prefix = prefix;
        param.suffix = PARAM_END;
        param.begin = idx;
        param.end = idx2;
        return param;
    }

    public void loadTemplate(InputStream inputStream) throws IOException {
        sections = new ArrayList<>();
        Stack<TemplateBlockSection> blockStack = new Stack<>();
        List<String> lines = Utils.readLines(inputStream);
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line.startsWith("#if ")) {
                TemplateBlockSection ifSection = new TemplateBlockSection(createLineSection(line));
                if (blockStack.isEmpty()) {
                    // top level block, add to global
                    sections.add(ifSection);
                } else {
                    // already in a block, add to it
                    blockStack.peek().addSection(ifSection);
                }
                blockStack.push(ifSection);
                continue;
            }
            if (line.equals("#fi")) {
                if (!blockStack.isEmpty()) {
                    blockStack.pop();
                } else {
                    throw new RuntimeException("unexpected #fi at line " + (i + 1));
                }
                continue;
            }
            if (!blockStack.isEmpty()) {
                // add line to current block
                blockStack.peek().addSection(createLineSection(line));
                continue;
            }
            sections.add(createLineSection(line));
        }
    }

    public void render(Map<String, String> params) {
        params.forEach((k, v) -> {
            Set<TemplateSection> ps = paramMap.get(k);
            if (ps != null) {
                for (TemplateSection ls : ps) {
                    ls.set(k, v);
                }
            }
        });
    }

    public String getResult() {
        StringBuilder stringBuilder = new StringBuilder();
        for (TemplateSection templateSection : sections) {
            String renderedContent = templateSection.render();
            if (renderedContent == null) {
                continue;
            }
            stringBuilder.append(renderedContent).append("\n");
        }
        return stringBuilder.toString();
    }

    private TemplateLineSection createLineSection(String line) {
        List<TemplateParam> params = parseAllParam(line);
        TemplateLineSection lineSection = new TemplateLineSection(line, params);
        params.forEach(param -> paramMap.computeIfAbsent(param.name, s -> new HashSet<>()).add(lineSection));
        return lineSection;
    }
}



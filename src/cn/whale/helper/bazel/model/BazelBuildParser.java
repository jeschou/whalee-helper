package cn.whale.helper.bazel.model;

import cn.whale.helper.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BazelBuildParser {

    public static void main(String[] args) throws Exception {
        String path = "/Users/a123/whale/whgo/product/whale-resource/cron/mts_cron/BUILD.bazel";
        try (BufferedReader reader = Utils.reader(new File(path))) {
            BazelBuild bb = parse(reader);
            System.out.println(bb);
        }
    }

    public static BazelBuild parse(BufferedReader reader) throws IOException {
        BazelBuild bazelBuild = new BazelBuild();
        String line = null;
        String comment = "";
        while ((line = reader.readLine()) != null) {
            if (line.startsWith("load(")) {
                bazelBuild.loadList.add(parseLoad(line));
            }
            if (line.startsWith("#")) {
                comment = line;
            }
            if (line.length() > 1 && line.endsWith("(")) {
                String name = line.substring(0, line.indexOf('('));
                Def def = bazelBuild.getDefByName(name);
                def.comment = comment;
                comment = "";
                parseDef(reader, def);
                bazelBuild.defList.add(def);
            }
        }
        Utils.safeClose(reader);
        return bazelBuild;
    }

    static Load parseLoad(String line) {
        Load load = new Load();
        List<String> segs = extractQuoteSegs(line);
        for (int i = 0; i < segs.size(); i++) {
            if (i == 0) {
                load.bzl = segs.get(i);
            } else {
                load.addDef(new Def(segs.get(i)));
            }
        }
        return load;
    }

    static void parseDef(BufferedReader reader, Def def) throws IOException {
        String line = null;
        ListParam multiLineListParam = null;
        while ((line = reader.readLine()) != null) {
            if (multiLineListParam != null) {
                line = line.trim();
                if (line.startsWith("\"")) {
                    multiLineListParam.values.addAll(extractQuoteSegs(line));
                } else if (line.startsWith("]")) {
                    def.params.add(multiLineListParam);
                    if (multiLineListParam.values.size() == 1) {
                        multiLineListParam.originalMultiLine = true;
                    }
                    multiLineListParam = null;
                }
            }
            if (line.endsWith(")")) {
                return;
            }
            int idx_ = line.indexOf('=');
            if (idx_ != -1) {
                int idx_b0 = line.indexOf('[', idx_);
                if (idx_b0 != -1) {
                    // list param
                    ListParam listParam = new ListParam();
                    listParam.name = line.substring(0, idx_).trim();
                    int idx_b1 = line.indexOf(']', idx_b0 + 1);
                    if (idx_b1 == -1) {
                        // multi line
                        multiLineListParam = listParam;
                        continue;
                    } else {
                        // single line
                        String paramListRaw = line.substring(idx_b0 + 1, idx_b1);
                        List<String> segs = extractQuoteSegs(paramListRaw);
                        listParam.values.addAll(segs);
                        def.params.add(listParam);
                    }

                } else {
                    // single param
                    Param param = new Param() {
                    };
                    param.name = line.substring(0, idx_).trim();
                    param.value = Utils.substrBetweenQuote(line);
                    def.params.add(param);
                }
            }
        }
    }

    static List<String> extractQuoteSegs(String raw) {
        List<String> segs = new ArrayList<>();
        int idx0 = raw.indexOf('"');
        int idx1 = idx0;
        while ((idx1 = raw.indexOf('"', idx0 + 1)) != -1) {
            segs.add(raw.substring(idx0 + 1, idx1));
            idx0 = raw.indexOf('"', idx1 + 1);
            if (idx0 == -1) {
                break;
            }
        }
        return segs;
    }
}

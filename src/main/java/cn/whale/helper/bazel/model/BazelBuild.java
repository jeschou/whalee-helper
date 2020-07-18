package cn.whale.helper.bazel.model;

import java.util.ArrayList;
import java.util.List;

public class BazelBuild {

    static final String INDENT = "    ";

    List<Load> loadList = new ArrayList<>();
    List<Def> defList = new ArrayList<>();

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Load load : loadList) {
            sb.append(load.toString()).append("\n");
        }
        sb.append("\n");

        for (Def def : defList) {
            sb.append(def.toString()).append("\n\n");
        }

        return sb.substring(0, sb.length() - 1);
    }

    public Def getDefByName(String name) {
        for (Load load : loadList) {
            for (Def def : load.defs) {
                if (def.name.equals(name)) {
                    return def;
                }
            }
        }
        return null;
    }

    public void removeDef(String name) {
        Def def = getDefByName(name);
        if (def != null) {
            def.loadRef.defs.remove(def);
            defList.remove(def);
        }
    }


    public void addGoTest(Def gotest) {
        Def def = getDefByName(gotest.name);
        if (def != null) {
            throw new IllegalStateException(gotest.name + " already exists");
        }
        Load load0 = loadList.get(0);
        load0.defs.add(gotest);
        gotest.loadRef = load0;

        int maxIndex0 = 1;

        for (Def d : load0.defs) {
            maxIndex0 = Math.max(maxIndex0, defList.indexOf(d));
        }
        if (maxIndex0 == defList.size() - 1) {
            defList.add(gotest);
        } else {
            defList.add(maxIndex0 + 1, gotest);
        }
    }
}


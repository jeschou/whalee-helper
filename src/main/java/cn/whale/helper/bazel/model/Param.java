package cn.whale.helper.bazel.model;

import cn.whale.helper.utils.Utils;

public class Param {
    public String name = "";
    public String value = "";

    public Param() {
    }

    public Param(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public String toString(String indent) {
        return name + " = " + Utils.quote(value);
    }
}

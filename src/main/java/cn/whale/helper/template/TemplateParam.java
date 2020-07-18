package cn.whale.helper.template;

public class TemplateParam {
    public static int PARAM_TYPE_REQUIRED = 1;
    public static int PARAM_TYPE_TEST = 1;
    public static int PARAM_TYPE_NORMAL = 10;


    public int type;
    public String name;

    public String prefix;
    public String suffix;

    public int begin;
    public int end;

    public String value;
}
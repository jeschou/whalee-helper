package cn.whale.helper.utils;

public class Holder<T> {
    private T obj;

    public Holder(T def) {
        setObj(def);
    }

    public Holder() {

    }

    public T getObj() {
        return obj;
    }

    public void setObj(T obj) {
        this.obj = obj;
    }
}

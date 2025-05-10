package Quadruple;

// 四元式
public class Quadruple {
    String op, arg1, arg2, result;

    Quadruple(String op, String arg1, String arg2, String result) {
        this.op = op;
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.result = result;
    }

    @Override
    public String toString() {
        return String.format("(%s %s %s %s)", op, arg1, arg2, result);
    }
}


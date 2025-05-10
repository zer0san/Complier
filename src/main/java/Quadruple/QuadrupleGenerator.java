package Quadruple;

import java.util.*;

public class QuadrupleGenerator {
    private int tempId = 0;
    private Map<String,String> cseCache = new HashMap<>(); // 公共子表达式消除
    private Map<String,String> valueMap = new HashMap<>(); // 代入传播
    private Set<String> usedTemps = new HashSet<>(); // 死代码检测
    List<Quadruple> quds = new ArrayList<>();

    // 创建临时变量
    String newTemp() {
        String temp = "t" + (tempId++);
        usedTemps.add(temp);
        return temp;
    }

    String generateExpr(Expr expr) {
        if (expr instanceof NumberExpr n) {
            return Integer.toString(n.value);
        } else if (expr instanceof VarExpr v) {
            return v.name;
        } else if (expr instanceof BinaryExpr b) {
            String arg1 = generateExpr(b.left);
            String arg2 = generateExpr(b.right);
            String result = newTemp();
            quds.add(new Quadruple(b.op, arg1, arg2, result));
            return result;
        }
        return null;
    }

    public void assign(String var,Expr expr){
        String value = generateExpr(expr);
        quds.add(new Quadruple("=",value,"_",var));
    }

    public void show(){
        for(var q : quds){
            System.out.println(q);
        }
    }
}

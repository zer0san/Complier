import com.Main;
import com.Rest.Result;

public class test_array {
    public static void main(String[] args) {
        // Test array declaration
        System.out.println("=== Test 1: Array Declaration ===");
        String code1 = "int a[10];";
        Result result1 = Main.Solve(code1);
        System.out.println("Input: " + code1);
        System.out.println("Result: " + (result1.isSuccess() ? "SUCCESS" : "FAILED"));
        if (result1.isSuccess()) {
            System.out.println("Output:\n" + result1.getRes());
        } else {
            System.out.println("Error: " + result1.getMsg());
        }

        // Test array assignment
        System.out.println("\n=== Test 2: Array Assignment ===");
        String code2 = "int a[5]; a[0] = 10;";
        Result result2 = Main.Solve(code2);
        System.out.println("Input: " + code2);
        System.out.println("Result: " + (result2.isSuccess() ? "SUCCESS" : "FAILED"));
        if (result2.isSuccess()) {
            System.out.println("Output:\n" + result2.getRes());
        } else {
            System.out.println("Error: " + result2.getMsg());
        }

        // Test array access in expression
        System.out.println("\n=== Test 3: Array Access in Expression ===");
        String code3 = "int a[5]; int b; a[0] = 10; b = a[0] + 5;";
        Result result3 = Main.Solve(code3);
        System.out.println("Input: " + code3);
        System.out.println("Result: " + (result3.isSuccess() ? "SUCCESS" : "FAILED"));
        if (result3.isSuccess()) {
            System.out.println("Output:\n" + result3.getRes());
        } else {
            System.out.println("Error: " + result3.getMsg());
        }

        // Test complex array operations
        System.out.println("\n=== Test 4: Complex Array Operations ===");
        String code4 = "int arr[3]; int i; i = 1; arr[i] = arr[0] * 2;";
        Result result4 = Main.Solve(code4);
        System.out.println("Input: " + code4);
        System.out.println("Result: " + (result4.isSuccess() ? "SUCCESS" : "FAILED"));
        if (result4.isSuccess()) {
            System.out.println("Output:\n" + result4.getRes());
        } else {
            System.out.println("Error: " + result4.getMsg());
        }
    }
}

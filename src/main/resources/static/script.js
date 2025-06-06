$(document).ready(function () {
    // 正确绑定 keydown 事件
    $("#ipt_area").on("keydown", function(e) {
        // 检查是否按下了 Tab 键
        if (e.key === 'Tab' || e.keyCode === 9) {
            // 阻止默认的 Tab 行为
            e.preventDefault();

            var indent = "    "; // 4 个空格作为缩进
            var start = this.selectionStart;
            var end = this.selectionEnd;
            var value = this.value;
            var selected = value.substring(start, end);

            if (start !== end) {
                // 如果选中了文本，缩进所有选中的行
                var newText = selected.replace(/^/gm, indent);
                this.value = value.substring(0, start) + newText + value.substring(end);
                this.selectionStart = start;
                this.selectionEnd = start + newText.length;
            } else {
                // 如果没有选中文本，只在光标位置插入缩进
                this.value = value.substring(0, start) + indent + value.substring(end);
                this.selectionStart = this.selectionEnd = start + indent.length;
            }
        }
    });

    $('#upload').click(function (e) {
        $.ajax({
            url: "http://localhost:8080/parse",
            type: "POST",
            data: JSON.stringify({sourceCode: $('#ipt_area').val()}),
            contentType: "application/json",
            processData: false,

            success: function (result) {
                console.log(result);
                const success = result.success;
                console.log("success = "+success)
                if (!success) {
                    var strings = result.msg?.toString().split("\n");
                    $("#output > *").css("color", "red");
                    $("#opt_area").text(strings.join("\n"));
                    $("#tokens").text("");
                    $("#asm").text("");
                } else {
                    var strings = result.res.toString().split("\n");
                    $("#output > *").css("color", "green");
                    $("#opt_area").text(strings.join("\n"));
                    $("#tokens").text(result.tokens);
                    $("#asm").text(result.asmCode);
                }
            },
            error: function (xhr, status, error) {
                $("#opt_area").css("color", "red");
                alert("failed" + error.toString());
            }
        });
    });
    function showTable(data){
        console.log(data)
    }
});

// @Getter
// private Map<String, Integer> keywordTable = new HashMap<>();
//
// @Getter
// private Map<String, Integer> identifierTable = new HashMap<>();
//
// @Getter
// private Map<String, Integer> constantTable = new HashMap<>();
//
// @Getter
// private Map<String, Integer> operatorTable = new HashMap<>();
// String tokens;
//
// @Getter
// private Map<String, Integer> separatorTable = new HashMap<>();
$(document).ready(function () {
    $('#upload').click(function (e) {

        const formData = new FormData();
        formData.append("sourceCode", $('#ipt_area').val());

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
                    $("#opt_area").css("color", "red");
                    $("#opt_area").text(strings.join("\n"));

                } else {
                    var strings = result.res.toString().split("\n");
                    $("#opt_area").css("color", "green");
                    $("#opt_area").text(strings.join("\n"));
                }
                // alert(data);
            },
            error: function (xhr, status, error) {
                $("#opt_area").style = "color: red;";
                // document.getElementById("opt_area").innerText = "解析失败，请检查输入代码是否正确！\n" + error.toString();
                alert("failed" + error.toString());
            }
        });

    });
});

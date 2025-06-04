$(document).ready(function () {
    $('#upload').click(function (e) {

        const formData = new FormData();
        formData.append("sourceCode", $('#ipt_area').val());

        $.ajax({
            url: "http://localhost:8080/parse",
            type: "POST",
            data: JSON.stringify({ sourceCode: $('#ipt_area').val() }),
            contentType: "application/json",
            processData: false,
            success: function (data) {
                $("#opt_area").innerText = data;
                // document.getElementById("opt_area").innerText = data;
                var strings = data.toString().split("\n");
                $("#opt_area").text(strings.join("\n"));
                console.log(data);
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

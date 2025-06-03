
$('#upload').click(function () {
    alert("click")
    const fileInput = $('#read_file')[0];
    if (fileInput.files.length === 0) {
        alert("请先选择文件！");
        return;
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    $.ajax({
        url:"http://172.18.149.11:8080/parse",
        type:"POST",
        data:formData,
        contentType:false,
        processData:false,
        success:function(data){
            // $("#opt_area").val(data);
            alert(data)
        },
        error:function (xhr, status, error) {
            alert("failed")
        }
    })
});

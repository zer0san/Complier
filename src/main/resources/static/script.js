$(document).ready(function () {
    // Tab key handling for code indentation
    $("#ipt_area").on("keydown", function (e) {
        if (e.key === 'Tab' || e.keyCode === 9) {
            e.preventDefault();

            var indent = "    "; // 4 spaces for indentation
            var start = this.selectionStart;
            var end = this.selectionEnd;
            var value = this.value;
            var selected = value.substring(start, end);

            if (start !== end) {
                // Indent all selected lines
                var newText = selected.replace(/^/gm, indent);
                this.value = value.substring(0, start) + newText + value.substring(end);
                this.selectionStart = start;
                this.selectionEnd = start + newText.length;
            } else {
                // Insert indentation at cursor position
                this.value = value.substring(0, start) + indent + value.substring(end);
                this.selectionStart = this.selectionEnd = start + indent.length;
            }
        }
    });

    // Compile button click handler
    $('#upload').click(function (e) {
        const sourceCode = $('#ipt_area').val().trim();

        if (!sourceCode) {
            alert('请输入代码后再编译');
            return;
        }

        // Show loading state
        $(this).text('编译中...').prop('disabled', true);

        $.ajax({
            url: "http://localhost:8080/parse",
            type: "POST",
            data: JSON.stringify({ sourceCode: sourceCode }),
            contentType: "application/json",
            processData: false,

            success: function (result) {
                console.log(result);
                const success = result.success;

                if (!success) {
                    // Show error
                    $("#opt_area").css("color", "#cc8080").text(result.msg || "编译失败");
                    $("#tokens").text("");
                    $("#asm").text("");
                    $("#symbol_table").text("");
                } else {
                    // Show success results
                    $("#opt_area").css("color", "#80cc80").text(result.res || "编译成功");
                    $("#tokens").text(result.tokens || "");
                    $("#asm").text(result.asmCode || "");
                    $("#symbol_table").text(result.symbolTable || "");
                }
            },
            error: function (xhr, status, error) {
                console.error('Ajax error:', error);
                $("#opt_area").css("color", "red").text("网络错误：" + error);
                $("#tokens").text("");
                $("#asm").text("");
                $("#symbol_table").text("");
            },
            complete: function () {
                // Reset button state
                $('#upload').text('编译运行').prop('disabled', false);
            }
        });
    });

    // File upload handling
    document.getElementById('read_file').addEventListener('change', function (event) {
        const file = event.target.files[0];
        if (file) {
            handleFileUpload(file);
        }
    });

    async function handleFileUpload(file) {
        try {
            const content = await readFileAsText(file);
            document.getElementById('ipt_area').value = content;
            console.log('File loaded successfully');
        } catch (error) {
            console.error('Error reading file:', error);
            alert('文件读取失败：' + error.message);
        }
    }

    function readFileAsText(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = event => resolve(event.target.result);
            reader.onerror = error => reject(error);
            reader.readAsText(file, 'UTF-8');
        });
    }

    // Output section toggle functionality
    $('.output-header').click(function () {
        const targetId = $(this).data('target');
        const targetTextarea = $('#' + targetId);
        const toggleBtn = $(this).find('.toggle-btn');

        if (targetTextarea.hasClass('collapsed')) {
            // Expand
            targetTextarea.removeClass('collapsed');
            toggleBtn.removeClass('collapsed').text('▼');
        } else {
            // Collapse
            targetTextarea.addClass('collapsed');
            toggleBtn.addClass('collapsed').text('▶');
        }
    });

    // Double-click to expand/maximize a section
    $('.output-textarea').dblclick(function () {
        const textarea = $(this);
        const toggleBtn = textarea.siblings('.output-header').find('.toggle-btn');

        if (textarea.hasClass('expanded')) {
            // Return to normal size
            textarea.removeClass('expanded');
        } else {
            // First ensure it's not collapsed
            if (textarea.hasClass('collapsed')) {
                textarea.removeClass('collapsed');
                toggleBtn.removeClass('collapsed').text('▼');
            }
            // Then expand to large size
            textarea.addClass('expanded');
        }
    });

    // Keyboard shortcuts for output sections
    $(document).keydown(function (e) {
        // Ctrl + 1, 2, 3, 4 to toggle sections
        if (e.ctrlKey) {
            let targetId = '';
            switch (e.which) {
                case 49: // Ctrl + 1
                    targetId = 'opt_area';
                    break;
                case 50: // Ctrl + 2
                    targetId = 'asm';
                    break;
                case 51: // Ctrl + 3
                    targetId = 'tokens';
                    break;
                case 52: // Ctrl + 4
                    targetId = 'symbol_table';
                    break;
            }

            if (targetId) {
                e.preventDefault();
                $(`[data-target="${targetId}"]`).click();
            }
        }
    });
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
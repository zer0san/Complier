window.onload = function () {
    const container = document.querySelector("#container");
    const divLeft = document.getElementById('divLeft');
    const divRight = document.getElementById('divRight');
    const divSplitter = document.querySelector(".spilter");

    let isResizing = false;
    let startX = 0;
    let startLeftWidth = 0;
    let isLeftHidden = false;
    let lastLeftWidth = '50%'; // Store the last width before hiding

    // Initialize splitter functionality
    function initSplitter() {
        divSplitter.addEventListener('mousedown', startResize);
        divSplitter.addEventListener('dblclick', toggleLeftPanel);

        // Prevent text selection during resize
        document.addEventListener('selectstart', preventSelection);
    }

    function startResize(e) {
        if (isLeftHidden) return; // Don't allow resize when left panel is hidden

        isResizing = true;
        startX = e.clientX;
        startLeftWidth = divLeft.offsetWidth;

        document.addEventListener('mousemove', handleResize);
        document.addEventListener('mouseup', stopResize);

        // Add visual feedback
        document.body.style.cursor = 'col-resize';
        divSplitter.style.background = 'linear-gradient(to right, #999, #777, #999)';

        e.preventDefault();
    }

    function handleResize(e) {
        if (!isResizing || isLeftHidden) return;

        const deltaX = e.clientX - startX;
        const containerWidth = container.offsetWidth;
        const splitterWidth = divSplitter.offsetWidth;

        let newLeftWidth = startLeftWidth + deltaX;

        // Set minimum and maximum widths
        const minWidth = 200;
        const maxWidth = containerWidth - minWidth - splitterWidth;

        newLeftWidth = Math.max(minWidth, Math.min(newLeftWidth, maxWidth));

        // Calculate percentage for flex-basis
        const leftPercentage = (newLeftWidth / containerWidth) * 100;
        const rightPercentage = ((containerWidth - newLeftWidth - splitterWidth) / containerWidth) * 100;

        // Apply new widths
        divLeft.style.flexBasis = leftPercentage + '%';
        divRight.style.flexBasis = rightPercentage + '%';

        // Store the current width
        lastLeftWidth = leftPercentage + '%';

        e.preventDefault();
    }

    function stopResize() {
        if (!isResizing) return;

        isResizing = false;

        document.removeEventListener('mousemove', handleResize);
        document.removeEventListener('mouseup', stopResize);

        // Remove visual feedback
        document.body.style.cursor = '';
        divSplitter.style.background = '';
    }

    function toggleLeftPanel(e) {
        e.preventDefault();
        e.stopPropagation();

        if (isLeftHidden) {
            // Show left panel
            divLeft.style.display = 'flex';
            divLeft.style.flexBasis = lastLeftWidth;
            divRight.style.flexBasis = `calc(100% - ${lastLeftWidth} - 8px)`;
            divSplitter.style.cursor = 'col-resize';
            divSplitter.title = '拖动调整大小，双击隐藏左侧面板';
            isLeftHidden = false;
        } else {
            // Hide left panel
            // Store current width before hiding
            const currentWidth = divLeft.offsetWidth;
            const containerWidth = container.offsetWidth;
            lastLeftWidth = ((currentWidth / containerWidth) * 100) + '%';

            divLeft.style.display = 'none';
            divLeft.style.flexBasis = '0';
            divRight.style.flexBasis = 'calc(100% - 8px)';
            divSplitter.style.cursor = 'pointer';
            divSplitter.title = '双击显示左侧面板';
            isLeftHidden = true;
        }
    }

    function preventSelection(e) {
        if (isResizing) {
            e.preventDefault();
            return false;
        }
    }

    // Initialize when DOM is ready
    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSplitter);
    } else {
        initSplitter();
    }
};
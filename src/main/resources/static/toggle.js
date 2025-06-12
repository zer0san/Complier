// Advanced toggle functionality for output sections

class OutputToggleManager {
    constructor() {
        this.sections = [
            'opt_area',
            'asm',
            'tokens',
            'symbol_table',
            'keyword_table',
            'identifier_table',
            'constant_table',
            'operator_table',
            'separator_table'
        ];
        this.collapsedSections = new Set();
        this.expandedSections = new Set();
        this.init();
    }

    init() {
        this.bindEvents();
        this.createToggleAllButton();
    }

    bindEvents() {
        // Individual section toggles

        $('.output-header').on('click', (e) => {
            const targetId = $(e.currentTarget).data('target');
            this.toggleSection(targetId);
        });

        // Double-click to expand
        $('.output-textarea').on('dblclick', (e) => {
            const targetId = e.target.id;
            this.expandSection(targetId);
        });

        // Handle textarea resize events
        $('.output-textarea, #ipt_area').on('mouseup', (e) => {
            // Force a repaint after resize
            setTimeout(() => {
                $(e.target).trigger('resize');
                // Ensure scrollable content is accessible
                this.ensureScrollable();
            }, 10);
        });

        // Keyboard shortcuts
        $(document).on('keydown', (e) => {
            if (e.ctrlKey && e.shiftKey) {
                switch (e.which) {
                    case 65: // Ctrl+Shift+A - Toggle All
                        e.preventDefault();
                        this.toggleAllSections();
                        break;
                    case 69: // Ctrl+Shift+E - Expand All
                        e.preventDefault();
                        this.expandAllSections();
                        break;
                    case 67: // Ctrl+Shift+C - Collapse All
                        e.preventDefault();
                        this.collapseAllSections();
                        break;
                }
            }
        });
    }

    toggleSection(targetId) {
        const textarea = $(`#${targetId}`);
        const toggleBtn = $(`[data-target="${targetId}"] .toggle-btn`);

        if (this.collapsedSections.has(targetId)) {
            this.showSection(targetId);
        } else {
            this.hideSection(targetId);
        }
    }

    ensureScrollable() {
        // Check if content exceeds viewport and adjust container if needed
        const container = $('#container');
        const totalHeight = container[0].scrollHeight;
        const viewportHeight = window.innerHeight;

        if (totalHeight > viewportHeight) {
            container.css('min-height', totalHeight + 'px');
        }
    }

    showSection(targetId) {
        const textarea = $(`#${targetId}`);
        const toggleBtn = $(`[data-target="${targetId}"] .toggle-btn`);

        textarea.removeClass('collapsed');
        textarea.css('resize', 'vertical');
        toggleBtn.removeClass('collapsed').text('▼');
        this.collapsedSections.delete(targetId);

        // Ensure page is scrollable after showing section
        setTimeout(() => this.ensureScrollable(), 100);
    }

    hideSection(targetId) {
        const textarea = $(`#${targetId}`);
        const toggleBtn = $(`[data-target="${targetId}"] .toggle-btn`);

        textarea.addClass('collapsed');
        textarea.css('resize', 'none');
        toggleBtn.addClass('collapsed').text('▶');
        this.collapsedSections.add(targetId);
        this.expandedSections.delete(targetId);
    }

    expandSection(targetId) {
        const textarea = $(`#${targetId}`);

        // First ensure it's visible
        if (this.collapsedSections.has(targetId)) {
            this.showSection(targetId);
        }

        // Toggle expanded state
        if (this.expandedSections.has(targetId)) {
            textarea.removeClass('expanded');
            this.expandedSections.delete(targetId);
        } else {
            textarea.addClass('expanded');
            this.expandedSections.add(targetId);
        }
    }

    toggleAllSections() {
        const allCollapsed = this.sections.every(id => this.collapsedSections.has(id));

        if (allCollapsed) {
            this.expandAllSections();
        } else {
            this.collapseAllSections();
        }
    }

    collapseAllSections() {
        this.sections.forEach(id => {
            if (!this.collapsedSections.has(id)) {
                this.hideSection(id);
            }
        });
    }

    expandAllSections() {
        this.sections.forEach(id => {
            if (this.collapsedSections.has(id)) {
                this.showSection(id);
            }
        });
    }

    createToggleAllButton() {
        const toggleAllBtn = $(`
            <div class="toggle-all-container" style="margin-bottom: 10px; text-align: right;">
                <button id="toggleAllBtn" class="toggle-all-btn" title="Ctrl+Shift+A: 全部折叠/展开">
                    <span>⚏</span> 全部
                </button>
            </div>
        `);

        $('#opt_title').after(toggleAllBtn);

        $('#toggleAllBtn').on('click', () => {
            this.toggleAllSections();
        });
    }

    // Method to apply terminal-style text coloring
    applyTerminalColors(element, isError = false) {
        if (isError) {
            element.removeClass('success-text').addClass('error-text');
        } else {
            element.removeClass('error-text').addClass('success-text');
        }
    }
}

// Initialize when document is ready
$(document).ready(function () {
    window.outputToggleManager = new OutputToggleManager();
});

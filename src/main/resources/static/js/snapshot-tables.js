(function () {
    'use strict';

    function initSnapshotTables() {
        document.querySelectorAll('[data-enhanced-table]').forEach(function (table) {
            bindSort(table);
            bindFilter(table);
            updateCount(table);
        });
    }

    function bindSort(table) {
        table.querySelectorAll('th[data-sort]').forEach(function (header, columnIndex) {
            const button = header.querySelector('.table-sort-control') || header;
            button.addEventListener('click', function () {
                sortTable(table, header, columnIndex);
            });
            button.addEventListener('keydown', function (event) {
                if (event.key === 'Enter' || event.key === ' ') {
                    event.preventDefault();
                    button.click();
                }
            });
        });
    }

    function bindFilter(table) {
        const selector = '#' + table.id;
        const input = document.querySelector('[data-table-filter="' + selector + '"]');
        const clearButton = document.querySelector('[data-table-clear="' + selector + '"]');

        if (input) {
            input.addEventListener('input', function () {
                filterTable(table, input.value);
            });
        }

        if (clearButton && input) {
            clearButton.addEventListener('click', function () {
                input.value = '';
                filterTable(table, '');
                input.focus();
            });
        }
    }

    function sortTable(table, activeHeader, columnIndex) {
        const tbody = table.querySelector('tbody');
        if (!tbody) return;

        const sortType = activeHeader.dataset.sort || 'text';
        const currentDirection = activeHeader.dataset.sortDirection === 'asc' ? 'asc' : 'desc';
        const nextDirection = currentDirection === 'asc' ? 'desc' : 'asc';

        Array.from(tbody.querySelectorAll('tr'))
            .sort(function (left, right) {
                const leftValue = sortableValue(left.children[columnIndex], sortType);
                const rightValue = sortableValue(right.children[columnIndex], sortType);
                const comparison = compare(leftValue, rightValue, sortType);
                return nextDirection === 'asc' ? comparison : -comparison;
            })
            .forEach(function (row) {
                tbody.appendChild(row);
            });

        table.querySelectorAll('th[data-sort]').forEach(function (header) {
            header.removeAttribute('data-sort-direction');
            header.setAttribute('aria-sort', 'none');
            header.classList.remove('sort-asc', 'sort-desc');
            const icon = header.querySelector('.table-sort-icon');
            if (icon) {
                icon.className = 'bi bi-arrow-down-up ms-1 table-sort-icon';
            }
        });

        activeHeader.dataset.sortDirection = nextDirection;
        activeHeader.setAttribute('aria-sort', nextDirection === 'asc' ? 'ascending' : 'descending');
        activeHeader.classList.add(nextDirection === 'asc' ? 'sort-asc' : 'sort-desc');
        const icon = activeHeader.querySelector('.table-sort-icon');
        if (icon) {
            icon.className = 'bi ms-1 table-sort-icon ' + (nextDirection === 'asc' ? 'bi-sort-up' : 'bi-sort-down');
        }

        updateCount(table);
    }

    function filterTable(table, query) {
        const normalizedQuery = normalize(query);
        table.querySelectorAll('tbody tr').forEach(function (row) {
            const text = normalize(row.textContent || '');
            row.hidden = normalizedQuery.length > 0 && !text.includes(normalizedQuery);
        });
        updateCount(table);
    }

    function updateCount(table) {
        const selector = '#' + table.id;
        const countElement = document.querySelector('[data-table-count="' + selector + '"]');
        if (!countElement) return;

        const rows = Array.from(table.querySelectorAll('tbody tr'));
        const visibleRows = rows.filter(function (row) {
            return !row.hidden;
        });
        countElement.textContent = visibleRows.length + ' / ' + rows.length + ' visibles';
    }

    function sortableValue(cell, sortType) {
        if (!cell) return sortType === 'text' ? '' : 0;

        const rawValue = cell.dataset.sortValue || cell.textContent || '';
        if (sortType === 'number') {
            return Number(String(rawValue).replace(/[^0-9.-]/g, '')) || 0;
        }
        if (sortType === 'date') {
            return Date.parse(rawValue) || 0;
        }
        return normalize(rawValue);
    }

    function compare(leftValue, rightValue, sortType) {
        if (sortType === 'text') {
            return leftValue.localeCompare(rightValue);
        }
        return leftValue - rightValue;
    }

    function normalize(value) {
        return String(value)
            .toLowerCase()
            .normalize('NFD')
            .replace(/[\u0300-\u036f]/g, '')
            .trim();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initSnapshotTables);
    } else {
        initSnapshotTables();
    }
})();

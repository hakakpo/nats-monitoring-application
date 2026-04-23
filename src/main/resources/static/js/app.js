// NATS Monitor - Frontend Application
(function() {
    'use strict';

    // --- WebSocket Connection ---
    let stompClient = null;
    let messageRateChart = null;
    let byteRateChart = null;
    let reconnectTimer = null;
    let isConnecting = false;
    const THEME_KEY = 'theme';

    function connectWebSocket() {
        if (isConnecting) return;
        if (typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            console.warn('SockJS or Stomp is not available, realtime updates disabled');
            return;
        }

        if (stompClient && stompClient.connected) {
            return;
        }

        isConnecting = true;
        const socket = new SockJS('/ws');
        stompClient = Stomp.over(socket);
        stompClient.debug = null; // Disable debug logs

        stompClient.connect({}, function() {
            isConnecting = false;
            clearReconnectTimer();
            console.log('WebSocket connected');
            stompClient.subscribe('/topic/metrics', function(message) {
                const data = JSON.parse(message.body);
                updateDashboard(data);
            });
        }, function(error) {
            isConnecting = false;
            console.log('WebSocket error, retrying in 5s...', error);
            setDisconnectedState();
            scheduleReconnect();
        });
    }

    function updateDashboard(data) {
        // Update connection status
        const statusDot = document.querySelector('.status-dot');
        const statusText = document.querySelector('.connection-status span:last-child');
        if (statusDot && statusText) {
            statusDot.className = 'status-dot ' + (data.connected ? 'connected' : 'disconnected');
            statusText.textContent = data.connected ? 'Connected' : 'Disconnected';
        }

        if (!data.connected) {
            setDisconnectedState();
            return;
        }

        // Update metric values
        updateElement('metricConnections', data.connections);
        updateElement('metricInMsgs', formatNumber(data.inMsgs));
        updateElement('metricOutMsgs', formatNumber(data.outMsgs));
        updateElement('metricSlowConsumers', data.slowConsumers);
        updateElement('metricCpu', (data.cpu || 0).toFixed(1));
        updateElement('metricMem', data.memFormatted);
        updateElement('metricSubs', data.subscriptions);

        // Update charts
        if (data.messageRateHistory) {
            updateChart(messageRateChart, data.messageRateHistory);
        }
        if (data.byteRateHistory) {
            updateChart(byteRateChart, data.byteRateHistory);
        }
    }

    function setDisconnectedState() {
        updateElement('metricConnections', '0');
        updateElement('metricInMsgs', '0');
        updateElement('metricOutMsgs', '0');
        updateElement('metricSlowConsumers', '0');
        updateElement('metricCpu', '0.0');
        updateElement('metricMem', '0 B');
        updateElement('metricSubs', '0');
        resetChart(messageRateChart);
        resetChart(byteRateChart);
    }

    function updateElement(id, value) {
        const el = document.getElementById(id);
        if (el) el.textContent = value;
    }

    function formatNumber(num) {
        if (num === undefined || num === null) return '0';
        return num.toLocaleString();
    }

    function readErrorMessage(response) {
        return response.text().then(function(text) {
            if (!text) {
                return response.statusText || 'Unknown error';
            }

            try {
                const payload = JSON.parse(text);
                return payload.message || text;
            } catch (_) {
                return text;
            }
        });
    }

    function getCssVar(name, fallback) {
        const value = getComputedStyle(document.documentElement).getPropertyValue(name);
        return value ? value.trim() : fallback;
    }

    function getActiveTheme() {
        return document.documentElement.getAttribute('data-theme') === 'light' ? 'light' : 'dark';
    }

    function applyTheme(theme) {
        const nextTheme = theme === 'light' ? 'light' : 'dark';
        document.documentElement.setAttribute('data-theme', nextTheme);
        localStorage.setItem(THEME_KEY, nextTheme);
        updateChartsTheme();
    }

    function initThemeToggle() {
        const toggle = document.getElementById('themeToggle');
        if (!toggle) return;

        toggle.checked = getActiveTheme() === 'light';
        toggle.addEventListener('change', function() {
            applyTheme(toggle.checked ? 'light' : 'dark');
        });
    }

    // --- Chart Initialization ---
    function initCharts() {
        const msgCtx = document.getElementById('messageRateChart');
        const byteCtx = document.getElementById('byteRateChart');

        const chartOptions = createChartOptions();

        if (msgCtx) {
            messageRateChart = new Chart(msgCtx, {
                type: 'line',
                data: {
                    labels: generateLabels(60),
                    datasets: [
                        {
                            label: 'Messages In/s',
                            data: [],
                            borderColor: '#22c55e',
                            backgroundColor: 'rgba(34, 197, 94, 0.1)',
                            fill: true
                        },
                        {
                            label: 'Messages Out/s',
                            data: [],
                            borderColor: '#3b82f6',
                            backgroundColor: 'rgba(59, 130, 246, 0.1)',
                            fill: true
                        }
                    ]
                },
                options: chartOptions
            });
        }

        if (byteCtx) {
            byteRateChart = new Chart(byteCtx, {
                type: 'line',
                data: {
                    labels: generateLabels(60),
                    datasets: [
                        {
                            label: 'Bytes In/s',
                            data: [],
                            borderColor: '#f59e0b',
                            backgroundColor: 'rgba(245, 158, 11, 0.1)',
                            fill: true
                        },
                        {
                            label: 'Bytes Out/s',
                            data: [],
                            borderColor: '#ef4444',
                            backgroundColor: 'rgba(239, 68, 68, 0.1)',
                            fill: true
                        }
                    ]
                },
                options: chartOptions
            });
        }
    }

    function createChartOptions() {
        const tickColor = getCssVar('--text-muted', '#94a3b8');
        const legendColor = getCssVar('--text', '#e2e8f0');
        const gridColor = getCssVar('--border-soft', 'rgba(51, 65, 85, 0.5)');

        return {
            responsive: true,
            maintainAspectRatio: false,
            animation: { duration: 300 },
            scales: {
                x: {
                    display: false,
                    grid: { display: false }
                },
                y: {
                    beginAtZero: true,
                    grid: { color: gridColor },
                    ticks: { color: tickColor, font: { size: 11 } }
                }
            },
            plugins: {
                legend: {
                    labels: { color: legendColor, usePointStyle: true, padding: 15 }
                }
            },
            elements: {
                line: { tension: 0.3, borderWidth: 2 },
                point: { radius: 0, hitRadius: 10 }
            }
        };
    }

    function updateChartTheme(chart) {
        if (!chart) return;
        const options = createChartOptions();
        chart.options.scales.y.grid.color = options.scales.y.grid.color;
        chart.options.scales.y.ticks.color = options.scales.y.ticks.color;
        chart.options.plugins.legend.labels.color = options.plugins.legend.labels.color;
        chart.update('none');
    }

    function updateChartsTheme() {
        updateChartTheme(messageRateChart);
        updateChartTheme(byteRateChart);
    }

    function generateLabels(count) {
        return Array.from({length: count}, (_, i) => '');
    }

    function updateChart(chart, historyData) {
        if (!chart) return;
        const inData = historyData.inRate || [];
        const outData = historyData.outRate || [];
        chart.data.labels = generateLabels(Math.max(inData.length, outData.length, 10));
        chart.data.datasets[0].data = inData;
        chart.data.datasets[1].data = outData;
        chart.update('none');
    }

    function resetChart(chart) {
        if (!chart) return;
        chart.data.labels = generateLabels(10);
        chart.data.datasets[0].data = [];
        chart.data.datasets[1].data = [];
        chart.update('none');
    }

    function clearReconnectTimer() {
        if (reconnectTimer) {
            clearTimeout(reconnectTimer);
            reconnectTimer = null;
        }
    }

    function scheduleReconnect() {
        if (reconnectTimer) return;
        reconnectTimer = setTimeout(function() {
            reconnectTimer = null;
            connectWebSocket();
        }, 5000);
    }

    // --- Alert Rule Functions ---
    window.createRule = function() {
        const rule = {
            name: document.getElementById('ruleName').value,
            type: document.getElementById('ruleType').value,
            streamName: document.getElementById('ruleStream').value || null,
            threshold: parseInt(document.getElementById('ruleThreshold').value),
            emailRecipient: document.getElementById('ruleEmail').value,
            emailEnabled: document.getElementById('ruleEmailEnabled').checked,
            cooldownMinutes: parseInt(document.getElementById('ruleCooldown').value) || 15,
            enabled: true
        };

        fetch('/api/alerts/rules', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(rule)
        }).then(res => {
            if (res.ok) {
                location.reload();
            } else {
                readErrorMessage(res).then(message => alert('Error: ' + message));
            }
        }).catch(err => alert('Error: ' + err.message));
    };

    window.toggleRule = function(id) {
        fetch('/api/alerts/rules/' + id + '/toggle', { method: 'POST' })
            .then(res => {
                if (res.ok) {
                    location.reload();
                } else {
                    readErrorMessage(res).then(message => alert('Error: ' + message));
                }
            })
            .catch(err => alert('Error: ' + err.message));
    };

    window.toggleEmailDelivery = function(id) {
        fetch('/api/alerts/rules/' + id + '/toggle-email', { method: 'POST' })
            .then(res => {
                if (res.ok) {
                    location.reload();
                } else {
                    readErrorMessage(res).then(message => alert('Error: ' + message));
                }
            })
            .catch(err => alert('Error: ' + err.message));
    };

    window.deleteRule = function(id) {
        if (confirm('Are you sure you want to delete this alert rule?')) {
            fetch('/api/alerts/rules/' + id, { method: 'DELETE' })
                .then(res => {
                    if (res.ok) {
                        location.reload();
                    } else {
                        readErrorMessage(res).then(message => alert('Error: ' + message));
                    }
                })
                .catch(err => alert('Error: ' + err.message));
        }
    };

    // --- Initialize ---
    document.addEventListener('DOMContentLoaded', function() {
        initThemeToggle();
        initCharts();
        connectWebSocket();
    });

    window.addEventListener('beforeunload', function() {
        clearReconnectTimer();
        if (stompClient && stompClient.connected) {
            stompClient.disconnect();
        }
    });
})();

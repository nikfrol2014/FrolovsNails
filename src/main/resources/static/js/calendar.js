const CalendarApp = {
    currentDate: new Date().toISOString().split('T')[0],
    appointments: [],
    availableDay: null,
    blocks: [],

    init: function() {
        this.checkAuth();
        this.setupEventListeners();
    },

    checkAuth: function() {
        const token = localStorage.getItem('token');
        if (!token) {
            this.showLoginForm();
        } else {
            this.loadData();
        }
    },

    showLoginForm: function() {
        const app = document.getElementById('app');
        app.innerHTML = `
            <div id="login-form">
                <h2>🔐 Вход в систему</h2>
                <input type="text" id="phone" placeholder="Телефон" value="+79161234567">
                <input type="password" id="password" placeholder="Пароль" value="password123">
                <button onclick="CalendarApp.login()">Войти</button>
                <div id="login-error" class="error-message"></div>
            </div>
        `;
    },

    login: function() {
        const phone = document.getElementById('phone').value;
        const password = document.getElementById('password').value;
        const errorDiv = document.getElementById('login-error');

        fetch('/api/auth/login', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ phone, password })
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    localStorage.setItem('token', data.data.accessToken);
                    localStorage.setItem('user', JSON.stringify({
                        phone: data.data.phone,
                        role: data.data.role,
                        firstName: data.data.firstName
                    }));
                    this.loadData();
                } else {
                    errorDiv.textContent = 'Ошибка входа: ' + data.message;
                }
            })
            .catch(error => {
                errorDiv.textContent = 'Ошибка соединения: ' + error.message;
            });
    },

    logout: function() {
        localStorage.removeItem('token');
        localStorage.removeItem('user');
        this.showLoginForm();
    },

    loadData: function() {
        this.showLoading();
        Promise.all([
            this.loadSchedule(),
            this.loadAppointments(),
            this.loadBlocks()
        ]).then(() => {
            this.render();
        }).catch(error => {
            if (error.status === 401 || error.status === 403) {
                this.logout();
            } else {
                this.showError(error.message);
            }
        });
    },

    showLoading: function() {
        document.getElementById('app').innerHTML = '<div class="loading">⏳ Загрузка...</div>';
    },

    showError: function(message) {
        document.getElementById('app').innerHTML = `<div class="error">❌ Ошибка: ${message}</div>`;
    },

    loadSchedule: function() {
        return fetch(`/api/schedule/admin/available-days?startDate=${this.currentDate}&endDate=${this.currentDate}`, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => {
                if (!response.ok) throw { status: response.status, message: 'Ошибка загрузки расписания' };
                return response.json();
            })
            .then(data => {
                if (data.data && data.data.days && data.data.days.length > 0) {
                    this.availableDay = data.data.days[0];
                } else {
                    this.availableDay = null;
                }
            });
    },

    loadAppointments: function() {
        return fetch(`/api/appointments?date=${this.currentDate}`, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => {
                if (!response.ok) throw { status: response.status, message: 'Ошибка загрузки записей' };
                return response.json();
            })
            .then(data => {
                this.appointments = data.data.appointments || [];
            });
    },

    loadBlocks: function() {
        return fetch(`/api/schedule/blocks?startDate=${this.currentDate}&endDate=${this.currentDate}`, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => {
                if (!response.ok) throw { status: response.status, message: 'Ошибка загрузки блокировок' };
                return response.json();
            })
            .then(data => {
                this.blocks = data.data.blocks || [];
            });
    },

    render: function() {
        const app = document.getElementById('app');
        const user = JSON.parse(localStorage.getItem('user') || '{}');

        let html = `
            <div class="header">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <h1>📅 Расписание мастера</h1>
                    <div style="display: flex; gap: 20px; align-items: center;">
                        <span>👤 ${user.firstName || 'Мастер'} (${user.role || 'ADMIN'})</span>
                        <button onclick="CalendarApp.logout()" style="padding: 5px 10px;">Выйти</button>
                    </div>
                </div>
                
                <div class="date-nav">
                    <button onclick="CalendarApp.prevDay()">← Вчера</button>
                    <span id="currentDate">${this.formatDate(this.currentDate)}</span>
                    <button onclick="CalendarApp.nextDay()">Завтра →</button>
                    <button onclick="CalendarApp.today()">Сегодня</button>
                </div>
            </div>
        `;

        if (!this.availableDay) {
            html += `
                <div class="timeline-container">
                    <div style="text-align: center; padding: 50px; color: #6c757d;">
                        ❌ Нет доступного времени на этот день<br>
                        <button onclick="CalendarApp.addAvailableDay()" style="margin-top: 20px; padding: 10px 20px;">
                            + Добавить рабочий день
                        </button>
                    </div>
                </div>
            `;
            app.innerHTML = html;
            return;
        }

        const workStart = this.parseTime(this.availableDay.workStart);
        const workEnd = this.parseTime(this.availableDay.workEnd);
        const totalHours = workEnd.hour - workStart.hour;

        html += `
            <div class="timeline-container">
                <div class="timeline" style="position: relative; height: 60px;">
                    ${this.renderHourMarkers(workStart.hour, workEnd.hour)}
                </div>
                
                <div class="appointments-container" style="position: relative; min-height: 300px;">
                    ${this.renderAppointments()}
                    ${this.renderBlocks()}
                </div>
                
                <div class="legend">
                    <div class="legend-item">
                        <div class="legend-color" style="background: #28a745;"></div>
                        <span>Подтверждено</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #ffc107;"></div>
                        <span>Ожидание</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #007bff;"></div>
                        <span>Выполнено</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: #6c757d;"></div>
                        <span>Отменено</span>
                    </div>
                    <div class="legend-item">
                        <div class="legend-color" style="background: rgba(220,53,69,0.3); border: 2px solid #dc3545;"></div>
                        <span>Заблокировано</span>
                    </div>
                </div>
                
                ${this.renderStats()}
            </div>
        `;

        app.innerHTML = html;
    },

    renderHourMarkers: function(startHour, endHour) {
        let markers = '';
        for (let hour = startHour; hour <= endHour; hour++) {
            const left = (hour - startHour) * 60;
            markers += `
                <div class="hour-marker" style="left: ${left}px;">
                    ${hour}:00
                </div>
            `;
        }
        return markers;
    },

    renderAppointments: function() {
        if (!this.appointments.length) return '';

        const workStart = this.parseTime(this.availableDay.workStart);

        return this.appointments.map(apt => {
            const start = this.parseDateTime(apt.startTime);
            const end = this.parseDateTime(apt.endTime);

            const startMinutes = (start.hour - workStart.hour) * 60 + start.minute;
            const duration = (end.hour - start.hour) * 60 + (end.minute - start.minute);

            const statusClass = this.getStatusClass(apt.status);
            const statusText = this.getStatusText(apt.status);

            return `
                <div class="appointment ${statusClass}" 
                     style="left: ${startMinutes}px; width: ${duration}px;"
                     onclick="CalendarApp.showAppointmentDetails(${JSON.stringify(apt).replace(/"/g, '&quot;')})"
                     title="${apt.client.firstName} ${apt.client.lastName || ''} - ${apt.service.name}">
                    <strong>${apt.client.firstName}</strong><br>
                    ${apt.service.name}<br>
                    <small>${statusText}</small>
                </div>
            `;
        }).join('');
    },

    renderBlocks: function() {
        if (!this.blocks.length) return '';

        const workStart = this.parseTime(this.availableDay.workStart);

        return this.blocks.filter(block => block.blocked).map(block => {
            const start = this.parseDateTime(block.startTime);
            const end = this.parseDateTime(block.endTime);

            const startMinutes = (start.hour - workStart.hour) * 60 + start.minute;
            const duration = (end.hour - start.hour) * 60 + (end.minute - start.minute);

            return `
                <div class="blocked" 
                     style="left: ${startMinutes}px; width: ${duration}px;"
                     title="Заблокировано: ${block.reason || 'нет причины'}">
                    🚫 ${block.reason || 'Занято'}<br>
                    <small>${block.notes || ''}</small>
                </div>
            `;
        }).join('');
    },

    renderStats: function() {
        const total = this.appointments.length;
        const confirmed = this.appointments.filter(a => a.status === 'CONFIRMED').length;
        const pending = this.appointments.filter(a => a.status === 'PENDING' || a.status === 'CREATED').length;
        const cancelled = this.appointments.filter(a => a.status === 'CANCELLED').length;
        const completed = this.appointments.filter(a => a.status === 'COMPLETED').length;

        return `
            <div class="stats">
                <div class="stat-item">
                    <div class="stat-label">Всего записей</div>
                    <div class="stat-value">${total}</div>
                </div>
                <div class="stat-item">
                    <div class="stat-label">Подтверждено</div>
                    <div class="stat-value" style="color: #28a745;">${confirmed}</div>
                </div>
                <div class="stat-item">
                    <div class="stat-label">Ожидание</div>
                    <div class="stat-value" style="color: #ffc107;">${pending}</div>
                </div>
                <div class="stat-item">
                    <div class="stat-label">Выполнено</div>
                    <div class="stat-value" style="color: #007bff;">${completed}</div>
                </div>
                <div class="stat-item">
                    <div class="stat-label">Отменено</div>
                    <div class="stat-value" style="color: #6c757d;">${cancelled}</div>
                </div>
            </div>
        `;
    },

    showAppointmentDetails: function(appointment) {
        const details = `
            📅 Запись #${appointment.id}\n
            👤 Клиент: ${appointment.client.firstName} ${appointment.client.lastName || ''}\n
            📞 Телефон: ${appointment.client.phone}\n
            💇 Услуга: ${appointment.service.name}\n
            ⏱ Длительность: ${appointment.service.durationMinutes} мин\n
            💰 Цена: ${appointment.service.price} руб\n
            🕐 Время: ${appointment.startTime} - ${appointment.endTime}\n
            📊 Статус: ${appointment.status}\n
            📝 Заметки: ${appointment.clientNotes || 'нет'}
        `;
        alert(details);
    },

    addAvailableDay: function() {
        const date = this.currentDate;
        const workStart = prompt('Введите время начала (например, 10:00)', '10:00');
        if (!workStart) return;

        const workEnd = prompt('Введите время окончания (например, 19:00)', '19:00');
        if (!workEnd) return;

        fetch(`/api/schedule/available-days?date=${date}&workStart=${workStart}&workEnd=${workEnd}`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('✅ Рабочий день добавлен');
                    this.loadData();
                } else {
                    alert('❌ Ошибка: ' + data.message);
                }
            });
    },

    prevDay: function() {
        const date = new Date(this.currentDate);
        date.setDate(date.getDate() - 1);
        this.currentDate = date.toISOString().split('T')[0];
        this.loadData();
    },

    nextDay: function() {
        const date = new Date(this.currentDate);
        date.setDate(date.getDate() + 1);
        this.currentDate = date.toISOString().split('T')[0];
        this.loadData();
    },

    today: function() {
        this.currentDate = new Date().toISOString().split('T')[0];
        this.loadData();
    },

    formatDate: function(dateStr) {
        const [year, month, day] = dateStr.split('-');
        return `${day}.${month}.${year}`;
    },

    parseTime: function(timeStr) {
        const [hour, minute] = timeStr.split(':').map(Number);
        return { hour, minute };
    },

    parseDateTime: function(dateTimeStr) {
        // Формат: "2026-02-18 12:00:00" или "18.02.2026 12:00"
        let timePart;
        if (dateTimeStr.includes('T')) {
            timePart = dateTimeStr.split('T')[1];
        } else {
            timePart = dateTimeStr.split(' ')[1];
        }
        const [hour, minute] = timePart.split(':').map(Number);
        return { hour, minute };
    },

    getStatusClass: function(status) {
        switch(status) {
            case 'CONFIRMED': return 'confirmed';
            case 'PENDING':
            case 'CREATED': return 'pending';
            case 'CANCELLED': return 'cancelled';
            case 'COMPLETED': return 'completed';
            default: return '';
        }
    },

    getStatusText: function(status) {
        switch(status) {
            case 'CONFIRMED': return '✅ Подтверждено';
            case 'PENDING': return '⏳ Ожидание';
            case 'CREATED': return '🆕 Создано';
            case 'CANCELLED': return '❌ Отменено';
            case 'COMPLETED': return '✔️ Выполнено';
            default: return status;
        }
    },

    setupEventListeners: function() {
        // Для навигации с клавиатуры
        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowLeft' && e.ctrlKey) {
                this.prevDay();
            } else if (e.key === 'ArrowRight' && e.ctrlKey) {
                this.nextDay();
            }
        });
    }
};

// Инициализация приложения
CalendarApp.init();

// Для отладки
window.CalendarApp = CalendarApp;
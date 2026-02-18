const CalendarApp = {
    currentStartDate: new Date().toISOString().split('T')[0], // Начальная дата ленты
    daysCount: 7, // Сколько дней показывать
    timelineData: null,
    appointments: [],
    availableDays: [],
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
            this.loadTimeline();
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
                    this.loadTimeline();
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

    loadTimeline: function() {
        this.showLoading();

        const formattedDate = this.formatDateForApi(this.currentStartDate);
        const url = `/api/appointments/timeline?startDate=${formattedDate}&daysCount=${this.daysCount}`;

        fetch(url, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => {
                if (!response.ok) {
                    if (response.status === 401 || response.status === 403) {
                        this.logout();
                        throw new Error('Авторизация не пройдена');
                    }
                    throw new Error('Ошибка загрузки: ' + response.status);
                }
                return response.json();
            })
            .then(data => {
                if (data.success) {
                    this.timelineData = data.data;
                    // Дополнительно загружаем рабочие дни и блокировки для этого периода
                    return this.loadAdditionalData();
                } else {
                    throw new Error(data.message);
                }
            })
            .then(() => {
                this.render();
            })
            .catch(error => {
                this.showError(error.message);
            });
    },

    loadAdditionalData: function() {
        if (!this.timelineData) return Promise.resolve();

        const startDate = this.formatDateForApi(this.timelineData.startDate);
        const endDate = this.formatDateForApi(this.timelineData.endDate);

        // Загружаем рабочие дни за период
        const schedulePromise = fetch(`/api/schedule/admin/available-days?startDate=${startDate}&endDate=${endDate}`, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => response.json())
            .then(data => {
                this.availableDays = data.data?.days || [];
            });

        // Загружаем блокировки за период
        const blocksPromise = fetch(`/api/schedule/blocks?startDate=${startDate}&endDate=${endDate}`, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => response.json())
            .then(data => {
                this.blocks = data.data?.blocks || [];
            });

        return Promise.all([schedulePromise, blocksPromise]);
    },

    showLoading: function() {
        document.getElementById('app').innerHTML = '<div class="loading">⏳ Загрузка ленты...</div>';
    },

    showError: function(message) {
        document.getElementById('app').innerHTML = `<div class="error">❌ Ошибка: ${message}</div>`;
    },

    render: function() {
        if (!this.timelineData) return;

        const app = document.getElementById('app');
        const user = JSON.parse(localStorage.getItem('user') || '{}');

        let html = `
            <div class="header">
                <div style="display: flex; justify-content: space-between; align-items: center;">
                    <h1>📅 Бесконечная лента расписания</h1>
                    <div style="display: flex; gap: 20px; align-items: center;">
                        <span>👤 ${user.firstName || 'Мастер'} (${user.role || 'ADMIN'})</span>
                        <button onclick="CalendarApp.logout()" style="padding: 5px 10px;">Выйти</button>
                    </div>
                </div>
                
                <div class="date-nav">
                    <button onclick="CalendarApp.prevWeek()">← Неделя назад</button>
                    <span id="currentRange">
                        ${this.formatDate(this.timelineData.startDate)} — ${this.formatDate(this.timelineData.endDate)}
                    </span>
                    <button onclick="CalendarApp.nextWeek()">Неделя вперед →</button>
                    <button onclick="CalendarApp.today()">Сегодня</button>
                    
                    <select id="daysCountSelect" onchange="CalendarApp.changeDaysCount()">
                        <option value="3">3 дня</option>
                        <option value="7" selected>7 дней</option>
                        <option value="14">14 дней</option>
                        <option value="30">30 дней</option>
                    </select>
                </div>
                
                <div class="stats">
                    <div class="stat-item">
                        <div class="stat-label">Всего записей</div>
                        <div class="stat-value">${this.timelineData.totalAppointments}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Подтверждено</div>
                        <div class="stat-value" style="color: #28a745;">${this.timelineData.stats.confirmedCount}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Ожидание</div>
                        <div class="stat-value" style="color: #ffc107;">${this.timelineData.stats.pendingCount}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Выполнено</div>
                        <div class="stat-value" style="color: #007bff;">${this.timelineData.stats.completedCount}</div>
                    </div>
                    <div class="stat-item">
                        <div class="stat-label">Отменено</div>
                        <div class="stat-value" style="color: #6c757d;">${this.timelineData.stats.cancelledCount}</div>
                    </div>
                </div>
            </div>
        `;

        // Рендерим ленту
        html += this.renderTimeline();

        // Легенда
        html += `
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
                <div class="legend-item">
                    <div class="legend-color" style="background: #f8f9fa; border: 2px dashed #aaa;"></div>
                    <span>Нет рабочего дня</span>
                </div>
            </div>
        `;

        app.innerHTML = html;

        // Устанавливаем выбранное значение в select
        document.getElementById('daysCountSelect').value = this.daysCount;
    },

    renderTimeline: function() {
        let html = '<div class="timeline-scroll-container" style="overflow-x: auto; white-space: nowrap;">';

        // Сортируем дни
        const sortedDays = Object.keys(this.timelineData.appointmentsByDay).sort();

        for (const dateStr of sortedDays) {
            const appointments = this.timelineData.appointmentsByDay[dateStr] || [];
            const availableDay = this.availableDays.find(d => d.availableDate === dateStr);
            const dayBlocks = this.blocks.filter(b => {
                const blockDate = b.startTime.split(' ')[0];
                return blockDate === dateStr;
            });

            html += this.renderDayColumn(dateStr, appointments, availableDay, dayBlocks);
        }

        html += '</div>';
        return html;
    },

    renderDayColumn: function(dateStr, appointments, availableDay, blocks) {
        const formattedDate = this.formatDate(dateStr);
        const dayName = this.getDayName(dateStr);
        const isToday = this.isToday(dateStr);
        const hasWorkingDay = availableDay && availableDay.available;

        let columnClass = 'day-column';
        if (isToday) columnClass += ' today';
        if (!hasWorkingDay) columnClass += ' non-working';

        let html = `
            <div class="${columnClass}" style="display: inline-block; vertical-align: top; width: 300px; margin-right: 10px; border: 1px solid #dee2e6; border-radius: 5px; background: white;">
                <div style="padding: 10px; background: #f8f9fa; border-bottom: 1px solid #dee2e6; position: sticky; left: 0;">
                    <div style="font-weight: bold;">${dayName}</div>
                    <div>${formattedDate}</div>
                    ${hasWorkingDay ?
            `<small>🕐 ${availableDay.workStart} — ${availableDay.workEnd}</small>` :
            '<small style="color: #dc3545;">❌ Нет рабочего дня</small>'}
                </div>
                <div class="appointments-list" style="min-height: 400px; padding: 10px; background: ${hasWorkingDay ? '#fff' : '#f8f9fa'};">
        `;

        // Добавляем блокировки
        blocks.forEach(block => {
            html += this.renderBlockItem(block);
        });

        // Добавляем записи
        appointments.forEach(apt => {
            html += this.renderAppointmentItem(apt);
        });

        // Если нет ни записей, ни блокировок, показываем пустой день
        if (appointments.length === 0 && blocks.length === 0) {
            html += '<div style="color: #aaa; text-align: center; padding: 20px;">Нет записей</div>';
        }

        html += `
                </div>
                ${hasWorkingDay ?
            `<div style="padding: 5px; border-top: 1px solid #dee2e6; text-align: center; background: #f8f9fa;">
                        <button onclick="CalendarApp.showAddAppointmentForm('${dateStr}')" style="font-size: 12px;">+ Добавить запись</button>
                    </div>` :
            `<div style="padding: 5px; border-top: 1px solid #dee2e6; text-align: center; background: #f8f9fa;">
                        <button onclick="CalendarApp.addAvailableDay('${dateStr}')" style="font-size: 12px;">➕ Сделать рабочим днём</button>
                    </div>`}
            </div>
        `;

        return html;
    },

    renderAppointmentItem: function(apt) {
        const statusClass = this.getStatusClass(apt.status);
        const statusText = this.getStatusText(apt.status);
        const timeStr = apt.startTime.split(' ')[1] + ' — ' + apt.endTime.split(' ')[1];

        return `
            <div class="appointment-item ${statusClass}" 
                 style="margin-bottom: 8px; padding: 8px; border-radius: 4px; cursor: pointer;"
                 onclick="CalendarApp.showAppointmentDetails(${JSON.stringify(apt).replace(/"/g, '&quot;')})"
                 draggable="true"
                 ondragstart="CalendarApp.dragStart(event, '${apt.id}')"
                 ondragend="CalendarApp.dragEnd(event)">
                <div style="display: flex; justify-content: space-between;">
                    <strong>${apt.client.firstName}</strong>
                    <small>${timeStr}</small>
                </div>
                <div style="font-size: 12px;">${apt.service.name}</div>
                <div style="font-size: 10px; color: #666;">${statusText}</div>
            </div>
        `;
    },

    renderBlockItem: function(block) {
        const timeStr = block.startTime.split(' ')[1] + ' — ' + block.endTime.split(' ')[1];

        return `
            <div class="blocked-item" 
                 style="margin-bottom: 8px; padding: 8px; border-radius: 4px; background: rgba(220,53,69,0.1); border: 1px solid #dc3545; color: #721c24;">
                <div style="display: flex; justify-content: space-between;">
                    <strong>🚫 ${block.reason || 'Заблокировано'}</strong>
                    <small>${timeStr}</small>
                </div>
                <div style="font-size: 12px;">${block.notes || ''}</div>
            </div>
        `;
    },

    // Навигация
    prevWeek: function() {
        const date = new Date(this.currentStartDate);
        date.setDate(date.getDate() - this.daysCount);
        this.currentStartDate = date.toISOString().split('T')[0];
        this.loadTimeline();
    },

    nextWeek: function() {
        const date = new Date(this.currentStartDate);
        date.setDate(date.getDate() + this.daysCount);
        this.currentStartDate = date.toISOString().split('T')[0];
        this.loadTimeline();
    },

    today: function() {
        this.currentStartDate = new Date().toISOString().split('T')[0];
        this.loadTimeline();
    },

    changeDaysCount: function() {
        this.daysCount = parseInt(document.getElementById('daysCountSelect').value);
        this.loadTimeline();
    },

    // Drag & Drop (заготовка)
    dragStart: function(event, appointmentId) {
        event.dataTransfer.setData('text/plain', appointmentId);
        event.dataTransfer.effectAllowed = 'move';
    },

    dragEnd: function(event) {
        // Будет реализовано позже
    },

    // Вспомогательные методы
    showAppointmentDetails: function(appointment) {
        const details = `
            📅 Запись #${appointment.id}\n
            👤 Клиент: ${appointment.client.firstName} ${appointment.client.lastName || ''}\n
            📞 Телефон: ${appointment.client.phone}\n
            💇 Услуга: ${appointment.service.name}\n
            ⏱ Длительность: ${appointment.service.durationMinutes} мин\n
            💰 Цена: ${appointment.service.price} руб\n
            🕐 Время: ${appointment.startTime} — ${appointment.endTime}\n
            📊 Статус: ${appointment.status}\n
            📝 Заметки: ${appointment.clientNotes || 'нет'}
        `;
        alert(details);
    },

    showAddAppointmentForm: function(dateStr) {
        alert('Добавить запись на ' + this.formatDate(dateStr) + ' (будет реализовано)');
    },

    addAvailableDay: function(dateStr) {
        const workStart = prompt('Введите время начала (например, 10:00)', '10:00');
        if (!workStart) return;

        const workEnd = prompt('Введите время окончания (например, 19:00)', '19:00');
        if (!workEnd) return;

        fetch(`/api/schedule/available-days?date=${dateStr}&workStart=${workStart}&workEnd=${workEnd}`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('✅ Рабочий день добавлен');
                    this.loadTimeline();
                } else {
                    alert('❌ Ошибка: ' + data.message);
                }
            });
    },

    formatDate: function(dateStr) {
        // Из '2026-02-18' в '18.02.2026'
        if (!dateStr) return '';
        const [year, month, day] = dateStr.split('-');
        return `${day}.${month}.${year}`;
    },

    formatDateForApi: function(dateStr) {
        // Из '2026-02-18' в '18.02.2026' для API
        return this.formatDate(dateStr);
    },

    getDayName: function(dateStr) {
        const date = new Date(dateStr + 'T12:00:00'); // Полдень, чтобы избежать проблем с часовыми поясами
        const days = ['Воскресенье', 'Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];
        return days[date.getDay()];
    },

    isToday: function(dateStr) {
        const today = new Date().toISOString().split('T')[0];
        return dateStr === today;
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
        document.addEventListener('keydown', (e) => {
            if (e.key === 'ArrowLeft' && e.ctrlKey) {
                this.prevWeek();
            } else if (e.key === 'ArrowRight' && e.ctrlKey) {
                this.nextWeek();
            }
        });
    }
};

// Инициализация приложения
CalendarApp.init();

// Для отладки
window.CalendarApp = CalendarApp;
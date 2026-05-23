// ПОЛНЫЙ ОБНОВЛЕННЫЙ ФАЙЛ calendar.js

const CalendarApp = {
    currentStartDate: new Date().toISOString().split('T')[0],
    daysCount: 7,
    timelineData: null,
    appointments: [],
    availableDays: [],
    blocks: [],
    selectedAppointment: null,
    selectedClient: null,

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
                    if (data.data.startDate && data.data.startDate.endsWith('.')) {
                        data.data.startDate = data.data.startDate.slice(0, -1);
                    }
                    if (data.data.endDate && data.data.endDate.endsWith('.')) {
                        data.data.endDate = data.data.endDate.slice(0, -1);
                    }

                    this.timelineData = data.data;
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

        const schedulePromise = fetch(`/api/schedule/admin/available-days?startDate=${startDate}&endDate=${endDate}`, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => response.json())
            .then(data => {
                this.availableDays = data.data?.days || [];
                this.availableDays = this.availableDays.map(day => {
                    if (day.availableDate && day.availableDate.includes('.')) {
                        const [dayStr, monthStr, yearStr] = day.availableDate.split('.');
                        day.availableDate = `${yearStr}-${monthStr}-${dayStr}`;
                    }
                    return day;
                });
            });

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
                    <span id="currentRange">${this.formatDateRange(this.timelineData.startDate, this.timelineData.endDate)}</span>
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

        html += this.renderTimeline();

        html += `
            <div class="legend">
                <div class="legend-item"><div class="legend-color" style="background: #28a745;"></div><span>Подтверждено</span></div>
                <div class="legend-item"><div class="legend-color" style="background: #ffc107;"></div><span>Ожидание</span></div>
                <div class="legend-item"><div class="legend-color" style="background: #007bff;"></div><span>Выполнено</span></div>
                <div class="legend-item"><div class="legend-color" style="background: #6c757d;"></div><span>Отменено</span></div>
                <div class="legend-item"><div class="legend-color" style="background: rgba(220,53,69,0.3); border: 2px solid #dc3545;"></div><span>Заблокировано</span></div>
                <div class="legend-item"><div class="legend-color" style="background: #f8f9fa; border: 2px dashed #aaa;"></div><span>Нет рабочего дня</span></div>
            </div>
        `;

        app.innerHTML = html;
        document.getElementById('daysCountSelect').value = this.daysCount;
    },

    renderTimeline: function() {
        let html = '<div class="timeline-scroll-container" style="overflow-x: auto; white-space: nowrap;">';

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
        const dayInfo = availableDay || this.availableDays.find(d => d && d.availableDate === dateStr);
        const hasWorkingDay = dayInfo && dayInfo.isAvailable === true;

        const formattedDate = this.formatDate(dateStr);
        const dayName = this.getDayName(dateStr);
        const isToday = this.isToday(dateStr);

        let columnClass = 'day-column';
        if (isToday) columnClass += ' today';
        if (!hasWorkingDay) columnClass += ' non-working';

        let workingHoursHtml = '';
        if (hasWorkingDay) {
            workingHoursHtml = `<small>🕐 ${dayInfo.workStart} — ${dayInfo.workEnd}</small>`;
        } else {
            workingHoursHtml = '<small style="color: #dc3545;">❌ Нет рабочего дня</small>';
        }

        let html = `
            <div class="${columnClass}" style="display: inline-block; vertical-align: top; width: 300px; margin-right: 10px; border: 1px solid #dee2e6; border-radius: 5px; background: white;">
                <div style="padding: 10px; background: #f8f9fa; border-bottom: 1px solid #dee2e6; position: sticky; left: 0;">
                    <div style="font-weight: bold;">${dayName}</div>
                    <div>${formattedDate}</div>
                    ${workingHoursHtml}
                </div>
                <div class="appointments-list" style="min-height: 400px; padding: 10px; background: ${hasWorkingDay ? '#fff' : '#f8f9fa'};"
                     ondragover="event.preventDefault()"
                     ondrop="CalendarApp.dropOnDay(event, '${dateStr}')">
        `;

        if (blocks && blocks.length > 0) {
            blocks.forEach(block => {
                html += this.renderBlockItem(block);
            });
        }

        if (appointments && appointments.length > 0) {
            appointments.forEach(apt => {
                html += this.renderAppointmentItem(apt);
            });
        }

        if ((!appointments || appointments.length === 0) && (!blocks || blocks.length === 0)) {
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
                 style="margin-bottom: 8px; padding: 8px; border-radius: 4px; position: relative;"
                 data-appointment-id="${apt.id}">
                
                <div class="drag-handle" 
                     style="position: absolute; left: 0; top: 0; bottom: 0; width: 20px; 
                            background: rgba(0,0,0,0.1); cursor: grab; border-radius: 4px 0 0 4px; display: flex; align-items: center; justify-content: center;"
                     draggable="true"
                     ondragstart="CalendarApp.dragStart(event, '${apt.id}')"
                     ondragend="CalendarApp.dragEnd(event)"
                     title="Перетащите чтобы переместить">
                    ⋮⋮
                </div>
                
                <div class="clickable-area" 
                     style="margin-left: 25px; cursor: pointer;"
                     onclick="CalendarApp.showAppointmentDetails(${JSON.stringify(apt).replace(/"/g, '&quot;')})">
                    <div style="display: flex; justify-content: space-between;">
                        <strong>${apt.client.firstName}</strong>
                        <small>${timeStr}</small>
                    </div>
                    <div style="font-size: 12px;">${apt.service.name}</div>
                    <div style="font-size: 10px; color: #666;">${statusText}</div>
                </div>
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

    // ========== ДЕТАЛИ КЛИЕНТА ==========

    showAppointmentDetails: function(appointment) {
        console.log('Клик по записи:', appointment);

        this.selectedAppointment = appointment;
        this.showLoadingModal();

        fetch(`/api/clients/${appointment.client.id}/details`, {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => {
                if (!response.ok) throw new Error('Ошибка загрузки: ' + response.status);
                return response.json();
            })
            .then(data => {
                console.log('Получены данные клиента:', data);
                this.closeLoadingModal();

                if (data.success) {
                    this.selectedClient = data.data.client;
                    this.showClientModal(data.data);
                } else {
                    alert('Ошибка: ' + data.message);
                }
            })
            .catch(error => {
                console.error('Ошибка:', error);
                this.closeLoadingModal();
                alert('Ошибка соединения: ' + error.message);
            });
    },

    showClientModal: function(clientData) {
        console.log('showClientModal вызван с данными:', clientData);

        this.closeLoadingModal();
        this.closeClientModal();

        const client = clientData.client;
        const stats = clientData.stats;

        const modal = document.createElement('div');
        modal.id = 'client-modal';
        modal.style.cssText = `
            position: fixed !important;
            top: 0 !important;
            left: 0 !important;
            width: 100vw !important;
            height: 100vh !important;
            background-color: rgba(0, 0, 0, 0.7) !important;
            display: flex !important;
            justify-content: center !important;
            align-items: center !important;
            z-index: 9999999 !important;
        `;

        const content = document.createElement('div');
        content.style.cssText = `
            background: white !important;
            width: 700px !important;
            max-height: 85vh !important;
            overflow-y: auto !important;
            border-radius: 12px !important;
            padding: 25px !important;
            box-shadow: 0 20px 60px rgba(0,0,0,0.3) !important;
            position: relative !important;
        `;

        // Форматируем историю записей
        const recentHtml = clientData.recentAppointments && clientData.recentAppointments.length > 0
            ? clientData.recentAppointments.map(apt => `
                <div style="padding: 10px; margin: 5px 0; background: #f8f9fa; border-left: 3px solid #007bff; border-radius: 0 4px 4px 0;">
                    <div><strong>${apt.startTime}</strong> — ${apt.service.name}</div>
                    <div style="font-size: 12px;">💰 ${apt.service.price} руб | ${this.getStatusText(apt.status)}</div>
                </div>
            `).join('')
            : '<div style="padding: 15px; text-align: center; color: #999;">📭 Нет истории посещений</div>';

        // Форматируем будущие записи
        const upcomingHtml = clientData.upcomingAppointments && clientData.upcomingAppointments.length > 0
            ? clientData.upcomingAppointments.map(apt => `
                <div style="padding: 10px; margin: 5px 0; background: #e8f4fd; border-left: 3px solid #007bff;">
                    <div><strong>${apt.startTime}</strong> — ${apt.service.name}</div>
                    <div style="font-size: 12px;">💰 ${apt.service.price} руб</div>
                </div>
            `).join('')
            : '<div style="padding: 15px; text-align: center; color: #999;">📅 Нет предстоящих записей</div>';

        content.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                <h2 style="margin: 0; font-size: 24px;">👤 ${client.firstName} ${client.lastName || ''}</h2>
                <button onclick="CalendarApp.closeClientModal()" style="border: none; background: none; font-size: 30px; cursor: pointer;">×</button>
            </div>
            
            <div style="background: #f8f9fa; padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 15px;">
                    <div>
                        <div style="color: #666; font-size: 12px;">📞 ТЕЛЕФОН</div>
                        <div style="font-size: 18px; font-weight: bold;">${client.phone || 'не указан'}</div>
                    </div>
                    <div>
                        <div style="color: #666; font-size: 12px;">🎂 ДЕНЬ РОЖДЕНИЯ</div>
                        <div>${client.birthDate || 'не указан'}</div>
                    </div>
                </div>
            </div>
            
            <div style="display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; margin-bottom: 20px;">
                <div style="background: #007bff; color: white; padding: 15px; border-radius: 8px; text-align: center;">
                    <div style="font-size: 24px; font-weight: bold;">${stats.totalVisits || 0}</div>
                    <div>Визитов</div>
                </div>
                <div style="background: #28a745; color: white; padding: 15px; border-radius: 8px; text-align: center;">
                    <div style="font-size: 24px; font-weight: bold;">${stats.totalSpent || 0} ₽</div>
                    <div>Потрачено</div>
                </div>
                <div style="background: #ffc107; color: #333; padding: 15px; border-radius: 8px; text-align: center;">
                    <div style="font-size: 24px; font-weight: bold;">${stats.attendanceRate || 0}%</div>
                    <div>Посещаемость</div>
                </div>
            </div>
            
            <div style="margin-bottom: 20px;">
                <h3 style="margin: 0 0 10px 0;">📊 Детальная статистика</h3>
                <div style="background: #f8f9fa; padding: 15px; border-radius: 8px;">
                    <div>Средний чек: <strong>${stats.averageBill || 0} ₽</strong></div>
                    <div>Любимая услуга: <strong>${stats.favoriteService || 'нет данных'}</strong></div>
                </div>
            </div>
            
            <div style="margin-bottom: 20px;">
                <h3 style="margin: 0 0 10px 0;">📅 Ближайшие записи</h3>
                ${upcomingHtml}
            </div>
            
            <div style="margin-bottom: 20px;">
                <h3 style="margin: 0 0 10px 0;">📋 История посещений</h3>
                ${recentHtml}
            </div>
            
            <div style="display: flex; gap: 10px; margin-top: 20px;">
                <button onclick="CalendarApp.editClient(${client.id})" 
                        style="flex: 1; padding: 12px; background: #007bff; color: white; border: none; border-radius: 5px; cursor: pointer;">
                    ✏️ Редактировать
                </button>
                <button onclick="CalendarApp.createAppointmentForClient(${client.id})" 
                        style="flex: 1; padding: 12px; background: #28a745; color: white; border: none; border-radius: 5px; cursor: pointer;">
                    ➕ Новая запись
                </button>
            </div>
        `;

        modal.appendChild(content);
        document.body.appendChild(modal);

        modal.onclick = function(e) {
            if (e.target === modal) {
                CalendarApp.closeClientModal();
            }
        };
    },

    closeClientModal: function() {
        const modal = document.getElementById('client-modal');
        if (modal) modal.remove();
    },

    showLoadingModal: function() {
        this.closeLoadingModal();

        const modal = document.createElement('div');
        modal.id = 'loading-modal';
        modal.style.cssText = `
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0,0,0,0.5);
            display: flex;
            justify-content: center;
            align-items: center;
            z-index: 999999;
        `;
        modal.innerHTML = '<div style="background: white; padding: 30px; border-radius: 10px;">⏳ Загрузка...</div>';
        document.body.appendChild(modal);
    },

    closeLoadingModal: function() {
        const modal = document.getElementById('loading-modal');
        if (modal) modal.remove();
    },

    // ========== РЕДАКТИРОВАНИЕ КЛИЕНТА (НОВОЕ) ==========

    editClient: function(clientId) {
        console.log('Редактирование клиента:', clientId);
        this.closeClientModal();

        if (!this.selectedClient) {
            alert('Ошибка: данные клиента не найдены');
            return;
        }

        this.showEditClientForm(this.selectedClient);
    },

    showEditClientForm: function(client) {
        const modal = document.createElement('div');
        modal.id = 'edit-client-modal';
        modal.style.cssText = `
            position: fixed !important;
            top: 0 !important;
            left: 0 !important;
            width: 100vw !important;
            height: 100vh !important;
            background: rgba(0, 0, 0, 0.7) !important;
            display: flex !important;
            justify-content: center !important;
            align-items: center !important;
            z-index: 9999999 !important;
        `;

        const form = document.createElement('div');
        form.style.cssText = `
            background: white !important;
            width: 500px !important;
            border-radius: 12px !important;
            padding: 30px !important;
        `;

        // Формируем дату для input type="date"
        let birthDateValue = '';
        if (client.birthDate) {
            const [day, month, year] = client.birthDate.split('.');
            birthDateValue = `${year}-${month}-${day}`;
        }

        form.innerHTML = `
            <h2 style="margin-top: 0;">✏️ Редактирование клиента</h2>
            
            <div style="margin-bottom: 15px;">
                <label style="display: block; margin-bottom: 5px;">Имя:</label>
                <input type="text" id="edit-firstname" value="${client.firstName || ''}" 
                       style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
            </div>
            
            <div style="margin-bottom: 15px;">
                <label style="display: block; margin-bottom: 5px;">Фамилия:</label>
                <input type="text" id="edit-lastname" value="${client.lastName || ''}" 
                       style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
            </div>
            
            <div style="margin-bottom: 15px;">
                <label style="display: block; margin-bottom: 5px;">Телефон:</label>
                <input type="text" id="edit-phone" value="${client.phone || ''}" 
                       style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
            </div>
            
            <div style="margin-bottom: 15px;">
                <label style="display: block; margin-bottom: 5px;">Дата рождения:</label>
                <input type="date" id="edit-birthdate" value="${birthDateValue}" 
                       style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
            </div>
            
            <div style="margin-bottom: 20px;">
                <label style="display: block; margin-bottom: 5px;">Заметки:</label>
                <textarea id="edit-notes" rows="4" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">${client.notes || ''}</textarea>
            </div>
            
            <div style="display: flex; gap: 10px;">
                <button onclick="CalendarApp.saveClient(${client.id})" 
                        style="flex: 1; padding: 12px; background: #28a745; color: white; border: none; border-radius: 5px; cursor: pointer;">
                    💾 Сохранить
                </button>
                <button onclick="CalendarApp.closeEditClientModal()" 
                        style="flex: 1; padding: 12px; background: #6c757d; color: white; border: none; border-radius: 5px; cursor: pointer;">
                    ❌ Отмена
                </button>
            </div>
        `;

        modal.appendChild(form);
        document.body.appendChild(modal);

        modal.onclick = function(e) {
            if (e.target === modal) {
                CalendarApp.closeEditClientModal();
            }
        };
    },

    closeEditClientModal: function() {
        const modal = document.getElementById('edit-client-modal');
        if (modal) modal.remove();
    },

    saveClient: function(clientId) {
        // Собираем данные из формы
        const firstName = document.getElementById('edit-firstname').value;
        const lastName = document.getElementById('edit-lastname').value;
        const phone = document.getElementById('edit-phone').value;
        const birthDateInput = document.getElementById('edit-birthdate').value;
        const notes = document.getElementById('edit-notes').value;

        // Конвертируем дату из YYYY-MM-DD в DD.MM.YYYY
        let birthDate = null;
        if (birthDateInput) {
            const [year, month, day] = birthDateInput.split('-');
            birthDate = `${day}.${month}.${year}`;
        }

        const data = {
            firstName: firstName || null,
            lastName: lastName || null,
            phone: phone || null,
            birthDate: birthDate,
            notes: notes || null
        };

        fetch(`/api/clients/${clientId}`, {
            method: 'PUT',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token'),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(data)
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('✅ Данные клиента обновлены');
                    this.closeEditClientModal();
                    // Обновляем данные клиента в карточке
                    this.loadTimeline();
                } else {
                    alert('❌ Ошибка: ' + data.message);
                }
            })
            .catch(error => {
                alert('❌ Ошибка соединения: ' + error.message);
            });
    },

    // ========== СОЗДАНИЕ ЗАПИСИ ДЛЯ КЛИЕНТА (НОВОЕ) ==========

    createAppointmentForClient: function(clientId) {
        console.log('Создание записи для клиента:', clientId);
        this.closeClientModal();

        // Получаем список услуг для выпадающего списка
        fetch('/api/services', {
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    this.showCreateAppointmentForm(clientId, data.data);
                } else {
                    alert('Ошибка загрузки услуг');
                }
            });
    },

    showCreateAppointmentForm: function(clientId, services) {
        const modal = document.createElement('div');
        modal.id = 'create-appointment-modal';
        modal.style.cssText = `
            position: fixed !important;
            top: 0 !important;
            left: 0 !important;
            width: 100vw !important;
            height: 100vh !important;
            background: rgba(0, 0, 0, 0.7) !important;
            display: flex !important;
            justify-content: center !important;
            align-items: center !important;
            z-index: 9999999 !important;
        `;

        const form = document.createElement('div');
        form.style.cssText = `
            background: white !important;
            width: 500px !important;
            border-radius: 12px !important;
            padding: 30px !important;
        `;

        // Создаем options для select
        const serviceOptions = services.map(s =>
            `<option value="${s.id}">${s.name} (${s.durationMinutes} мин, ${s.price} ₽)</option>`
        ).join('');

        form.innerHTML = `
            <h2 style="margin-top: 0;">📅 Новая запись</h2>
            
            <div style="margin-bottom: 15px;">
                <label style="display: block; margin-bottom: 5px;">Услуга:</label>
                <select id="appointment-service" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
                    ${serviceOptions}
                </select>
            </div>
            
            <div style="margin-bottom: 15px;">
                <label style="display: block; margin-bottom: 5px;">Дата:</label>
                <input type="date" id="appointment-date" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
            </div>
            
            <div style="margin-bottom: 15px;">
                <label style="display: block; margin-bottom: 5px;">Время:</label>
                <input type="time" id="appointment-time" value="12:00" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;">
            </div>
            
            <div style="margin-bottom: 20px;">
                <label style="display: block; margin-bottom: 5px;">Заметки:</label>
                <textarea id="appointment-notes" rows="3" style="width: 100%; padding: 8px; border: 1px solid #ddd; border-radius: 4px;"></textarea>
            </div>
            
            <div style="display: flex; gap: 10px;">
                <button onclick="CalendarApp.saveAppointmentForClient(${clientId})" 
                        style="flex: 1; padding: 12px; background: #28a745; color: white; border: none; border-radius: 5px; cursor: pointer;">
                    💾 Создать запись
                </button>
                <button onclick="CalendarApp.closeCreateAppointmentModal()" 
                        style="flex: 1; padding: 12px; background: #6c757d; color: white; border: none; border-radius: 5px; cursor: pointer;">
                    ❌ Отмена
                </button>
            </div>
        `;

        modal.appendChild(form);
        document.body.appendChild(modal);

        modal.onclick = function(e) {
            if (e.target === modal) {
                CalendarApp.closeCreateAppointmentModal();
            }
        };
    },

    closeCreateAppointmentModal: function() {
        const modal = document.getElementById('create-appointment-modal');
        if (modal) modal.remove();
    },

    saveAppointmentForClient: function(clientId) {
        const serviceId = document.getElementById('appointment-service').value;
        const date = document.getElementById('appointment-date').value;
        const time = document.getElementById('appointment-time').value;
        const notes = document.getElementById('appointment-notes').value;

        if (!date || !time) {
            alert('Выберите дату и время');
            return;
        }

        // Конвертируем дату и время в нужный формат (DD.MM.YYYY HH:MM)
        const [year, month, day] = date.split('-');
        const formattedDateTime = `${day}.${month}.${year} ${time}`;

        const request = {
            serviceId: parseInt(serviceId),
            startTime: formattedDateTime,
            notes: notes || null
        };

        fetch(`/api/clients/${clientId}/appointments`, {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token'),
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(request)
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('✅ Запись создана');
                    this.closeCreateAppointmentModal();
                    this.loadTimeline(); // Обновляем ленту
                } else {
                    alert('❌ Ошибка: ' + data.message);
                }
            })
            .catch(error => {
                alert('❌ Ошибка соединения: ' + error.message);
            });
    },

    // ========== DRAG & DROP ==========

    dragStart: function(event, appointmentId) {
        event.stopPropagation();
        event.dataTransfer.setData('text/plain', appointmentId);
        event.dataTransfer.effectAllowed = 'move';
        this.draggedAppointmentId = appointmentId;
        event.target.classList.add('dragging');
    },

    dragEnd: function(event) {
        event.target.classList.remove('dragging');
        this.draggedAppointmentId = null;
    },

    dropOnDay: function(event, targetDateStr) {
        event.preventDefault();

        const appointmentId = this.draggedAppointmentId;
        if (!appointmentId) return;

        const targetTime = prompt('Введите время (например, 14:30):', '12:00');
        if (!targetTime) return;

        const newDateTime = `${targetDateStr} ${targetTime}`;
        this.moveAppointment(appointmentId, newDateTime);
    },

    moveAppointment: function(appointmentId, newDateTime) {
        const formattedDateTime = this.formatDateTimeForApi(newDateTime);

        fetch(`/api/appointments/${appointmentId}/move?newStartTime=${encodeURIComponent(formattedDateTime)}`, {
            method: 'PATCH',
            headers: {
                'Authorization': 'Bearer ' + localStorage.getItem('token')
            }
        })
            .then(response => response.json())
            .then(data => {
                if (data.success) {
                    alert('✅ Запись перемещена');
                    this.loadTimeline();
                } else {
                    alert('❌ Ошибка: ' + data.message);
                }
            })
            .catch(error => {
                alert('❌ Ошибка соединения: ' + error.message);
            });
    },

    // ========== НАВИГАЦИЯ ==========

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

    showAddAppointmentForm: function(dateStr) {
        alert('Добавить запись на ' + this.formatDate(dateStr) + ' (будет реализовано)');
    },

    addAvailableDay: function(dateStr) {
        const workStart = prompt('Введите время начала (например, 10:00)', '10:00');
        if (!workStart) return;

        const workEnd = prompt('Введите время окончания (например, 19:00)', '19:00');
        if (!workEnd) return;

        fetch(`/api/schedule/admin/available-days?date=${dateStr}&workStart=${workStart}&workEnd=${workEnd}`, {
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
            })
            .catch(error => {
                alert('❌ Ошибка соединения: ' + error.message);
            });
    },

    // ========== ФОРМАТИРОВАНИЕ ==========

    formatDate: function(dateStr) {
        if (!dateStr) return '';
        if (dateStr.includes('.') && dateStr.split('.').length === 3) {
            return dateStr;
        }
        if (dateStr.includes('-')) {
            const [year, month, day] = dateStr.split('-');
            return `${day}.${month}.${year}`;
        }
        return dateStr;
    },

    formatDateForApi: function(dateStr) {
        if (!dateStr) return '';
        if (dateStr.includes('.') && dateStr.split('.').length === 3) {
            return dateStr;
        }
        if (dateStr.includes('-')) {
            const [year, month, day] = dateStr.split('-');
            return `${day}.${month}.${year}`;
        }
        return dateStr;
    },

    formatDateRange: function(startDate, endDate) {
        const start = this.formatDate(startDate);
        const end = this.formatDate(endDate);
        return `${start} — ${end}`;
    },

    formatDateTimeForApi: function(dateTimeStr) {
        if (!dateTimeStr) return '';
        if (dateTimeStr.includes('-')) {
            const [datePart, timePart] = dateTimeStr.split(' ');
            const [year, month, day] = datePart.split('-');
            return `${day}.${month}.${year} ${timePart}`;
        }
        return dateTimeStr;
    },

    getDayName: function(dateStr) {
        if (!dateStr) return '';
        try {
            let year, month, day;
            if (dateStr.includes('-')) {
                [year, month, day] = dateStr.split('-');
            } else if (dateStr.includes('.')) {
                [day, month, year] = dateStr.split('.');
            }
            const date = new Date(parseInt(year), parseInt(month) - 1, parseInt(day));
            const days = ['Воскресенье', 'Понедельник', 'Вторник', 'Среда', 'Четверг', 'Пятница', 'Суббота'];
            return days[date.getDay()];
        } catch (e) {
            return '';
        }
    },

    isToday: function(dateStr) {
        if (!dateStr) return false;
        const today = new Date().toISOString().split('T')[0];
        let compareDate = dateStr;
        if (dateStr.includes('.')) {
            const [day, month, year] = dateStr.split('.');
            compareDate = `${year}-${month}-${day}`;
        }
        return compareDate === today;
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

// Инициализация
CalendarApp.init();
window.CalendarApp = CalendarApp;
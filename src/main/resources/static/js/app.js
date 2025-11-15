// SprintPilot Application JavaScript

document.addEventListener('DOMContentLoaded', function() {
    console.log('SprintPilot Application Loaded');
    
    // Initialize tooltips
    var tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    var tooltipList = tooltipTriggerList.map(function (tooltipTriggerEl) {
        return new bootstrap.Tooltip(tooltipTriggerEl);
    });
    
    // Initialize popovers
    var popoverTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="popover"]'));
    var popoverList = popoverTriggerList.map(function (popoverTriggerEl) {
        return new bootstrap.Popover(popoverTriggerEl);
    });
});

// API Service
const ApiService = {
    baseUrl: '/api',
    
    // Sprint Management
    createSprint: async function(sprintData) {
        return this.post('/sprints/create', sprintData);
    },
    
    updateSprint: async function(id, sprintData) {
        return this.put(`/sprints/${id}`, sprintData);
    },
    
    getSprint: async function(id) {
        return this.get(`/sprints/${id}`);
    },
    
    getAllSprints: async function() {
        return this.get('/sprints');
    },
    
    getActiveSprints: async function() {
        return this.get('/sprints/active');
    },
    
    getCompletedSprints: async function() {
        return this.get('/sprints/completed');
    },
    
    startSprint: async function(id) {
        return this.post(`/sprints/${id}/start`);
    },
    
    completeSprint: async function(id) {
        return this.post(`/sprints/${id}/complete`);
    },
    
    // AI Services
    generateSprintSummary: async function(data) {
        return this.post('/ai/sprint-summary', data);
    },
    
    generateMeetingInvite: async function(data) {
        return this.post('/ai/meeting-invite', data);
    },
    
    generateRiskSummary: async function(data) {
        return this.post('/ai/risk-summary', data);
    },
    
    generateConfluencePage: async function(data) {
        return this.post('/ai/confluence-page', data);
    },
    
    generateTeamsMessage: async function(data) {
        return this.post('/ai/teams-message', data);
    },
    
    // HTTP Methods
    get: async function(endpoint) {
        try {
            const response = await fetch(this.baseUrl + endpoint);
            return await response.json();
        } catch (error) {
            console.error('GET request failed:', error);
            throw error;
        }
    },
    
    post: async function(endpoint, data) {
        try {
            const response = await fetch(this.baseUrl + endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data)
            });
            return await response.json();
        } catch (error) {
            console.error('POST request failed:', error);
            throw error;
        }
    },
    
    put: async function(endpoint, data) {
        try {
            const response = await fetch(this.baseUrl + endpoint, {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(data)
            });
            return await response.json();
        } catch (error) {
            console.error('PUT request failed:', error);
            throw error;
        }
    },
    
    delete: async function(endpoint) {
        try {
            const response = await fetch(this.baseUrl + endpoint, {
                method: 'DELETE'
            });
            return await response.json();
        } catch (error) {
            console.error('DELETE request failed:', error);
            throw error;
        }
    }
};

// Date Utilities
const DateUtils = {
    formatDate: function(dateString) {
        const date = new Date(dateString);
        return date.toLocaleDateString('en-US', { 
            year: 'numeric', 
            month: 'short', 
            day: 'numeric' 
        });
    },
    
    getWorkingDays: function(startDate, endDate, holidays = []) {
        let count = 0;
        const start = new Date(startDate);
        const end = new Date(endDate);
        const holidaySet = new Set(holidays);
        
        while (start <= end) {
            const dayOfWeek = start.getDay();
            const dateStr = start.toISOString().split('T')[0];
            
            if (dayOfWeek !== 0 && dayOfWeek !== 6 && !holidaySet.has(dateStr)) {
                count++;
            }
            
            start.setDate(start.getDate() + 1);
        }
        
        return count;
    },
    
    addWorkingDays: function(startDate, days, holidays = []) {
        const result = new Date(startDate);
        const holidaySet = new Set(holidays);
        let addedDays = 0;
        
        while (addedDays < days) {
            result.setDate(result.getDate() + 1);
            const dayOfWeek = result.getDay();
            const dateStr = result.toISOString().split('T')[0];
            
            if (dayOfWeek !== 0 && dayOfWeek !== 6 && !holidaySet.has(dateStr)) {
                addedDays++;
            }
        }
        
        return result;
    }
};

// UI Utilities
const UIUtils = {
    showLoading: function(elementId) {
        const element = document.getElementById(elementId);
        if (element) {
            element.innerHTML = '<div class="spinner-border" role="status"><span class="visually-hidden">Loading...</span></div>';
        }
    },
    
    hideLoading: function(elementId) {
        const element = document.getElementById(elementId);
        if (element) {
            element.innerHTML = '';
        }
    },
    
    showAlert: function(message, type = 'info') {
        const alertDiv = document.createElement('div');
        alertDiv.className = `alert alert-${type} alert-dismissible fade show`;
        alertDiv.setAttribute('role', 'alert');
        alertDiv.innerHTML = `
            ${message}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        `;
        
        const container = document.querySelector('.container-fluid');
        if (container) {
            container.insertBefore(alertDiv, container.firstChild);
            
            // Auto-dismiss after 5 seconds
            setTimeout(() => {
                alertDiv.remove();
            }, 5000);
        }
    },
    
    openModal: function(modalId) {
        const modal = new bootstrap.Modal(document.getElementById(modalId));
        modal.show();
    },
    
    closeModal: function(modalId) {
        const modal = bootstrap.Modal.getInstance(document.getElementById(modalId));
        if (modal) {
            modal.hide();
        }
    }
};

// Sprint Management Functions
const SprintManager = {
    createNewSprint: async function(formData) {
        UIUtils.showLoading('create-sprint-loading');
        
        try {
            const response = await ApiService.createSprint(formData);
            
            if (response.success) {
                UIUtils.showAlert('Sprint created successfully!', 'success');
                window.location.href = `/sprint/${response.data.id}`;
            } else {
                UIUtils.showAlert('Failed to create sprint: ' + response.error, 'danger');
            }
        } catch (error) {
            UIUtils.showAlert('Error creating sprint: ' + error.message, 'danger');
        } finally {
            UIUtils.hideLoading('create-sprint-loading');
        }
    },
    
    loadSprintDetails: async function(sprintId) {
        try {
            const response = await ApiService.getSprint(sprintId);
            
            if (response.success) {
                this.renderSprintDetails(response.data);
            } else {
                UIUtils.showAlert('Failed to load sprint details', 'danger');
            }
        } catch (error) {
            UIUtils.showAlert('Error loading sprint: ' + error.message, 'danger');
        }
    },
    
    renderSprintDetails: function(sprint) {
        // Render sprint details in the UI
        // This would update various elements on the page
        console.log('Rendering sprint:', sprint);
    }
};

// AI Integration Functions
const AIIntegration = {
    generateSprintSummary: async function(sprintId) {
        const button = event.target;
        const originalText = button.innerHTML;
        button.innerHTML = '<span class="spinner-border spinner-border-sm"></span> Generating...';
        button.disabled = true;
        
        try {
            const sprint = await ApiService.getSprint(sprintId);
            const response = await ApiService.generateSprintSummary(sprint.data);
            
            if (response.success) {
                this.displayAIResponse('sprint-summary-container', response.data);
            } else {
                UIUtils.showAlert('Failed to generate summary', 'danger');
            }
        } catch (error) {
            UIUtils.showAlert('Error generating summary: ' + error.message, 'danger');
        } finally {
            button.innerHTML = originalText;
            button.disabled = false;
        }
    },
    
    displayAIResponse: function(containerId, content) {
        const container = document.getElementById(containerId);
        if (container) {
            container.innerHTML = `
                <div class="ai-response fade-in">
                    <h5><i class="bi bi-stars"></i> AI Generated Content</h5>
                    <div class="content">${this.formatMarkdown(content)}</div>
                </div>
            `;
        }
    },
    
    formatMarkdown: function(text) {
        // Simple markdown to HTML conversion
        return text
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/^- (.*?)$/gm, '<li>$1</li>')
            .replace(/(<li>.*<\/li>)/s, '<ul>$1</ul>')
            .replace(/\n/g, '<br>');
    }
};

// Export to Outlook Function
function exportToOutlook(subject, body, attendees = '', startTime = '', duration = 60) {
    const outlookUrl = new URL('https://outlook.office.com/calendar/0/deeplink/compose');
    
    outlookUrl.searchParams.append('subject', subject);
    outlookUrl.searchParams.append('body', body);
    
    if (attendees) {
        outlookUrl.searchParams.append('to', attendees);
    }
    
    if (startTime) {
        outlookUrl.searchParams.append('startdt', startTime);
        const endTime = new Date(new Date(startTime).getTime() + duration * 60000);
        outlookUrl.searchParams.append('enddt', endTime.toISOString());
    }
    
    window.open(outlookUrl.toString(), '_blank');
}

// CSV Import Handler
function handleCSVImport(file) {
    const reader = new FileReader();
    
    reader.onload = function(e) {
        const csv = e.target.result;
        const lines = csv.split('\n');
        const headers = lines[0].split(',');
        const tasks = [];
        
        for (let i = 1; i < lines.length; i++) {
            if (lines[i].trim()) {
                const values = lines[i].split(',');
                const task = {};
                
                headers.forEach((header, index) => {
                    task[header.trim()] = values[index]?.trim();
                });
                
                tasks.push(task);
            }
        }
        
        displayImportPreview(tasks);
    };
    
    reader.readAsText(file);
}

function displayImportPreview(tasks) {
    const previewContainer = document.getElementById('import-preview');
    if (!previewContainer) return;
    
    let html = `
        <h4>Import Preview</h4>
        <div class="table-responsive">
            <table class="table table-sm">
                <thead>
                    <tr>
                        <th>Task Key</th>
                        <th>Summary</th>
                        <th>Story Points</th>
                        <th>Category</th>
                        <th>Assignee</th>
                    </tr>
                </thead>
                <tbody>
    `;
    
    tasks.forEach(task => {
        html += `
            <tr>
                <td>${task.key || ''}</td>
                <td>${task.summary || ''}</td>
                <td>${task.storyPoints || 0}</td>
                <td>${task.category || 'FEATURE'}</td>
                <td>${task.assignee || 'Unassigned'}</td>
            </tr>
        `;
    });
    
    html += `
                </tbody>
            </table>
        </div>
        <button class="btn btn-primary" onclick="confirmImport()">Confirm Import</button>
        <button class="btn btn-secondary" onclick="cancelImport()">Cancel</button>
    `;
    
    previewContainer.innerHTML = html;
}

// Global Functions for HTML onclick handlers
window.ApiService = ApiService;
window.DateUtils = DateUtils;
window.UIUtils = UIUtils;
window.SprintManager = SprintManager;
window.AIIntegration = AIIntegration;
window.exportToOutlook = exportToOutlook;
window.handleCSVImport = handleCSVImport;

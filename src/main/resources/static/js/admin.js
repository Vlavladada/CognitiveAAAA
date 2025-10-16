// Admin Dashboard JavaScript

class AdminDashboard {
    constructor() {
        this.currentTab = 'overview';
        this.currentUser = null;
        this.accessToken = null;
        this.supabase = null;
        
        this.initializeSupabase();
        this.initializeEventListeners();
    }
    
    async initializeSupabase() {
        try {
            // Fetch configuration from backend
            const response = await fetch('/api/config/public');
            const config = await response.json();
            
            this.supabase = supabase.createClient(config.supabaseUrl, config.supabaseAnonKey);
            this.checkAuthStatus();
        } catch (error) {
            console.error('Failed to load configuration:', error);
            alert('Failed to load configuration. Please check your setup.');
        }
    }
    
    initializeEventListeners() {
        // Tab navigation
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.addEventListener('click', (e) => {
                const tab = e.target.dataset.tab;
                this.switchTab(tab);
            });
        });
        
        // Action buttons
        document.getElementById('refreshBtn')?.addEventListener('click', () => this.refreshData());
        document.getElementById('signOutBtn')?.addEventListener('click', () => this.signOut());
        
        // User management
        document.getElementById('userSearch')?.addEventListener('input', (e) => this.filterUsers());
        document.getElementById('userRoleFilter')?.addEventListener('change', (e) => this.filterUsers());
        document.getElementById('prevPageBtn')?.addEventListener('click', () => this.previousPage());
        document.getElementById('nextPageBtn')?.addEventListener('click', () => this.nextPage());
        
        // Session management
        document.getElementById('sessionSearch')?.addEventListener('input', (e) => this.filterSessions());
        document.getElementById('sessionStatusFilter')?.addEventListener('change', (e) => this.filterSessions());
        
        // System actions
        document.getElementById('cleanupBtn')?.addEventListener('click', () => this.cleanupOldSessions());
        document.getElementById('exportDataBtn')?.addEventListener('click', () => this.exportData());
        document.getElementById('systemHealthBtn')?.addEventListener('click', () => this.checkSystemHealth());
        
        // Modal events
        document.querySelectorAll('.modal-close').forEach(btn => {
            btn.addEventListener('click', (e) => this.closeModal(e.target.closest('.modal')));
        });
        
        document.getElementById('saveUserBtn')?.addEventListener('click', () => this.saveUser());
        document.getElementById('cancelUserBtn')?.addEventListener('click', () => this.closeModal(document.getElementById('userModal')));
        document.getElementById('deleteSessionBtn')?.addEventListener('click', () => this.deleteSession());
        document.getElementById('closeSessionBtn')?.addEventListener('click', () => this.closeModal(document.getElementById('sessionModal')));
        
        // Close modal on outside click
        window.addEventListener('click', (e) => {
            if (e.target.classList.contains('modal')) {
                this.closeModal(e.target);
            }
        });
    }
    
    async checkAuthStatus() {
        try {
            const { data: { session } } = await this.supabase.auth.getSession();
            
            if (session) {
                this.currentUser = session.user;
                this.accessToken = session.access_token;
                
                // Check if user is admin
                const isAdmin = await this.checkAdminStatus();
                if (isAdmin) {
                    this.loadDashboard();
                } else {
                    alert('Access denied. Admin privileges required.');
                    this.signOut();
                }
            } else {
                this.redirectToLogin();
            }
        } catch (error) {
            console.error('Error checking auth status:', error);
            this.redirectToLogin();
        }
    }
    
    async checkAdminStatus() {
        try {
            const response = await fetch('/api/auth/me', {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });
            
            if (response.ok) {
                const userData = await response.json();
                return userData.role === 'ADMIN' || userData.role === 'SUPER_ADMIN';
            }
            return false;
        } catch (error) {
            console.error('Error checking admin status:', error);
            return false;
        }
    }
    
    redirectToLogin() {
        window.location.href = '/';
    }
    
    async signOut() {
        try {
            await this.supabase.auth.signOut();
            this.redirectToLogin();
        } catch (error) {
            console.error('Error signing out:', error);
        }
    }
    
    switchTab(tabName) {
        // Update navigation
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.remove('active');
        });
        document.querySelector(`[data-tab="${tabName}"]`).classList.add('active');
        
        // Update content
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(`${tabName}Tab`).classList.add('active');
        
        this.currentTab = tabName;
        
        // Load tab-specific data
        switch (tabName) {
            case 'overview':
                this.loadOverviewData();
                break;
            case 'users':
                this.loadUsersData();
                break;
            case 'sessions':
                this.loadSessionsData();
                break;
            case 'analytics':
                this.loadAnalyticsData();
                break;
            case 'system':
                this.loadSystemData();
                break;
        }
    }
    
    async loadDashboard() {
        await this.loadOverviewData();
    }
    
    async loadOverviewData() {
        try {
            const response = await fetch('/api/admin/stats/overview', {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });
            
            if (response.ok) {
                const stats = await response.json();
                this.updateOverviewStats(stats);
            }
        } catch (error) {
            console.error('Error loading overview data:', error);
        }
    }
    
    updateOverviewStats(stats) {
        document.getElementById('totalUsers').textContent = stats.totalUsers;
        document.getElementById('activeUsers').textContent = stats.activeUsers;
        document.getElementById('totalSessions').textContent = stats.totalSessions;
        document.getElementById('completedSessions').textContent = stats.completedSessions;
        document.getElementById('completionRate').textContent = `${(stats.completionRate * 100).toFixed(1)}%`;
    }
    
    async loadUsersData(page = 0) {
        try {
            const response = await fetch(`/api/admin/users?page=${page}&size=20`, {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });
            
            if (response.ok) {
                const usersPage = await response.json();
                this.updateUsersTable(usersPage.content);
                this.updatePagination(usersPage);
            }
        } catch (error) {
            console.error('Error loading users data:', error);
        }
    }
    
    updateUsersTable(users) {
        const tbody = document.getElementById('usersTableBody');
        tbody.innerHTML = '';
        
        users.forEach(user => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${user.email}</td>
                <td><span class="role-badge role-${user.role.toLowerCase().replace('_', '-')}">${user.role}</span></td>
                <td>${user.totalTestsCompleted}</td>
                <td>${user.lastLogin ? new Date(user.lastLogin).toLocaleString() : 'Never'}</td>
                <td><span class="status-badge ${user.isActive ? 'status-active' : 'status-inactive'}">${user.isActive ? 'Active' : 'Inactive'}</span></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="adminDashboard.editUser('${user.supabaseId}')">Edit</button>
                </td>
            `;
            tbody.appendChild(row);
        });
    }
    
    updatePagination(page) {
        const prevBtn = document.getElementById('prevPageBtn');
        const nextBtn = document.getElementById('nextPageBtn');
        const pageInfo = document.getElementById('pageInfo');
        
        prevBtn.disabled = page.first;
        nextBtn.disabled = page.last;
        pageInfo.textContent = `Page ${page.number + 1} of ${page.totalPages}`;
        
        prevBtn.onclick = () => this.loadUsersData(page.number - 1);
        nextBtn.onclick = () => this.loadUsersData(page.number + 1);
    }
    
    async loadSessionsData() {
        try {
            const response = await fetch('/api/admin/sessions?page=0&size=20', {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });
            
            if (response.ok) {
                const sessionsPage = await response.json();
                this.updateSessionsTable(sessionsPage.content);
            }
        } catch (error) {
            console.error('Error loading sessions data:', error);
        }
    }
    
    updateSessionsTable(sessions) {
        const tbody = document.getElementById('sessionsTableBody');
        tbody.innerHTML = '';
        
        sessions.forEach(session => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${session.sessionId.substring(0, 8)}...</td>
                <td>${session.user ? session.user.email : 'Anonymous'}</td>
                <td>${new Date(session.startTime).toLocaleString()}</td>
                <td>${session.endTime ? new Date(session.endTime).toLocaleString() : 'Incomplete'}</td>
                <td>${session.trials.length}</td>
                <td><span class="status-badge ${session.endTime ? 'status-completed' : 'status-incomplete'}">${session.endTime ? 'Completed' : 'Incomplete'}</span></td>
                <td>
                    <button class="btn btn-sm btn-primary" onclick="adminDashboard.viewSession('${session.sessionId}')">View</button>
                    <button class="btn btn-sm btn-danger" onclick="adminDashboard.deleteSession('${session.sessionId}')">Delete</button>
                </td>
            `;
            tbody.appendChild(row);
        });
    }
    
    async loadAnalyticsData() {
        try {
            const [activityResponse, performanceResponse] = await Promise.all([
                fetch('/api/admin/stats/users/activity', {
                    headers: { 'Authorization': `Bearer ${this.accessToken}` }
                }),
                fetch('/api/admin/stats/sessions/performance', {
                    headers: { 'Authorization': `Bearer ${this.accessToken}` }
                })
            ]);
            
            if (activityResponse.ok && performanceResponse.ok) {
                const activityData = await activityResponse.json();
                const performanceData = await performanceResponse.json();
                
                this.updateAnalyticsCharts(activityData, performanceData);
            }
        } catch (error) {
            console.error('Error loading analytics data:', error);
        }
    }
    
    updateAnalyticsCharts(activityData, performanceData) {
        // Placeholder for charts - in a real implementation, you'd use Chart.js or similar
        document.getElementById('userActivityChart').innerHTML = `
            <div style="display: flex; align-items: center; justify-content: center; height: 100%; color: #666;">
                Chart visualization would go here<br>
                <small>${activityData.length} users with recent activity</small>
            </div>
        `;
        
        document.getElementById('performanceChart').innerHTML = `
            <div style="display: flex; align-items: center; justify-content: center; height: 100%; color: #666;">
                Performance chart would go here<br>
                <small>${performanceData.length} completed sessions</small>
            </div>
        `;
        
        // Update top users table
        const topUsers = performanceData
            .filter(session => session.userId)
            .sort((a, b) => b.accuracy - a.accuracy)
            .slice(0, 10);
        
        const tbody = document.getElementById('topUsersTableBody');
        tbody.innerHTML = '';
        
        topUsers.forEach(session => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${session.userId}</td>
                <td>${session.totalTrials}</td>
                <td>${(session.accuracy * 100).toFixed(1)}%</td>
                <td>${session.averageResponseTime.toFixed(0)}ms</td>
            `;
            tbody.appendChild(row);
        });
    }
    
    async loadSystemData() {
        // Load system information
        const systemInfo = {
            'Application Version': '1.0.0',
            'Environment': 'Development',
            'Database': 'H2 In-Memory',
            'Last Cleanup': 'Never',
            'System Status': 'Healthy'
        };
        
        const infoGrid = document.getElementById('systemInfo');
        infoGrid.innerHTML = '';
        
        Object.entries(systemInfo).forEach(([label, value]) => {
            const infoItem = document.createElement('div');
            infoItem.className = 'info-item';
            infoItem.innerHTML = `
                <div class="info-label">${label}</div>
                <div class="info-value">${value}</div>
            `;
            infoGrid.appendChild(infoItem);
        });
    }
    
    async editUser(userId) {
        try {
            const response = await fetch(`/api/admin/users/${userId}`, {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });
            
            if (response.ok) {
                const user = await response.json();
                this.showUserModal(user);
            }
        } catch (error) {
            console.error('Error loading user:', error);
        }
    }
    
    showUserModal(user) {
        document.getElementById('userEmail').value = user.email;
        document.getElementById('userRole').value = user.role;
        document.getElementById('userStatus').value = user.isActive.toString();
        
        document.getElementById('userModal').style.display = 'block';
        
        // Store current user ID for saving
        this.currentEditingUserId = user.supabaseId;
    }
    
    async saveUser() {
        try {
            const role = document.getElementById('userRole').value;
            const isActive = document.getElementById('userStatus').value === 'true';
            
            const response = await fetch(`/api/admin/users/${this.currentEditingUserId}/role`, {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ role: role })
            });
            
            if (response.ok) {
                // Also update status
                await fetch(`/api/admin/users/${this.currentEditingUserId}/status`, {
                    method: 'PUT',
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`,
                        'Content-Type': 'application/json'
                    },
                    body: JSON.stringify({ isActive: isActive })
                });
                
                this.closeModal(document.getElementById('userModal'));
                this.loadUsersData();
                alert('User updated successfully');
            }
        } catch (error) {
            console.error('Error saving user:', error);
            alert('Error updating user');
        }
    }
    
    async viewSession(sessionId) {
        try {
            const response = await fetch(`/api/admin/sessions/${sessionId}`, {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });
            
            if (response.ok) {
                const session = await response.json();
                this.showSessionModal(session);
            }
        } catch (error) {
            console.error('Error loading session:', error);
        }
    }
    
    showSessionModal(session) {
        const details = document.getElementById('sessionDetails');
        details.innerHTML = `
            <div class="info-item">
                <div class="info-label">Session ID</div>
                <div class="info-value">${session.sessionId}</div>
            </div>
            <div class="info-item">
                <div class="info-label">User</div>
                <div class="info-value">${session.user ? session.user.email : 'Anonymous'}</div>
            </div>
            <div class="info-item">
                <div class="info-label">Start Time</div>
                <div class="info-value">${new Date(session.startTime).toLocaleString()}</div>
            </div>
            <div class="info-item">
                <div class="info-label">End Time</div>
                <div class="info-value">${session.endTime ? new Date(session.endTime).toLocaleString() : 'Incomplete'}</div>
            </div>
            <div class="info-item">
                <div class="info-label">Total Trials</div>
                <div class="info-value">${session.trials.length}</div>
            </div>
            <div class="info-item">
                <div class="info-label">Completed Trials</div>
                <div class="info-value">${session.trials.filter(t => t.status === 'COMPLETED').length}</div>
            </div>
        `;
        
        document.getElementById('sessionModal').style.display = 'block';
        this.currentEditingSessionId = session.sessionId;
    }
    
    async deleteSession(sessionId) {
        if (confirm('Are you sure you want to delete this session?')) {
            try {
                const response = await fetch(`/api/admin/sessions/${sessionId}`, {
                    method: 'DELETE',
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`
                    }
                });
                
                if (response.ok) {
                    this.closeModal(document.getElementById('sessionModal'));
                    this.loadSessionsData();
                    alert('Session deleted successfully');
                }
            } catch (error) {
                console.error('Error deleting session:', error);
                alert('Error deleting session');
            }
        }
    }
    
    async cleanupOldSessions() {
        if (confirm('This will delete incomplete sessions older than 30 days. Continue?')) {
            try {
                const response = await fetch('/api/admin/system/cleanup', {
                    method: 'POST',
                    headers: {
                        'Authorization': `Bearer ${this.accessToken}`
                    }
                });
                
                if (response.ok) {
                    const result = await response.json();
                    alert(`Cleanup completed. Deleted ${result.deletedSessions} sessions.`);
                    this.loadSystemData();
                }
            } catch (error) {
                console.error('Error during cleanup:', error);
                alert('Error during cleanup');
            }
        }
    }
    
    async exportData() {
        try {
            const response = await fetch('/api/admin/export/data', {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });
            
            if (response.ok) {
                const blob = await response.blob();
                const url = window.URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url;
                a.download = `cognitive-test-data-${new Date().toISOString().split('T')[0]}.csv`;
                document.body.appendChild(a);
                a.click();
                window.URL.revokeObjectURL(url);
                document.body.removeChild(a);
            }
        } catch (error) {
            console.error('Error exporting data:', error);
            alert('Error exporting data');
        }
    }
    
    async checkSystemHealth() {
        try {
            const response = await fetch('/api/admin/system/health', {
                headers: {
                    'Authorization': `Bearer ${this.accessToken}`
                }
            });
            
            if (response.ok) {
                const health = await response.json();
                alert(`System Health: ${health.status}\nUptime: ${health.uptime}\nMemory Usage: ${health.memoryUsage}`);
            }
        } catch (error) {
            console.error('Error checking system health:', error);
            alert('Error checking system health');
        }
    }
    
    closeModal(modal) {
        modal.style.display = 'none';
    }
    
    async refreshData() {
        switch (this.currentTab) {
            case 'overview':
                await this.loadOverviewData();
                break;
            case 'users':
                await this.loadUsersData();
                break;
            case 'sessions':
                await this.loadSessionsData();
                break;
            case 'analytics':
                await this.loadAnalyticsData();
                break;
            case 'system':
                await this.loadSystemData();
                break;
        }
    }
    
    filterUsers() {
        // Implement client-side filtering
        const searchTerm = document.getElementById('userSearch').value.toLowerCase();
        const roleFilter = document.getElementById('userRoleFilter').value;
        
        const rows = document.querySelectorAll('#usersTableBody tr');
        rows.forEach(row => {
            const email = row.cells[0].textContent.toLowerCase();
            const role = row.cells[1].textContent;
            
            const matchesSearch = email.includes(searchTerm);
            const matchesRole = !roleFilter || role.includes(roleFilter);
            
            row.style.display = matchesSearch && matchesRole ? '' : 'none';
        });
    }
    
    filterSessions() {
        // Implement client-side filtering
        const searchTerm = document.getElementById('sessionSearch').value.toLowerCase();
        const statusFilter = document.getElementById('sessionStatusFilter').value;
        
        const rows = document.querySelectorAll('#sessionsTableBody tr');
        rows.forEach(row => {
            const sessionId = row.cells[0].textContent.toLowerCase();
            const user = row.cells[1].textContent.toLowerCase();
            const status = row.cells[5].textContent.toLowerCase();
            
            const matchesSearch = sessionId.includes(searchTerm) || user.includes(searchTerm);
            const matchesStatus = !statusFilter || status.includes(statusFilter);
            
            row.style.display = matchesSearch && matchesStatus ? '' : 'none';
        });
    }
}

// Initialize admin dashboard when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.adminDashboard = new AdminDashboard();
});

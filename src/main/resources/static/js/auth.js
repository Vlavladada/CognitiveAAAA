class SupabaseAuthManager {
    constructor() {
        this.currentUser = null;
        this.isAuthenticated = false;
        this.supabase = null;
        this.init();
    }
    
    async init() {
        this.supabase = supabase.createClient(
            'https://ewseweiyrrrnpvhulriv.supabase.co',
            'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImV3c2V3ZWl5cnJybnB2aHVscml2Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjA2MTAxMTEsImV4cCI6MjA3NjE4NjExMX0.sB1C5ZtCbKBJytrmJBrB11Te8mOIN6719P0yi2XNt3I'
        );
        
        await this.checkAuthStatus();
        this.initializeEventListeners();
        
        // Update UI based on authentication status
        this.updateUIForAuthStatus();
    }
    
    async checkAuthStatus() {
        try {
            // Get current Supabase session
            const { data: { session } } = await this.supabase.auth.getSession();
            
            if (session?.user) {
                const response = await fetch('/api/auth/me', {
                    headers: {
                        'X-User-ID': session.user.id
                    }
                });
                if (response.ok) {
                    const user = await response.json();
                    if (user) {
                        this.currentUser = user;
                        this.isAuthenticated = true;
                        this.showMainApp();
                    } else {
                        this.showAuthForm();
                    }
                } else {
                    this.showAuthForm();
                }
            } else {
                this.showAuthForm();
            }
        } catch (error) {
            console.error('Error checking auth status:', error);
            this.showAuthForm();
        }
    }
    
    initializeEventListeners() {
        // Tab switching
        document.getElementById('signInTab')?.addEventListener('click', () => this.switchTab('signIn'));
        document.getElementById('signUpTab')?.addEventListener('click', () => this.switchTab('signUp'));
        
        // Form submissions
        document.getElementById('signInBtn')?.addEventListener('click', () => this.handleSignIn());
        document.getElementById('signUpBtn')?.addEventListener('click', () => this.handleSignUp());
        
        // Sign out
        document.getElementById('signOutBtn')?.addEventListener('click', () => this.handleSignOut());
        
        // Enter key handling
        document.getElementById('signInPassword')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleSignIn();
        });
        document.getElementById('signUpPassword')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.handleSignUp();
        });
    }
    
    switchTab(tab) {
        const signInTab = document.getElementById('signInTab');
        const signUpTab = document.getElementById('signUpTab');
        const signInForm = document.getElementById('signInForm');
        const signUpForm = document.getElementById('signUpForm');
        
        if (tab === 'signIn') {
            signInTab?.classList.add('active');
            signUpTab?.classList.remove('active');
            signInForm?.classList.remove('hidden');
            signUpForm?.classList.add('hidden');
        } else {
            signUpTab?.classList.add('active');
            signInTab?.classList.remove('active');
            signUpForm?.classList.remove('hidden');
            signInForm?.classList.add('hidden');
        }
        
        this.clearErrors();
    }
    
    async handleSignIn() {
        const email = document.getElementById('signInEmail')?.value;
        const password = document.getElementById('signInPassword')?.value;
        
        if (!email || !password) {
            this.showError('signInError', 'Please fill in all fields');
            return;
        }
        
        try {
            const { data, error } = await this.supabase.auth.signInWithPassword({
                email,
                password
            });
            
            if (error) {
                this.showError('signInError', error.message);
                return;
            }
            
            if (data.user) {
                // Send Supabase session to backend
                const response = await fetch('/api/auth/supabase', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        accessToken: data.session?.access_token || 'mock-token',
                        refreshToken: data.session?.refresh_token || 'mock-refresh-token',
                        email: data.user.email,
                        supabaseUserId: data.user.id
                    })
                });
                
                const result = await response.json();
                
                if (result.success) {
                    this.currentUser = result.user;
                    this.isAuthenticated = true;
                    this.showMainApp();
                } else {
                    this.showError('signInError', result.message);
                }
            }
        } catch (error) {
            console.error('Sign in error:', error);
            this.showError('signInError', 'An error occurred during sign in');
        }
    }
    
    async handleSignUp() {
        const email = document.getElementById('signUpEmail')?.value;
        const password = document.getElementById('signUpPassword')?.value;
        
        if (!email || !password) {
            this.showError('signUpError', 'Please fill in all fields');
            return;
        }
        
        if (password.length < 6) {
            this.showError('signUpError', 'Password must be at least 6 characters long');
            return;
        }
        
        try {
            const { data, error } = await this.supabase.auth.signUp({
                email,
                password
            });
            
            if (error) {
                this.showError('signUpError', error.message);
                return;
            }
            
            if (data.user) {
                // Send Supabase session to backend
                const response = await fetch('/api/auth/supabase', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify({
                        accessToken: data.session?.access_token || 'mock-token',
                        refreshToken: data.session?.refresh_token || 'mock-refresh-token',
                        email: data.user.email,
                        supabaseUserId: data.user.id
                    })
                });
                
                const result = await response.json();
                
                if (result.success) {
                    this.currentUser = result.user;
                    this.isAuthenticated = true;
                    this.showMainApp();
                } else {
                    this.showError('signUpError', result.message);
                }
            }
        } catch (error) {
            console.error('Sign up error:', error);
            this.showError('signUpError', 'An error occurred during registration');
        }
    }
    
    async handleSignOut() {
        try {
            await this.supabase.auth.signOut();
            await fetch('/api/auth/logout', {
                method: 'POST'
            });
            
            this.currentUser = null;
            this.isAuthenticated = false;
            this.showAuthForm();
        } catch (error) {
            console.error('Sign out error:', error);
        }
    }
    
    showError(elementId, message) {
        const errorElement = document.getElementById(elementId);
        if (errorElement) {
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }
    }
    
    clearErrors() {
        const errorElements = document.querySelectorAll('.error-message');
        errorElements.forEach(element => {
            element.textContent = '';
            element.style.display = 'none';
        });
    }
    
    showAuthForm() {
        document.getElementById('auth')?.classList.remove('hidden');
        document.getElementById('mainApp')?.classList.add('hidden');
    }
    
    showMainApp() {
        document.getElementById('auth')?.classList.add('hidden');
        document.getElementById('mainApp')?.classList.remove('hidden');
        
        // Update user info
        const userEmailElement = document.getElementById('userEmail');
        if (userEmailElement && this.currentUser) {
            userEmailElement.textContent = this.currentUser.email;
        }
        
        // Show/hide admin link based on role
        const adminLink = document.getElementById('adminLink');
        if (adminLink && this.currentUser) {
            if (this.currentUser.role === 'ADMIN' || this.currentUser.role === 'SUPER_ADMIN') {
                adminLink.classList.remove('hidden');
            } else {
                adminLink.classList.add('hidden');
            }
        }
        
        // Show sign out button
        document.getElementById('signOutBtn')?.classList.remove('hidden');
        
        // Show history button for authenticated users
        const viewHistoryBtn = document.getElementById('viewHistoryBtn');
        if (viewHistoryBtn) {
            viewHistoryBtn.classList.remove('hidden');
            viewHistoryBtn.addEventListener('click', () => this.showHistory());
        }
        
        // Handle back to instructions button
        const backToInstructionsBtn = document.getElementById('backToInstructionsBtn');
        if (backToInstructionsBtn) {
            backToInstructionsBtn.addEventListener('click', () => this.showInstructions());
        }
        
        // Handle device management button
        const manageDevicesBtn = document.getElementById('manageDevicesBtn');
        if (manageDevicesBtn) {
            manageDevicesBtn.addEventListener('click', () => this.showDevices());
        }
        
        // Handle device management buttons
        const backToInstructionsFromDevicesBtn = document.getElementById('backToInstructionsFromDevicesBtn');
        if (backToInstructionsFromDevicesBtn) {
            backToInstructionsFromDevicesBtn.addEventListener('click', () => this.showInstructions());
        }
        
        const startNewTestFromDevicesBtn = document.getElementById('startNewTestFromDevicesBtn');
        if (startNewTestFromDevicesBtn) {
            startNewTestFromDevicesBtn.addEventListener('click', () => this.showInstructions());
        }
        
        // Device management buttons
        const connectDevicesBtn = document.getElementById('connectDevicesBtn');
        if (connectDevicesBtn) {
            connectDevicesBtn.addEventListener('click', () => this.connectDevices());
        }
        
        const refreshDevicesBtn = document.getElementById('refreshDevicesBtn');
        if (refreshDevicesBtn) {
            refreshDevicesBtn.addEventListener('click', () => this.refreshDevices());
        }
    }
    
    async showHistory() {
        try {
            // Get current Supabase session
            const { data: { session } } = await this.supabase.auth.getSession();
            
            const headers = {};
            if (session?.user) {
                headers['X-User-ID'] = session.user.id;
            }
            
            const response = await fetch('/api/user/results', {
                headers: headers
            });
            const results = await response.json();
            
            const historyContent = document.getElementById('historyContent');
            if (!historyContent) return;
            
            if (results.length === 0) {
                historyContent.innerHTML = `
                    <div class="no-results">
                        <p>You haven't completed any tests yet.</p>
                        <p>Take your first test to see your results here!</p>
                    </div>
                `;
            } else {
                let historyHTML = '<div class="results-list">';
                
                results.forEach((result, index) => {
                    const date = new Date(result.createdAt).toLocaleDateString();
                    const isLatest = index === 0; // Most recent result
                    const testId = `test-${index}`;
                    
                    historyHTML += `
                        <div class="result-item ${isLatest ? 'latest-result' : ''}" data-test-id="${testId}">
                            <div class="result-header clickable-header" data-test-id="${testId}">
                                <h4>Test #${results.length - index} ${isLatest ? '<span class="latest-badge">Latest</span>' : ''}</h4>
                                <span class="result-date">${date}</span>
                                <span class="expand-icon" data-test-id="${testId}">▼</span>
                            </div>
                            <div class="result-summary">
                                <div class="summary-metric">
                                    <span class="label">Accuracy:</span>
                                    <span class="value">${result.accuracy.toFixed(1)}%</span>
                                </div>
                                <div class="summary-metric">
                                    <span class="label">Avg RT:</span>
                                    <span class="value">${result.averageResponseTime.toFixed(0)} ms</span>
                                </div>
                                <div class="summary-metric">
                                    <span class="label">Switch Cost:</span>
                                    <span class="value">${result.switchCost.toFixed(0)} ms</span>
                                </div>
                                <div class="summary-metric">
                                    <span class="label">Interference:</span>
                                    <span class="value">${result.taskInterference.toFixed(0)} ms</span>
                                </div>
                            </div>
                            <div class="detailed-results ${isLatest ? 'expanded' : 'collapsed'}" data-test-id="${testId}">
                                <h5>Detailed Performance</h5>
                                <div class="detailed-grid">
                                    <div class="detail-section">
                                        <h6>Task Performance</h6>
                                        <div class="detail-metric">
                                            <span class="label">Color Task:</span>
                                            <span class="value">${result.colorTaskAccuracy.toFixed(1)}% accuracy, ${result.colorTaskAvgRt.toFixed(0)}ms avg RT</span>
                                        </div>
                                        <div class="detail-metric">
                                            <span class="label">Shape Task:</span>
                                            <span class="value">${result.shapeTaskAccuracy.toFixed(1)}% accuracy, ${result.shapeTaskAvgRt.toFixed(0)}ms avg RT</span>
                                        </div>
                                    </div>
                                    <div class="detail-section">
                                        <h6>Cognitive Metrics</h6>
                                        <div class="detail-metric">
                                            <span class="label">Congruent Trials:</span>
                                            <span class="value">${result.congruentAvgRt.toFixed(0)}ms avg RT</span>
                                        </div>
                                        <div class="detail-metric">
                                            <span class="label">Incongruent Trials:</span>
                                            <span class="value">${result.incongruentAvgRt.toFixed(0)}ms avg RT</span>
                                        </div>
                                    </div>
                                </div>
                                <div class="interpretation">
                                    <h6>Performance Assessment</h6>
                                    <p class="assessment-text">
                                        ${this.getPerformanceAssessment(result)}
                                    </p>
                                </div>
                            </div>
                        </div>
                    `;
                });
                
                historyHTML += '</div>';
                historyContent.innerHTML = historyHTML;
            }
            
            // Hide all test phases and show history phase
            document.getElementById('instructionsPhase')?.classList.add('hidden');
            document.getElementById('training')?.classList.add('hidden');
            document.getElementById('test')?.classList.add('hidden');
            document.getElementById('historyPhase')?.classList.remove('hidden');
            
            // Add event listener for start new test button if it doesn't exist
            const startNewTestBtn = document.getElementById('startNewTestBtn');
            if (startNewTestBtn && !startNewTestBtn.hasAttribute('data-listener-added')) {
                startNewTestBtn.addEventListener('click', () => {
                    this.showInstructions();
                });
                startNewTestBtn.setAttribute('data-listener-added', 'true');
            }
            
            // Add click event listeners for expandable result items
            this.addExpandableListeners();
            
        } catch (error) {
            console.error('Error loading history:', error);
            const historyContent = document.getElementById('historyContent');
            if (historyContent) {
                historyContent.innerHTML = '<div class="error">Error loading your test history. Please try again.</div>';
            }
        }
    }
    
    updateUIForAuthStatus() {
        // Update authentication-dependent UI elements
        const authElements = document.querySelectorAll('[data-auth-required]');
        authElements.forEach(element => {
            if (this.isAuthenticated) {
                element.classList.remove('hidden');
            } else {
                element.classList.add('hidden');
            }
        });
        
        const unauthElements = document.querySelectorAll('[data-auth-hidden]');
        unauthElements.forEach(element => {
            if (this.isAuthenticated) {
                element.classList.add('hidden');
            } else {
                element.classList.remove('hidden');
            }
        });
    }
    
    showInstructions() {
        // Hide all phases
        document.getElementById('historyPhase')?.classList.add('hidden');
        document.getElementById('devicesPhase')?.classList.add('hidden');
        document.getElementById('training')?.classList.add('hidden');
        document.getElementById('test')?.classList.add('hidden');
        
        // Show instructions phase
        document.getElementById('instructionsPhase')?.classList.remove('hidden');
    }
    
    async showDevices() {
        try {
            // Hide all other phases
            document.getElementById('instructionsPhase')?.classList.add('hidden');
            document.getElementById('historyPhase')?.classList.add('hidden');
            document.getElementById('training')?.classList.add('hidden');
            document.getElementById('test')?.classList.add('hidden');
            
            // Show devices phase
            document.getElementById('devicesPhase')?.classList.remove('hidden');
            
            // Load connected devices
            await this.loadConnectedDevices();
            
        } catch (error) {
            console.error('Error showing devices:', error);
        }
    }
    
    async loadConnectedDevices() {
        try {
            const response = await fetch('/api/junction/devices');
            const devices = await response.json();
            
            const devicesList = document.getElementById('devicesList');
            if (!devicesList) return;
            
            if (devices.length === 0) {
                devicesList.innerHTML = `
                    <div class="no-devices">
                        <div class="no-devices-icon">
                            <span class="material-icons">devices_other</span>
                        </div>
                        <h3>No Devices Connected</h3>
                        <p>Connect your wearable devices and health apps to enhance your cognitive assessment.</p>
                        <p>Supported devices include Apple Health, Google Fit, Fitbit, Garmin, and many more.</p>
                    </div>
                `;
            } else {
                let devicesHTML = '<div class="devices-grid">';
                
                devices.forEach(device => {
                    const statusClass = device.status === 'connected' ? 'connected' : 'disconnected';
                    const statusIcon = device.status === 'connected' ? 'check_circle' : 'error';
                    
                    devicesHTML += `
                        <div class="device-card ${statusClass}">
                            <div class="device-header">
                                <div class="device-icon">
                                    <span class="material-icons">${this.getDeviceIcon(device.type)}</span>
                                </div>
                                <div class="device-status">
                                    <span class="material-icons status-icon">${statusIcon}</span>
                                </div>
                            </div>
                            <div class="device-info">
                                <h4>${device.name}</h4>
                                <p class="device-provider">${device.provider}</p>
                                <p class="device-type">${device.type}</p>
                                ${device.connectedAt ? `<p class="device-date">Connected: ${new Date(device.connectedAt).toLocaleDateString()}</p>` : ''}
                            </div>
                        </div>
                    `;
                });
                
                devicesHTML += '</div>';
                devicesList.innerHTML = devicesHTML;
            }
            
        } catch (error) {
            console.error('Error loading devices:', error);
            const devicesList = document.getElementById('devicesList');
            if (devicesList) {
                devicesList.innerHTML = '<div class="error">Error loading devices. Please try again.</div>';
            }
        }
    }
    
    getDeviceIcon(deviceType) {
        const iconMap = {
            'watch': 'watch',
            'scale': 'monitor_weight',
            'thermometer': 'thermostat',
            'glucose_meter': 'bloodtype',
            'blood_pressure': 'favorite',
            'heart_rate': 'favorite',
            'sleep': 'bedtime',
            'activity': 'directions_run',
            'nutrition': 'restaurant',
            'mindfulness': 'self_improvement'
        };
        return iconMap[deviceType.toLowerCase()] || 'devices_other';
    }
    
    async connectDevices() {
        try {
            const response = await fetch('/api/junction/link');
            const data = await response.json();
            
            if (data.linkUrl) {
                // Open Junction Link in a new window
                const linkWindow = window.open(data.linkUrl, 'junction-link', 'width=800,height=600,scrollbars=yes,resizable=yes');
                
                // Check if window is closed and refresh devices
                const checkClosed = setInterval(() => {
                    if (linkWindow.closed) {
                        clearInterval(checkClosed);
                        this.loadConnectedDevices();
                    }
                }, 1000);
            } else {
                alert('Failed to generate device connection link. Please try again.');
            }
        } catch (error) {
            console.error('Error connecting devices:', error);
            alert('Error connecting devices. Please try again.');
        }
    }
    
    async refreshDevices() {
        try {
            const response = await fetch('/api/junction/refresh', {
                method: 'POST'
            });
            const data = await response.json();
            
            if (data.success) {
                await this.loadConnectedDevices();
            } else {
                alert('Failed to refresh devices. Please try again.');
            }
        } catch (error) {
            console.error('Error refreshing devices:', error);
            alert('Error refreshing devices. Please try again.');
        }
    }
    
    getPerformanceAssessment(result) {
        let assessment = '';
        
        // Overall accuracy assessment
        if (result.accuracy >= 95) {
            assessment += 'Excellent overall accuracy (95%+) indicates exceptional attention control. ';
        } else if (result.accuracy >= 90) {
            assessment += 'Very good accuracy (90%+) shows strong cognitive performance. ';
        } else if (result.accuracy >= 85) {
            assessment += 'Good accuracy (85%+) with room for improvement. ';
        } else if (result.accuracy >= 80) {
            assessment += 'Average accuracy (80%+) suggests potential benefits from practice. ';
        } else {
            assessment += 'Below-average accuracy suggests potential benefits from cognitive training. ';
        }
        
        // Switch cost assessment
        if (result.switchCost <= 30) {
            assessment += 'Exceptional cognitive flexibility with minimal switching difficulty. ';
        } else if (result.switchCost <= 60) {
            assessment += 'Very good cognitive flexibility. ';
        } else if (result.switchCost <= 100) {
            assessment += 'Good cognitive flexibility typical of healthy adults. ';
        } else if (result.switchCost <= 150) {
            assessment += 'Average cognitive flexibility with some switching difficulty. ';
        } else {
            assessment += 'Below-average cognitive flexibility, suggesting potential benefits from executive function training. ';
        }
        
        // Interference assessment
        if (result.taskInterference <= 20) {
            assessment += 'Excellent interference control with superior ability to filter distractions.';
        } else if (result.taskInterference <= 40) {
            assessment += 'Very good interference control with minimal distraction effects.';
        } else if (result.taskInterference <= 70) {
            assessment += 'Good interference control with moderate susceptibility to irrelevant information.';
        } else if (result.taskInterference <= 120) {
            assessment += 'Average interference control with typical susceptibility to distractions.';
        } else {
            assessment += 'Below-average interference control, suggesting potential benefits from attention training.';
        }
        
        return assessment;
    }
    
    addExpandableListeners() {
        // Add click listeners to all result headers
        const clickableHeaders = document.querySelectorAll('.clickable-header');
        clickableHeaders.forEach(header => {
            header.addEventListener('click', (e) => {
                const testId = e.currentTarget.getAttribute('data-test-id');
                this.toggleDetailedResults(testId);
            });
        });
    }
    
    toggleDetailedResults(testId) {
        // Close all other expanded results
        const allDetailedResults = document.querySelectorAll('.detailed-results');
        const allExpandIcons = document.querySelectorAll('.expand-icon');
        
        allDetailedResults.forEach(details => {
            if (details.getAttribute('data-test-id') !== testId) {
                details.classList.remove('expanded');
                details.classList.add('collapsed');
            }
        });
        
        allExpandIcons.forEach(icon => {
            if (icon.getAttribute('data-test-id') !== testId) {
                icon.textContent = '▼';
                icon.style.transform = 'rotate(0deg)';
            }
        });
        
        // Toggle the clicked result
        const targetDetails = document.querySelector(`.detailed-results[data-test-id="${testId}"]`);
        const targetIcon = document.querySelector(`.expand-icon[data-test-id="${testId}"]`);
        
        if (targetDetails && targetIcon) {
            if (targetDetails.classList.contains('expanded')) {
                // Collapse
                targetDetails.classList.remove('expanded');
                targetDetails.classList.add('collapsed');
                targetIcon.textContent = '▼';
                targetIcon.style.transform = 'rotate(0deg)';
            } else {
                // Expand
                targetDetails.classList.remove('collapsed');
                targetDetails.classList.add('expanded');
                targetIcon.textContent = '▲';
                targetIcon.style.transform = 'rotate(0deg)';
            }
        }
    }
}

// Initialize auth manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.authManager = new SupabaseAuthManager();
});
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
                    historyHTML += `
                        <div class="result-item">
                            <div class="result-header">
                                <h4>Test #${results.length - index}</h4>
                                <span class="result-date">${date}</span>
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
                            <div class="result-actions">
                                <a href="/results/${result.session.sessionId}" class="btn btn-sm btn-primary">View Details</a>
                            </div>
                        </div>
                    `;
                });
                
                historyHTML += '</div>';
                historyContent.innerHTML = historyHTML;
            }
            
            // Show history phase
            document.getElementById('instructionsPhase')?.classList.add('hidden');
            document.getElementById('historyPhase')?.classList.remove('hidden');
            
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
        document.getElementById('historyPhase')?.classList.add('hidden');
        document.getElementById('instructionsPhase')?.classList.remove('hidden');
    }
}

// Initialize auth manager when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.authManager = new SupabaseAuthManager();
});
// Supabase Authentication JavaScript

class SupabaseAuth {
    constructor() {
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
            // Fallback to build-time configuration
            this.supabase = supabase.createClient(
                window.SUPABASE_URL || 'YOUR_SUPABASE_URL',
                window.SUPABASE_ANON_KEY || 'YOUR_SUPABASE_ANON_KEY'
            );
            this.checkAuthStatus();
        }
    }
    
    initializeEventListeners() {
        // Tab switching
        document.getElementById('signInTab')?.addEventListener('click', () => this.switchTab('signIn'));
        document.getElementById('signUpTab')?.addEventListener('click', () => this.switchTab('signUp'));
        
        // Authentication forms
        document.getElementById('signInBtn')?.addEventListener('click', () => this.signIn());
        document.getElementById('signUpBtn')?.addEventListener('click', () => this.signUp());
        document.getElementById('continueAnonymousBtn')?.addEventListener('click', () => this.continueAsGuest());
        document.getElementById('signOutBtn')?.addEventListener('click', () => this.signOut());
        
        // Enter key support
        document.getElementById('signInPassword')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.signIn();
        });
        document.getElementById('signUpPassword')?.addEventListener('keypress', (e) => {
            if (e.key === 'Enter') this.signUp();
        });
    }
    
    async checkAuthStatus() {
        try {
            const { data: { session } } = await this.supabase.auth.getSession();
            
            if (session) {
                this.currentUser = session.user;
                this.accessToken = session.access_token;
                this.showMainApp();
            } else {
                this.showAuthForm();
            }
        } catch (error) {
            console.error('Error checking auth status:', error);
            this.showAuthForm();
        }
    }
    
    switchTab(tab) {
        const signInTab = document.getElementById('signInTab');
        const signUpTab = document.getElementById('signUpTab');
        const signInForm = document.getElementById('signInForm');
        const signUpForm = document.getElementById('signUpForm');
        
        if (tab === 'signIn') {
            signInTab.classList.add('active');
            signUpTab.classList.remove('active');
            signInForm.classList.remove('hidden');
            signUpForm.classList.add('hidden');
        } else {
            signUpTab.classList.add('active');
            signInTab.classList.remove('active');
            signUpForm.classList.remove('hidden');
            signInForm.classList.add('hidden');
        }
        
        // Clear error messages
        this.clearErrors();
    }
    
    async signIn() {
        const email = document.getElementById('signInEmail').value;
        const password = document.getElementById('signInPassword').value;
        
        if (!email || !password) {
            this.showError('signInError', 'Please fill in all fields');
            return;
        }
        
        try {
            const { data, error } = await this.supabase.auth.signInWithPassword({
                email: email,
                password: password
            });
            
            if (error) {
                this.showError('signInError', error.message);
            } else {
                this.currentUser = data.user;
                this.accessToken = data.session.access_token;
                this.showMainApp();
            }
        } catch (error) {
            this.showError('signInError', 'An error occurred during sign in');
        }
    }
    
    async signUp() {
        const email = document.getElementById('signUpEmail').value;
        const password = document.getElementById('signUpPassword').value;
        
        if (!email || !password) {
            this.showError('signUpError', 'Please fill in all fields');
            return;
        }
        
        if (password.length < 6) {
            this.showError('signUpError', 'Password must be at least 6 characters');
            return;
        }
        
        try {
            const { data, error } = await this.supabase.auth.signUp({
                email: email,
                password: password
            });
            
            if (error) {
                this.showError('signUpError', error.message);
            } else {
                this.showError('signUpError', 'Check your email for verification link', 'success');
            }
        } catch (error) {
            this.showError('signUpError', 'An error occurred during sign up');
        }
    }
    
    async signOut() {
        try {
            await this.supabase.auth.signOut();
            this.currentUser = null;
            this.accessToken = null;
            this.showAuthForm();
        } catch (error) {
            console.error('Error signing out:', error);
        }
    }
    
    continueAsGuest() {
        this.currentUser = null;
        this.accessToken = null;
        this.showMainApp();
    }
    
    showAuthForm() {
        document.getElementById('auth').classList.remove('hidden');
        document.getElementById('mainApp').classList.add('hidden');
    }
    
    showMainApp() {
        document.getElementById('auth').classList.add('hidden');
        document.getElementById('mainApp').classList.remove('hidden');
        
        // Initialize the task switching test
        if (window.taskSwitchingTest) {
            window.taskSwitchingTest.setAuthToken(this.accessToken);
        }
    }
    
    showError(elementId, message, type = 'error') {
        const errorElement = document.getElementById(elementId);
        errorElement.textContent = message;
        errorElement.className = `error-message ${type}`;
    }
    
    clearErrors() {
        document.getElementById('signInError').textContent = '';
        document.getElementById('signUpError').textContent = '';
    }
    
    getAuthHeaders() {
        if (this.accessToken) {
            return {
                'Authorization': `Bearer ${this.accessToken}`,
                'Content-Type': 'application/json'
            };
        }
        return {
            'Content-Type': 'application/json'
        };
    }
}

// Initialize authentication when DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
    window.supabaseAuth = new SupabaseAuth();
});

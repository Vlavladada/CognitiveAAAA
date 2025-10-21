class TaskSwitchingTest {
    constructor() {
        this.sessionId = null;
        this.currentPhase = 'instructions';
        this.currentTrialIndex = 0;
        this.trials = [];
        this.trialStartTime = null;
        this.isWaitingForResponse = false;
        this.initializeEventListeners();
    }
    
    initializeEventListeners() {
        const startBtn = document.getElementById('startBtn');
        const restartBtn = document.getElementById('restartBtn');
        
        if (!startBtn) {
            console.error('startBtn element not found! Retrying in 100ms...');
            setTimeout(() => this.initializeEventListeners(), 100);
            return;
        }
        
        // startBtn is required, restartBtn is optional (only exists on results page)
        startBtn.addEventListener('click', () => this.startTest());
        
        if (restartBtn) {
            restartBtn.addEventListener('click', () => this.restartTest());
        }
        
        // Keyboard event listeners - use capture to ensure we get the events
        document.addEventListener('keydown', (e) => this.handleKeyPress(e), true);
        
        // Prevent default behavior for space bar
        document.addEventListener('keydown', (e) => {
            if (e.code === 'Space') {
                e.preventDefault();
            }
        });

        document.addEventListener('keydown', (e) => {
            console.log('Global keydown event:', e.key, 'code:', e.code);
        });
    }

    async readJsonOrText(resp) {
        const ct = resp.headers.get('content-type') || '';
        if (ct.includes('application/json')) return { kind: 'json', body: await resp.json() };
        return { kind: 'text', body: await resp.text() };
    }
    
    async startTest() {
        console.log('startTest() called');
        try {
            // Get current user ID from auth manager
            const userId = window.authManager?.currentUser?.supabaseUserId;
            
            // Create session
            const headers = {
                'Content-Type': 'application/x-www-form-urlencoded',
            };
            
            if (userId) {
                headers['X-User-ID'] = userId;
            }
            
            const response = (await fetch('/api/session', {
                method: 'POST',
                headers: headers,
                body: ''
            }));
            
            if (!response.ok) {
                throw new Error('Failed to create session');
            }
            
            console.log('Session created:', response);
            const parsed = await this.readJsonOrText(response);
            if (parsed.kind !== 'json') {
                console.error('Non-JSON session response:', parsed.body);
                alert('Server returned non-JSON for /api/session');
                return;
            }

            const session = parsed.body;
            console.log('Create session payload:', session);
            console.log('Session ID from payload:', session?.id);
            console.log('Session ID type:', typeof session?.id);

            const normalized = session?.id;
            if (!normalized) {
                console.error('Invalid session id from server:', session?.id);
                alert('Server returned an invalid session id.');
                return;
            }

            this.sessionId = normalized;
            console.log('SessionId set to:', this.sessionId);
            console.log('SessionId type after setting:', typeof this.sessionId);

            await this.startTraining();
        } catch (error) {
            console.error('Error starting test:', error);
            alert('Error starting test. Please try again.');
        }
    }
    
    async startTraining() {
        try {
            console.log('Starting training with sessionId:', this.sessionId);
            console.log('SessionId type:', typeof this.sessionId);
            const response = await fetch(`/api/session/${this.sessionId}/training`, {
                method: 'POST'
            });
            
            if (!response.ok) {
                throw new Error('Failed to start training');
            }
            
            this.trials = await response.json();
            this.currentTrialIndex = 0;
            this.currentPhase = 'training';
            
            this.showPhase('training');
            this.updateProgress('trainingProgress', 0, this.trials.length);
            
            // Start first trial after proper preparation time
            setTimeout(() => this.runNextTrial(), 2000);
            
        } catch (error) {
            console.error('Error starting training:', error);
            alert('Error starting training. Please try again.');
        }
    }
    
    async startTestPhase() {
        try {
            const response = await fetch(`/api/session/${this.sessionId}/test`, {
                method: 'POST'
            });
            
            if (!response.ok) {
                throw new Error('Failed to start test');
            }
            
            this.trials = await response.json();
            this.currentTrialIndex = 0;
            this.currentPhase = 'test';
            
            this.showPhase('test');
            this.updateProgress('testProgress', 0, this.trials.length);
            
            // Start first trial after proper preparation time
            setTimeout(() => this.runNextTrial(), 2000);
            
        } catch (error) {
            console.error('Error starting test:', error);
            alert('Error starting test. Please try again.');
        }
    }
    
    async runNextTrial() {
        if (this.currentTrialIndex >= this.trials.length) {
            if (this.currentPhase === 'training') {
                // Training complete, start actual test
                await this.startTestPhase();
            } else {
                // Test complete, show results
                await this.showResults();
            }
            return;
        }
        
        const trial = this.trials[this.currentTrialIndex];
        const containerId = this.currentPhase === 'training' ? 'trainingTrial' : 'testTrial';
        const progressId = this.currentPhase === 'training' ? 'trainingProgress' : 'testProgress';
        
        this.updateProgress(progressId, this.currentTrialIndex, this.trials.length);
        
        // Implement proper timing parameters following psychological best practices
        // Fixation: 800ms (standard in cognitive testing)
        this.showFixationPoint(containerId);
        
        setTimeout(() => {
            // Cue: 200ms (brief but visible)
            this.showCue(containerId, trial.taskType);
            
            setTimeout(() => {
                // Clear cue and prepare for stimulus
                this.clearContainer(containerId);
                
                // Cue-Stimulus Interval (CSI): 800ms (allows task preparation)
                setTimeout(() => {
                    this.showStimulus(containerId, trial);
                }, 800);
                
            }, 200); // cue display time
            
        }, 800); // fixation time
    }
    
    showFixationPoint(containerId) {
        const container = document.getElementById(containerId);
        container.innerHTML = '<div class="fixation-point">+</div>';
    }
    
    showCue(containerId, taskType) {
        const container = document.getElementById(containerId);
        const cueText = taskType === 'COLOR' ? 'COLOR' : 'SHAPE';
        const cueClass = taskType === 'COLOR' ? 'color-cue' : 'shape-cue';
        container.innerHTML = `<div class="cue ${cueClass}">${cueText}</div>`;
    }
    
    showStimulus(containerId, trial) {
        const container = document.getElementById(containerId);
        
        // Create stimulus class name
        const shapeClass = trial.stimulusShape.toLowerCase();
        const colorClass = trial.stimulusColor.toLowerCase();
        const stimulusClass = `${shapeClass}-${colorClass}`;
        
        // Create stimulus symbol
        const symbol = trial.stimulusShape === 'CIRCLE' ? '●' : '■';
        
        container.innerHTML = `
            <div class="stimulus ${stimulusClass}">${symbol}</div>
            <div class="feedback" style="display: none;"></div>
        `;
        
        // Use requestAnimationFrame to ensure DOM is updated before starting timer
        requestAnimationFrame(() => {
            this.trialStartTime = Date.now();
            this.isWaitingForResponse = true;
            
            console.log('Stimulus displayed, timer started at:', this.trialStartTime);
            
            // Set timeout for response
            setTimeout(() => {
                if (this.isWaitingForResponse) {
                    console.log('Timeout triggered - no response received');
                    this.handleTimeout(containerId);
                }
            }, 2000);
        });
    }
    
    handleKeyPress(event) {
        if (!this.isWaitingForResponse) {
            console.log('Key press ignored - not waiting for response');
            return;
        }
        
        console.log('Key pressed:', event.key);
        
        let response = null;
        if (event.key.toLowerCase() === 'b') {
            response = 'LEFT';
        } else if (event.key.toLowerCase() === 'n') {
            response = 'RIGHT';
        }
        
        if (response) {
            console.log('Processing response:', response);
            this.handleResponse(response);
        } else {
            console.log('Invalid key pressed:', event.key);
        }
    }
    
    async handleResponse(response) {
        if (!this.isWaitingForResponse) {
            console.log('Response ignored - not waiting for response');
            return;
        }
        
        this.isWaitingForResponse = false;
        const responseTime = Date.now() - this.trialStartTime;
        const trial = this.trials[this.currentTrialIndex];
        
        console.log('Response received:', response, 'Response time:', responseTime, 'ms');
        console.log('Expected response:', trial.correctResponse);
        
        // Show feedback
        const containerId = this.currentPhase === 'training' ? 'trainingTrial' : 'testTrial';
        const feedbackElement = document.querySelector(`#${containerId} .feedback`);
        
        const isCorrect = response === trial.correctResponse;
        feedbackElement.textContent = isCorrect ? 'Correct!' : 'Incorrect';
        feedbackElement.className = `feedback ${isCorrect ? 'correct' : 'incorrect'}`;
        feedbackElement.style.display = 'block';
        
        // Record response
        try {
            await fetch(`/api/session/${this.sessionId}/response`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    response: response,
                    responseTime: responseTime
                })
            });
        } catch (error) {
            console.error('Error recording response:', error);
        }
        
        // Move to next trial after delay
        setTimeout(() => {
            this.currentTrialIndex++;
            this.runNextTrial();
        }, 1000);
    }
    
    handleTimeout(containerId) {
        if (!this.isWaitingForResponse) return;
        
        this.isWaitingForResponse = false;
        const feedbackElement = document.querySelector(`#${containerId} .feedback`);
        feedbackElement.textContent = 'Too slow!';
        feedbackElement.className = 'feedback timeout';
        feedbackElement.style.display = 'block';
        
        // Record timeout
        fetch(`/api/session/${this.sessionId}/response`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                response: null,
                responseTime: 2000
            })
        }).catch(error => console.error('Error recording timeout:', error));
        
        // Move to next trial after delay
        setTimeout(() => {
            this.currentTrialIndex++;
            this.runNextTrial();
        }, 1000);
    }
    
    async showResults() {
        try {
            const response = await fetch(`/api/session/${this.sessionId}/complete`, {
                method: 'POST'
            });
            
            if (!response.ok) {
                throw new Error('Failed to get results');
            }
            
            const results = await response.json();
            
            // Navigate to the history view within the main app
            if (window.authManager && window.authManager.isAuthenticated) {
                // User is authenticated, show the history view
                await window.authManager.showHistory();
                // Reset test state
                this.resetTestState();
            } else {
                // User is not authenticated, redirect to results page
                window.location.href = `/results/${this.sessionId}`;
            }
            
        } catch (error) {
            console.error('Error getting results:', error);
            alert('Error getting results. Please try again.');
        }
    }
    
    displayResults(results) {
        const resultsContent = document.getElementById('resultsContent');
        
        resultsContent.innerHTML = `
            <div class="results-content">
                <h3>Your Performance</h3>
                
                <div class="results-metric">
                    <span class="label">Total Trials:</span>
                    <span class="value">${results.totalTrials}</span>
                </div>
                
                <div class="results-metric">
                    <span class="label">Correct Responses:</span>
                    <span class="value">${results.correctTrials} (${(results.accuracy * 100).toFixed(1)}%)</span>
                </div>
                
                <div class="results-metric">
                    <span class="label">Average Response Time:</span>
                    <span class="value">${results.averageRT.toFixed(0)} ms</span>
                </div>
                
                <div class="results-metric">
                    <span class="label">Task-Repeat Trials RT:</span>
                    <span class="value">${results.repeatTrialsRT.toFixed(0)} ms</span>
                </div>
                
                <div class="results-metric">
                    <span class="label">Task-Switch Trials RT:</span>
                    <span class="value">${results.switchTrialsRT.toFixed(0)} ms</span>
                </div>
                
                <div class="results-metric highlight">
                    <span class="label">Task-Switch Cost:</span>
                    <span class="value">${results.switchCost.toFixed(0)} ms</span>
                </div>
                
                <div class="results-metric">
                    <span class="label">Congruent Trials RT:</span>
                    <span class="value">${results.congruentTrialsRT.toFixed(0)} ms</span>
                </div>
                
                <div class="results-metric">
                    <span class="label">Incongruent Trials RT:</span>
                    <span class="value">${results.incongruentTrialsRT.toFixed(0)} ms</span>
                </div>
                
                <div class="results-metric highlight">
                    <span class="label">Task Interference:</span>
                    <span class="value">${results.taskInterference.toFixed(0)} ms</span>
                </div>
                
                <div style="margin-top: 30px; padding: 20px; background: #f8f9fa; border-radius: 8px;">
                    <h4>Interpretation:</h4>
                    <p><strong>Task-Switch Cost:</strong> The difference in response time between task-switch and task-repeat trials. Higher values indicate more difficulty switching between tasks.</p>
                    <p><strong>Task Interference:</strong> The difference in response time between incongruent and congruent trials. Higher values indicate more interference from irrelevant stimulus features.</p>
                </div>
            </div>
        `;
    }
    
    showPhase(phaseName) {
        // Hide all phases
        document.querySelectorAll('.test-phase').forEach(phase => {
            phase.classList.add('hidden');
        });
        
        // Show target phase
        document.getElementById(phaseName).classList.remove('hidden');
    }
    
    updateProgress(progressId, current, total) {
        const progressBar = document.querySelector(`#${progressId} .progress-bar::after`);
        const progressText = document.querySelector(`#${progressId} .progress-text`);
        
        const percentage = (current / total) * 100;
        document.querySelector(`#${progressId} .progress-bar`).style.setProperty('--progress', `${percentage}%`);
        
        if (progressText) {
            progressText.textContent = `Trial ${current} of ${total}`;
        }
        
        // Update progress bar width
        const progressBarElement = document.querySelector(`#${progressId} .progress-bar`);
        progressBarElement.style.setProperty('--progress-width', `${percentage}%`);
    }
    
    clearContainer(containerId) {
        const container = document.getElementById(containerId);
        container.innerHTML = '';
    }
    
    restartTest() {
        this.sessionId = null;
        this.currentPhase = 'instructions';
        this.currentTrialIndex = 0;
        this.trials = [];
        this.trialStartTime = null;
        this.isWaitingForResponse = false;
        
        this.showPhase('instructionsPhase');
    }
    
    resetTestState() {
        this.sessionId = null;
        this.currentPhase = 'instructions';
        this.currentTrialIndex = 0;
        this.trials = [];
        this.trialStartTime = null;
        this.isWaitingForResponse = false;
        
        // Clear any trial containers
        const trainingTrial = document.getElementById('trainingTrial');
        const testTrial = document.getElementById('testTrial');
        if (trainingTrial) trainingTrial.innerHTML = '';
        if (testTrial) testTrial.innerHTML = '';
        
        // Reset progress bars
        const trainingProgress = document.getElementById('trainingProgress');
        const testProgress = document.getElementById('testProgress');
        if (trainingProgress) {
            trainingProgress.style.setProperty('--progress-width', '0%');
        }
        if (testProgress) {
            testProgress.style.setProperty('--progress-width', '0%');
        }
    }
}

// Initialize the test when the page loads
document.addEventListener('DOMContentLoaded', () => {
    new TaskSwitchingTest();
});

// Update CSS for progress bar
const style = document.createElement('style');
style.textContent = `
    .progress-bar::after {
        width: var(--progress-width, 0%) !important;
    }
`;
document.head.appendChild(style);

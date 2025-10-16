# 🔐 Security Configuration Guide

## ⚠️ **IMPORTANT: Supabase Anon Key Security**

The Supabase **anon key** is designed to be **publicly accessible** but should still be protected in production environments.

### **🔍 Understanding Supabase Anon Key**

**✅ What it's designed for:**
- **Client-side authentication** (browser, mobile apps)
- **Public access** to your Supabase project
- **Row Level Security (RLS)** provides the actual protection

**⚠️ Security considerations:**
- **Rate limiting** - can be abused for API quota consumption
- **Resource usage** - attackers could exhaust your database connections
- **Information disclosure** - reveals your Supabase project URL

## 🛡️ **Security Implementation**

### **1. Environment-Based Configuration**

```kotlin
@GetMapping("/public")
fun getPublicConfig(): Map<String, Any> {
    return when (environment.lowercase()) {
        "production" -> {
            // ✅ PRODUCTION: Only safe configuration
            mapOf(
                "appVersion" to appVersion,
                "environment" to environment,
                "features" to mapOf(
                    "authentication" to true,
                    "adminPanel" to true
                )
            )
        }
        "development", "test" -> {
            // ⚠️ DEVELOPMENT: Full config for convenience
            mapOf(
                "supabaseUrl" to supabaseUrl,
                "supabaseAnonKey" to supabaseAnonKey,
                "appVersion" to appVersion,
                "environment" to environment
            )
        }
        else -> {
            // ✅ DEFAULT: Safe configuration
            mapOf(
                "appVersion" to appVersion,
                "environment" to environment
            )
        }
    }
}
```

### **2. Build-Time Configuration (Recommended)**

**For Production Deployment:**

```html
<!-- In greeting.html -->
<script th:if="${@environment.getProperty('app.environment') == 'production'}">
    window.SUPABASE_URL = /*[[${@environment.getProperty('supabase.url')}]]*/ 'YOUR_SUPABASE_URL';
    window.SUPABASE_ANON_KEY = /*[[${@environment.getProperty('supabase.anon-key')}]]*/ 'YOUR_SUPABASE_ANON_KEY';
</script>
```

**Benefits:**
- ✅ **No API calls** to fetch configuration
- ✅ **Server-side rendering** of credentials
- ✅ **No network exposure** of sensitive data
- ✅ **Faster loading** - no additional HTTP requests

### **3. Frontend Security**

```javascript
async initializeSupabase() {
    try {
        const response = await fetch('/api/config/public');
        const config = await response.json();
        
        // Check if we have Supabase credentials (development only)
        if (config.supabaseUrl && config.supabaseAnonKey) {
            this.supabase = supabase.createClient(config.supabaseUrl, config.supabaseAnonKey);
        } else {
            // Production mode - use build-time configuration
            this.supabase = supabase.createClient(
                window.SUPABASE_URL || 'YOUR_SUPABASE_URL',
                window.SUPABASE_ANON_KEY || 'YOUR_SUPABASE_ANON_KEY'
            );
        }
    } catch (error) {
        // Fallback to build-time configuration
        this.supabase = supabase.createClient(
            window.SUPABASE_URL || 'YOUR_SUPABASE_URL',
            window.SUPABASE_ANON_KEY || 'YOUR_SUPABASE_ANON_KEY'
        );
    }
}
```

## 🚀 **Deployment Strategies**

### **Option 1: Build-Time Configuration (Recommended)**

```properties
# application.properties
app.environment=production
supabase.url=https://your-project.supabase.co
supabase.anon-key=your-anon-key
supabase.jwt.secret=your-jwt-secret
```

**Cloud Run Environment Variables:**
```bash
APP_ENVIRONMENT=production
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_ANON_KEY=your-anon-key
SUPABASE_JWT_SECRET=your-jwt-secret
```

### **Option 2: Environment Variables Only**

```javascript
// Frontend uses environment variables directly
const supabase = supabase.createClient(
    process.env.SUPABASE_URL,
    process.env.SUPABASE_ANON_KEY
);
```

### **Option 3: Secure API Endpoint**

```kotlin
@GetMapping("/config")
@PreAuthorize("hasRole('USER')")
fun getConfig(principal: Principal): Map<String, Any> {
    // Only authenticated users get configuration
    return mapOf(
        "supabaseUrl" to supabaseUrl,
        "supabaseAnonKey" to supabaseAnonKey
    )
}
```

## 🔒 **Additional Security Measures**

### **1. Supabase Row Level Security (RLS)**

```sql
-- Enable RLS on all tables
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE test_sessions ENABLE ROW LEVEL SECURITY;
ALTER TABLE trials ENABLE ROW LEVEL SECURITY;

-- Users can only see their own data
CREATE POLICY "Users can view own data" ON users
    FOR SELECT USING (auth.uid()::text = supabase_id);

CREATE POLICY "Users can view own sessions" ON test_sessions
    FOR SELECT USING (auth.uid()::text = user_supabase_id);
```

### **2. Rate Limiting**

```kotlin
@Component
class RateLimitingFilter : OncePerRequestFilter() {
    private val rateLimiter = RateLimiter.create(100.0) // 100 requests per second
    
    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, filterChain: FilterChain) {
        if (!rateLimiter.tryAcquire()) {
            response.status = HttpStatus.TOO_MANY_REQUESTS.value()
            return
        }
        filterChain.doFilter(request, response)
    }
}
```

### **3. CORS Configuration**

```kotlin
@Bean
fun corsConfigurationSource(): CorsConfigurationSource {
    val configuration = CorsConfiguration()
    configuration.allowedOriginPatterns = listOf("https://yourdomain.com", "https://*.yourdomain.com")
    configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
    configuration.allowedHeaders = listOf("*")
    configuration.allowCredentials = true
    
    val source = UrlBasedCorsConfigurationSource()
    source.registerCorsConfiguration("/**", configuration)
    return source
}
```

## 📊 **Security Comparison**

| Method | Security Level | Performance | Complexity | Recommended |
|--------|---------------|-------------|------------|-------------|
| **Build-time Config** | 🟢 High | 🟢 Fast | 🟡 Medium | ✅ **Yes** |
| **Environment Variables** | 🟢 High | 🟢 Fast | 🟢 Low | ✅ **Yes** |
| **Secure API Endpoint** | 🟡 Medium | 🟡 Medium | 🔴 High | ⚠️ **Maybe** |
| **Public API Endpoint** | 🔴 Low | 🟡 Medium | 🟢 Low | ❌ **No** |

## 🎯 **Recommendations**

### **For Production:**
1. ✅ Use **build-time configuration** with Thymeleaf
2. ✅ Set `app.environment=production`
3. ✅ Enable **Supabase RLS** on all tables
4. ✅ Implement **rate limiting**
5. ✅ Configure **proper CORS**

### **For Development:**
1. ✅ Use **dynamic configuration** from `/api/config/public`
2. ✅ Set `app.environment=development`
3. ✅ Keep credentials in `application.properties`

### **For Cloud Deployment:**
1. ✅ Use **environment variables** in Cloud Run
2. ✅ Set `APP_ENVIRONMENT=production`
3. ✅ Never commit credentials to version control

## 🚨 **Security Checklist**

- [ ] **Environment-based configuration** implemented
- [ ] **Build-time credentials** for production
- [ ] **Supabase RLS** enabled on all tables
- [ ] **Rate limiting** configured
- [ ] **CORS** properly configured
- [ ] **No credentials** in version control
- [ ] **Environment variables** used in production
- [ ] **Security headers** configured
- [ ] **HTTPS only** in production
- [ ] **Regular security audits** scheduled

---

**Remember:** The Supabase anon key is designed to be public, but implementing proper security measures protects against abuse and ensures your application scales securely! 🛡️

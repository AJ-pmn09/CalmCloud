// Example Production Configuration
// Copy these values to app/build.gradle.kts when ready for production

/*
// ============================================
// PRODUCTION CONFIGURATION EXAMPLE
// ============================================

defaultConfig {
    // Option 1: Using Public IP (HTTP - Testing Only)
    buildConfigField("String", "SERVER_URL", "\"http://203.0.113.45\"")
    buildConfigField("String", "SERVER_PORT", "\"3003\"")
    
    // Option 2: Using Domain with HTTPS (Production - Recommended)
    // buildConfigField("String", "SERVER_URL", "\"https://api.calmcloud.com\"")
    // buildConfigField("String", "SERVER_PORT", "\"443\"")
    
    buildConfigField("boolean", "ENABLE_DEBUG_CONFIG", "false")
}

buildTypes {
    debug {
        // Development server (local network)
        buildConfigField("String", "SERVER_URL", "\"http://10.0.2.2\"")  // Emulator
        // OR
        // buildConfigField("String", "SERVER_URL", "\"http://192.168.100.6\"")  // Same network
        buildConfigField("String", "SERVER_PORT", "\"3003\"")
        buildConfigField("boolean", "ENABLE_DEBUG_CONFIG", "true")
    }
    release {
        // Production server (public URL)
        buildConfigField("String", "SERVER_URL", "\"https://api.calmcloud.com\"")
        buildConfigField("String", "SERVER_PORT", "\"443\"")
        buildConfigField("boolean", "ENABLE_DEBUG_CONFIG", "false")
    }
}
*/


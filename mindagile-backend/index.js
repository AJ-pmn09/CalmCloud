const express = require("express");
const cors = require("cors");
const http = require("http");
const { Server } = require("socket.io");
const rateLimit = require("express-rate-limit");
const helmet = require("helmet");
require('dotenv').config();

const app = express();
const server = http.createServer(app);

// Security middleware - Helmet for security headers
app.use(helmet({
  contentSecurityPolicy: false, // Disable CSP for API
  crossOriginEmbedderPolicy: false
}));

// CORS configuration
const corsOptions = {
  origin: process.env.NODE_ENV === 'production' 
    ? process.env.ALLOWED_ORIGINS?.split(',') || '*' 
    : '*', // Allow all in development
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
};
app.use(cors(corsOptions));

// Rate limiting - prevent brute force attacks
const limiter = rateLimit({
  windowMs: 15 * 60 * 1000, // 15 minutes
  max: process.env.NODE_ENV === 'production' ? 100 : 1000, // Limit each IP to 100 requests per windowMs in production
  message: 'Too many requests from this IP, please try again later.',
  standardHeaders: true,
  legacyHeaders: false,
});

// Apply rate limiting to API routes
app.use('/api/', limiter);

// Body parsing middleware
app.use(express.json({ limit: '10mb' }));
app.use(express.urlencoded({ extended: true, limit: '10mb' }));

// Static file serving for images (if public directory exists)
const path = require('path');
const fs = require('fs');
const publicPath = path.join(__dirname, 'public');
if (fs.existsSync(publicPath)) {
  app.use('/images', express.static(path.join(publicPath, 'images')));
  console.log('✅ Static file serving enabled for /images');
} else {
  // Create a placeholder route for images to prevent 404 errors
  app.use('/images', (req, res) => {
    res.status(404).json({ error: 'Image not found', path: req.path });
  });
  console.log('⚠️  Public directory not found - image serving disabled');
}

// Request logging
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// Routes
const addLoginToken = require('./middleware/addLoginToken');
const authRoutes = require('./routes/auth');
const studentDataRoutes = require('./routes/studentData');
const assistanceRoutes = require('./routes/assistance');
const remindersRoutes = require('./routes/reminders');
const alertsRoutes = require('./routes/alerts');
const communicationsRoutes = require('./routes/communications');
const appointmentsRoutes = require('./routes/appointments');
const achievementsRoutes = require('./routes/achievements');
const counselorNotesRoutes = require('./routes/counselorNotes');
const fhirExportRoutes = require('./routes/fhirExport');
const activityLogsRoutes = require('./routes/activityLogs');
const staffRepliesRoutes = require('./routes/staffReplies');
const analyticsRoutes = require('./routes/analytics');
let screenersRoutes;
try {
  screenersRoutes = require('./routes/screeners');
} catch (e) {
  screenersRoutes = null;
  console.warn('⚠️  Screeners not loaded (copy routes/screeners.js and services/screenerScoring.js to enable clinical screeners)');
}

app.use('/api/auth', addLoginToken);
app.use('/api/auth', authRoutes);
app.use('/api', studentDataRoutes);
app.use('/api', assistanceRoutes);
app.use('/api', remindersRoutes);
app.use('/api', alertsRoutes);
app.use('/api/communications', communicationsRoutes);
app.use('/api', appointmentsRoutes);
app.use('/api', achievementsRoutes);
app.use('/api', counselorNotesRoutes);
app.use('/api', fhirExportRoutes);
app.use('/api', activityLogsRoutes);
app.use('/api', staffRepliesRoutes);
app.use('/api', analyticsRoutes);
if (screenersRoutes) app.use('/api', screenersRoutes);

// Health check (before rate limiting) - /hello and /api/hello for flexibility
const healthHandler = (req, res) => {
  res.json({ 
    message: "MindAIgle K-12 Backend 🚀", 
    status: "healthy",
    timestamp: new Date().toISOString(),
    version: "1.0.0"
  });
};
app.get("/hello", healthHandler);
app.get("/api/hello", healthHandler);

// API health check with multi-database status
app.get("/api/health", async (req, res) => {
  try {
    const { testAllConnections } = require('./config/databaseManager');
    const dbStatus = await testAllConnections();
    const useSingleDb = process.env.USE_SINGLE_DB === 'true';
    const allConnected = Object.values(dbStatus).every(db => db.status === 'connected');

    res.json({
      status: allConnected ? "healthy" : "degraded",
      timestamp: new Date().toISOString(),
      mode: useSingleDb ? "single" : "multi",
      databases: dbStatus,
      allDatabasesConnected: allConnected
    });
  } catch (error) {
    res.status(500).json({
      status: "error",
      timestamp: new Date().toISOString(),
      error: process.env.NODE_ENV !== 'production' ? error.message : undefined
    });
  }
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Error:', err);
  // Don't expose internal error details in production
  const errorMessage = process.env.NODE_ENV === 'production' 
    ? 'Server error' 
    : err.message;
  res.status(err.status || 500).json({
    error: 'Server error',
    ...(process.env.NODE_ENV !== 'production' && { details: errorMessage, stack: err.stack })
  });
});

// Initialize Socket.IO
const io = new Server(server, {
  cors: {
    origin: '*',
    methods: ['GET', 'POST'],
    credentials: true
  }
});

// WebSocket connection handler - export io for use in routes
const socketHandler = require('./websocket/socketHandler')(io);
app.set('io', io); // Make io available to routes via req.app.get('io')

// Start reminder scheduler (runs every hour)
const { checkAndSendReminders } = require('./jobs/reminderScheduler');
setInterval(() => {
  checkAndSendReminders().catch(err => {
    console.error('Reminder scheduler error:', err);
  });
}, 60 * 60 * 1000); // Run every hour

// Run immediately on startup
checkAndSendReminders().catch(err => {
  console.error('Initial reminder check error:', err);
});

// Initialize and test all database connections on startup
const { testAllConnections } = require('./config/databaseManager');
testAllConnections().then(dbStatus => {
  console.log('\n📊 Database Connection Status:');
  Object.entries(dbStatus).forEach(([school, status]) => {
    const icon = status.status === 'connected' ? '✅' : '❌';
    console.log(`  ${icon} ${school}: ${status.status}`);
  });
  console.log('');
}).catch(err => {
  console.error('❌ Error testing database connections:', err);
});

const PORT = process.env.PORT || 3002;
server.listen(PORT, '0.0.0.0', () => {
  console.log(`\n=== MindAigle Backend Started ===`);
  console.log(`Time: ${new Date().toISOString()}`);
  console.log(`HTTP Server: http://0.0.0.0:${PORT}`);
  console.log(`WebSocket Server: ws://0.0.0.0:${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}\n`);
});

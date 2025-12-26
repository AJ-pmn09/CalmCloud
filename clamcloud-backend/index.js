const express = require("express");
const cors = require("cors");
require('dotenv').config();

const app = express();

// Middleware
app.use(cors({
  origin: '*', // Allow all origins for development
  methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
  allowedHeaders: ['Content-Type', 'Authorization'],
  credentials: true
}));
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Request logging
app.use((req, res, next) => {
  console.log(`[${new Date().toISOString()}] ${req.method} ${req.path}`);
  next();
});

// Routes
const authRoutes = require('./routes/auth');
const studentDataRoutes = require('./routes/studentData');
const assistanceRoutes = require('./routes/assistance');

app.use('/api/auth', authRoutes);
app.use('/api', studentDataRoutes);
app.use('/api', assistanceRoutes);

// Health check
app.get("/hello", (req, res) => {
  res.json({ message: "CalmCloud K-12 Backend 🚀", status: "healthy" });
});

// Error handling middleware
app.use((err, req, res, next) => {
  console.error('Error:', err);
  res.status(500).json({
    error: 'Server error',
    details: err.message
  });
});

const PORT = process.env.PORT || 8080;
app.listen(PORT, '0.0.0.0', () => {
  console.log(`\n=== CalmCloud Backend Started ===`);
  console.log(`Time: ${new Date().toISOString()}`);
  console.log(`Listening on http://0.0.0.0:${PORT}`);
  console.log(`Environment: ${process.env.NODE_ENV || 'development'}\n`);
});

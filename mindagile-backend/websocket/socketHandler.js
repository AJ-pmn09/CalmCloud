const jwt = require('jsonwebtoken');

// Store connected clients by user ID and role
const connectedClients = new Map();

module.exports = (io) => {
  // Authentication middleware for Socket.IO
  io.use((socket, next) => {
    const token = socket.handshake.auth.token || socket.handshake.headers.authorization?.replace('Bearer ', '');
    
    if (!token) {
      // Allow connection without auth for testing, but mark as unauthenticated
      socket.userId = null;
      socket.userRole = null;
      return next();
    }

    try {
      const decoded = jwt.verify(token, process.env.JWT_SECRET || 'your-secret-key');
      socket.userId = decoded.userId;
      socket.userRole = decoded.role;
      next();
    } catch (error) {
      // Allow connection but mark as unauthenticated
      socket.userId = null;
      socket.userRole = null;
      next();
    }
  });

  io.on('connection', (socket) => {
    const userId = socket.userId;
    const userRole = socket.userRole;

    console.log(`[WebSocket] Client connected: ${socket.id}${userId ? ` (User: ${userId}, Role: ${userRole})` : ' (Unauthenticated)'}`);

    // Store client connection
    if (userId) {
      if (!connectedClients.has(userId)) {
        connectedClients.set(userId, []);
      }
      connectedClients.get(userId).push({
        socketId: socket.id,
        role: userRole,
        connectedAt: new Date()
      });
    }

    // Join role-based room for targeted broadcasts
    if (userRole) {
      socket.join(`role:${userRole}`);
    }

    // Join user-specific room
    if (userId) {
      socket.join(`user:${userId}`);
    }

    // Test event - client can send 'test' to verify connection
    socket.on('test', (data) => {
      console.log(`[WebSocket] Test received from ${socket.id}:`, data);
      socket.emit('test_response', {
        message: 'Connection successful!',
        timestamp: new Date().toISOString(),
        received: data
      });
    });

    // Subscribe to real-time updates based on role
    socket.on('subscribe', (channels) => {
      console.log(`[WebSocket] ${socket.id} subscribing to:`, channels);
      if (Array.isArray(channels)) {
        channels.forEach(channel => {
          socket.join(channel);
        });
      } else if (typeof channels === 'string') {
        socket.join(channels);
      }
    });

    // Unsubscribe from channels
    socket.on('unsubscribe', (channels) => {
      console.log(`[WebSocket] ${socket.id} unsubscribing from:`, channels);
      if (Array.isArray(channels)) {
        channels.forEach(channel => {
          socket.leave(channel);
        });
      } else if (typeof channels === 'string') {
        socket.leave(channels);
      }
    });

    // Handle disconnection
    socket.on('disconnect', (reason) => {
      console.log(`[WebSocket] Client disconnected: ${socket.id} (Reason: ${reason})`);
      
      if (userId && connectedClients.has(userId)) {
        const userSockets = connectedClients.get(userId);
        const index = userSockets.findIndex(client => client.socketId === socket.id);
        if (index !== -1) {
          userSockets.splice(index, 1);
        }
        if (userSockets.length === 0) {
          connectedClients.delete(userId);
        }
      }
    });

    // Send connection confirmation
    socket.emit('connected', {
      socketId: socket.id,
      userId: userId,
      role: userRole,
      timestamp: new Date().toISOString()
    });
  });

  // Helper function to broadcast to specific role
  const broadcastToRole = (role, event, data) => {
    io.to(`role:${role}`).emit(event, data);
  };

  // Helper function to send to specific user
  const sendToUser = (userId, event, data) => {
    io.to(`user:${userId}`).emit(event, data);
  };

  // Helper function to broadcast to channel
  const broadcastToChannel = (channel, event, data) => {
    io.to(channel).emit(event, data);
  };

  // Export helper functions for use in routes
  return {
    broadcastToRole,
    sendToUser,
    broadcastToChannel,
    io
  };
};

// Export helper functions for use in other files
module.exports.broadcastToRole = (io, role, event, data) => {
  io.to(`role:${role}`).emit(event, data);
};

module.exports.sendToUser = (io, userId, event, data) => {
  io.to(`user:${userId}`).emit(event, data);
};

module.exports.broadcastToChannel = (io, channel, event, data) => {
  io.to(channel).emit(event, data);
};


const { WebSocketServer } = require('ws');
const http = require('http');

const PORT = process.env.PORT || 8080;
const server = http.createServer((req, res) => {
    res.writeHead(200, { 'Content-Type': 'text/plain' });
    res.end('AI Fit Tracker Synchronization Server\n');
});

const wss = new WebSocketServer({ server });

// In-memory states
const rooms = {}; // roomId -> Array of { ws, username, reps, state }
const roomMetadata = {}; // roomId -> { id, name, hostName, exerciseType, maxParticipants, participantCount }
const chats = {}; // coachId <-> traineeId chat history
let leaderboard = [
    { username: "@gym_bro", score: 1420 },
    { username: "@yoga_mind", score: 950 },
    { username: "@plank_master", score: 2100 },
    { username: "@fit_queen", score: 1850 },
    { username: "@iron_man", score: 1200 }
];

function log(level, message, ...args) {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] [${level}]`, message, ...args);
}

log('INFO', `Starting real-time server on port ${PORT}...`);

// Setup heartbeat ping interval
const interval = setInterval(() => {
    wss.clients.forEach((ws) => {
        if (ws.isAlive === false) {
            log('WARN', `Terminating inactive connection for user: ${ws.currentUser || 'unknown'}`);
            return ws.terminate();
        }
        ws.isAlive = false;
        try {
            ws.ping();
        } catch (err) {
            log('ERROR', 'Error sending ping to client:', err);
        }
    });
}, 30000);

wss.on('close', () => {
    clearInterval(interval);
});

wss.on('connection', (ws) => {
    ws.isAlive = true;
    let currentUser = null;
    let currentRoomId = null;

    log('INFO', 'New client connected.');

    ws.on('pong', () => {
        ws.isAlive = true;
    });

    ws.on('error', (error) => {
        log('ERROR', `WebSocket error for ${currentUser || 'unknown'}:`, error);
    });

    ws.on('message', (messageBuffer) => {
        try {
            if (!messageBuffer) {
                log('WARN', 'Received empty message.');
                return;
            }
            const message = JSON.parse(messageBuffer.toString());
            log('INFO', `Received action: ${message.action}`, message);

            switch (message.action) {
                case 'join_room': {
                    const { roomId, username } = message;
                    currentUser = username;
                    ws.currentUser = username; // Attach to ws instance for heartbeat tracking
                    currentRoomId = roomId;

                    if (!rooms[roomId]) {
                        rooms[roomId] = [];
                    }

                    // Remove if already exists in this room
                    rooms[roomId] = rooms[roomId].filter(u => u.username !== username);

                    // Add new user
                    rooms[roomId].push({ ws, username, reps: 0, state: "READY" });

                    log('INFO', `${username} joined room ${roomId}`);

                    // Update participant count
                    if (roomMetadata[roomId]) {
                        roomMetadata[roomId].participantCount = rooms[roomId].length;
                    } else {
                        roomMetadata[roomId] = {
                            id: roomId,
                            name: `Room ${roomId.slice(0, 4)}`,
                            hostName: username,
                            exerciseType: 'SQUAT',
                            maxParticipants: 4,
                            participantCount: rooms[roomId].length
                        };
                    }
                    broadcastRoomsList();

                    // Notify everyone in the room
                    broadcastToRoom(roomId, {
                        type: 'room_update',
                        users: rooms[roomId].map(u => ({ username: u.username, reps: u.reps, state: u.state }))
                    });
                    break;
                }

                case 'create_room': {
                    const { roomId, name, hostName, exerciseType, maxParticipants } = message;
                    roomMetadata[roomId] = {
                        id: roomId,
                        name: name,
                        hostName: hostName,
                        exerciseType: exerciseType,
                        maxParticipants: maxParticipants,
                        participantCount: 1
                    };
                    log('INFO', `Room created: ${name} (ID: ${roomId}) by ${hostName}`);
                    broadcastRoomsList();
                    break;
                }

                case 'get_rooms': {
                    sendRoomsList(ws);
                    break;
                }

                case 'rep_update': {
                    const { reps, state } = message;
                    if (currentRoomId && currentUser && rooms[currentRoomId]) {
                        const user = rooms[currentRoomId].find(u => u.username === currentUser);
                        if (user) {
                            user.reps = reps;
                            user.state = state;
                            log('INFO', `Rep update: ${currentUser} in room ${currentRoomId} now has ${reps} reps (${state})`);

                            broadcastToRoom(currentRoomId, {
                                type: 'room_update',
                                users: rooms[currentRoomId].map(u => ({ username: u.username, reps: u.reps, state: u.state }))
                            });
                        }
                    }
                    break;
                }

                case 'leave_room': {
                    handleLeaveRoom();
                    break;
                }

                case 'send_chat': {
                    const { sender, messageText, timestamp } = message;
                    log('INFO', `Chat from ${sender}: "${messageText}"`);

                    // Broadcast chat message back to the sender safely
                    try {
                        if (ws.readyState === 1) {
                            ws.send(JSON.stringify({
                                type: 'chat_receive',
                                sender: sender,
                                messageText: messageText,
                                timestamp: timestamp
                            }));
                        }
                    } catch (err) {
                        log('ERROR', 'Error sending chat response back to sender:', err);
                    }

                    // Simulated PT Coach auto-response
                    if (sender !== '@pt_coach') {
                        setTimeout(() => {
                            try {
                                const botResponse = getSmartCoachResponse(messageText);
                                if (ws.readyState === 1) {
                                    ws.send(JSON.stringify({
                                        type: 'chat_receive',
                                        sender: '@pt_coach',
                                        messageText: botResponse,
                                        timestamp: Date.now()
                                    }));
                                }
                            } catch (err) {
                                log('ERROR', 'Error sending coach auto-response:', err);
                            }
                        }, 1500);
                    }
                    break;
                }

                case 'get_leaderboard': {
                    // Send leaderboard
                    try {
                        if (ws.readyState === 1) {
                            ws.send(JSON.stringify({
                                type: 'leaderboard_data',
                                data: leaderboard.sort((a, b) => b.score - a.score)
                            }));
                        }
                    } catch (err) {
                        log('ERROR', 'Error sending leaderboard data:', err);
                    }
                    break;
                }

                case 'submit_score': {
                    const { username, score } = message;
                    log('INFO', `Score submitted: ${username} -> ${score}`);
                    
                    const existingUser = leaderboard.find(u => u.username === username);
                    if (existingUser) {
                        existingUser.score = Math.max(existingUser.score, score);
                    } else {
                        leaderboard.push({ username, score });
                    }

                    // Send updated leaderboard to everyone requesting
                    try {
                        if (ws.readyState === 1) {
                            ws.send(JSON.stringify({
                                type: 'leaderboard_data',
                                data: leaderboard.sort((a, b) => b.score - a.score)
                            }));
                        }
                    } catch (err) {
                        log('ERROR', 'Error sending leaderboard update:', err);
                    }
                    break;
                }
            }
        } catch (err) {
            log('ERROR', 'Error parsing/handling incoming message:', err);
            // Optionally notify client about bad JSON
            try {
                if (ws.readyState === 1) {
                    ws.send(JSON.stringify({
                        type: 'error',
                        message: 'Malformed JSON payload'
                    }));
                }
            } catch (e) {
                // Ignore send error
            }
        }
    });

    ws.on('close', () => {
        log('INFO', `Client disconnected: ${currentUser || 'unknown'}`);
        handleLeaveRoom();
    });

    function handleLeaveRoom() {
        if (currentRoomId && currentUser && rooms[currentRoomId]) {
            rooms[currentRoomId] = rooms[currentRoomId].filter(u => u.username !== currentUser);
            log('INFO', `${currentUser} left room ${currentRoomId}`);

            if (rooms[currentRoomId].length === 0) {
                delete rooms[currentRoomId];
                delete roomMetadata[currentRoomId];
            } else {
                if (roomMetadata[currentRoomId]) {
                    roomMetadata[currentRoomId].participantCount = rooms[currentRoomId].length;
                }
                broadcastToRoom(currentRoomId, {
                    type: 'room_update',
                    users: rooms[currentRoomId].map(u => ({ username: u.username, reps: u.reps, state: u.state }))
                });
            }
            broadcastRoomsList();
            currentRoomId = null;
        }
    }
});

function broadcastToRoom(roomId, data) {
    if (rooms[roomId]) {
        try {
            const payload = JSON.stringify(data);
            rooms[roomId].forEach(user => {
                if (user.ws.readyState === 1) { // OPEN
                    try {
                        user.ws.send(payload);
                    } catch (err) {
                        log('ERROR', `Error broadcasting to ${user.username} in room ${roomId}:`, err);
                    }
                }
            });
        } catch (err) {
            log('ERROR', `Error encoding room data broadcast for room ${roomId}:`, err);
        }
    }
}

function broadcastRoomsList() {
    try {
        const data = JSON.stringify({
            type: 'rooms_list',
            data: Object.values(roomMetadata)
        });
        wss.clients.forEach(client => {
            if (client.readyState === 1) { // OPEN
                try {
                    client.send(data);
                } catch (err) {
                    log('ERROR', 'Error broadcasting rooms list to client:', err);
                }
            }
        });
    } catch (err) {
        log('ERROR', 'Error encoding rooms list broadcast:', err);
    }
}

function sendRoomsList(ws) {
    if (ws.readyState === 1) { // OPEN
        try {
            ws.send(JSON.stringify({
                type: 'rooms_list',
                data: Object.values(roomMetadata)
            }));
        } catch (err) {
            log('ERROR', 'Error sending rooms list to specific client:', err);
        }
    }
}

function getSmartCoachResponse(userMessage) {
    const msg = userMessage.toLowerCase();
    
    // Squat responses
    if (msg.includes('squat') || msg.includes('gánh đùi')) {
        return "Tư thế Squat của bạn hôm nay tốt lắm! Đùi đã đạt góc dưới 95 độ chưa? Nhớ đẩy hông ra sau và không để đầu gối vượt quá mũi chân nhé.\n\n" +
               "Your Squat form is looking great! Make sure your thighs go below 95 degrees. Remember to push your hips back and keep your knees behind your toes.";
    } 
    // Plank responses
    else if (msg.includes('plank') || msg.includes('tấm ván')) {
        return "Khi Plank hãy siết chặt cơ bụng và đùi. Lưng - hông - gót chân phải thẳng hàng. Nếu thấy mỏi hông hãy nghỉ 30s rồi tập tiếp.\n\n" +
               "During Plank, squeeze your core and glutes. Keep your back, hips, and heels aligned. If your hips feel fatigued, take a 30s rest.";
    } 
    // Push up responses
    else if (msg.includes('pushup') || msg.includes('push-up') || msg.includes('hít đất') || msg.includes('chống đẩy')) {
        return "Khi hít đất (Push-up), hãy chú ý giữ thân người thẳng như một tấm ván. Hạ người xuống sâu đến khi khuỷu tay tạo góc 90 độ rồi đẩy lên dứt khoát nhé.\n\n" +
               "Keep your body straight like a plank when doing push-ups. Lower yourself until elbows reach 90 degrees, then push back up explosively.";
    }
    // Jumping Jacks responses
    else if (msg.includes('jumping jack') || msg.includes('nhảy')) {
        return "Jumping Jacks là bài tập cardio rất tốt! Hãy tiếp đất nhẹ nhàng bằng mũi bàn chân để giảm tác động lên khớp gối nhé.\n\n" +
               "Jumping Jacks are fantastic cardio! Land softly on the balls of your feet to minimize impact on your knees.";
    }
    // Bicep Curl responses
    else if (msg.includes('bicep curl') || msg.includes('gập tay') || msg.includes('tạ tay')) {
        return "Khi gập tạ tay trước (Bicep Curl), giữ khuỷu tay cố định sát sườn và tập trung siết chặt cơ bắp ở đỉnh động tác. Tránh dùng đà để văng tạ.\n\n" +
               "During Bicep Curls, keep your elbows pinned to your sides and squeeze the biceps at the top. Avoid using momentum to swing the weights.";
    }
    // Soreness/Pain responses
    else if (msg.includes('mỏi') || msg.includes('đau') || msg.includes('hurt') || msg.includes('sore') || msg.includes('pain')) {
        return "Đó là phản ứng bình thường khi các sợi cơ được kích hoạt! Hãy nghỉ ngơi đầy đủ, bổ sung protein (Whey/ức gà) và giãn cơ sau buổi tập.\n\n" +
               "Muscle soreness is normal as your muscle fibers adapt! Get plenty of rest, consume enough protein, and stretch after your session.";
    } 
    // Greeting responses
    else if (msg.includes('chào') || msg.includes('hi') || msg.includes('hello') || msg.includes('chay')) {
        return "Chào bạn! Hôm nay kế hoạch tập luyện của bạn thế nào? Cần tôi tư vấn thêm về bài tập hay dinh dưỡng không?\n\n" +
               "Hello! How is your workout plan going today? Let me know if you need any advice on exercises or nutrition.";
    }
    // Nutrition responses
    else if (msg.includes('dinh dưỡng') || msg.includes('ăn') || msg.includes('nutrition') || msg.includes('eat') || msg.includes('diet')) {
        return "Dinh dưỡng chiếm 70% thành công! Hãy nạp đủ protein, bổ sung tinh bột hấp thu chậm (như khoai lang, yến mạch) và hạn chế mỡ xấu nhé.\n\n" +
               "Nutrition is 70% of success! Ensure adequate protein intake, focus on complex carbs (sweet potatoes, oats), and avoid unhealthy fats.";
    }
    // Default response
    else {
        return "Chào bạn! Tôi đã nhận được phản hồi của bạn. Hãy giữ thói quen tập luyện đều đặn và tiếp tục phát huy nhé!\n\n" +
               "Hello! I've received your message. Keep up the consistent training and stay active!";
    }
}

server.listen(PORT, '0.0.0.0', () => {
    log('INFO', `Real-time synchronization server is running on http://localhost:${PORT}`);
});

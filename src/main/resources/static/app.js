// Variables globales
let stompClient = null;
let brokerId = 'courtier1';
let autoScrollEnabled = true;
let stockSymbols = ['AAPL', 'GOOGL', 'MSFT', 'AMZN', 'TSLA', 'META', 'NVDA', 'NFLX', 'INTC', 'AMD'];

// Initialisation
window.onload = function() {
    brokerId = document.getElementById('brokerId').value;
    checkServerStatus();
    connectBroker();
    loadSubscriptions();
    
    // Auto-refresh subscriptions every 10 seconds
    setInterval(loadSubscriptions, 10000);
};

// Vérifier le statut du serveur
function checkServerStatus() {
    fetch('/api/stocks/test')
        .then(response => {
            if (response.ok) {
                document.getElementById('serverStatus').innerHTML = '🟢 Connecté';
                document.getElementById('serverStatus').style.color = '#10b981';
            }
        })
        .catch(() => {
            document.getElementById('serverStatus').innerHTML = '🔴 Hors ligne';
            document.getElementById('serverStatus').style.color = '#ef4444';
        });
}

// Connecter le courtier
function connectBroker() {
    brokerId = document.getElementById('brokerId').value.trim();
    if (!brokerId) {
        brokerId = 'courtier1';
        document.getElementById('brokerId').value = brokerId;
    }
    
    // Fermer la connexion existante
    if (stompClient !== null) {
        stompClient.disconnect();
    }
    
    // Créer une nouvelle connexion
    const socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, function(frame) {
        // Mettre à jour le statut
        document.getElementById('connectionStatus').innerHTML = 
            '<i class="fas fa-check-circle"></i> Connecté en tant que: ' + brokerId;
        document.getElementById('connectionStatus').className = 'status connected';
        
        addLog('✅ Connecté au serveur WebSocket');
        
        // S'abonner au canal personnel
        const personalChannel = '/topic/stocks/' + brokerId;
        stompClient.subscribe(personalChannel, function(message) {
            const stock = JSON.parse(message.body);
            displayStockPrice(stock);
            addLog(`📥 Reçu ${stock.symbol}: $${stock.currentPrice}`);
        });
        
        // S'abonner au canal public (optionnel)
        stompClient.subscribe('/topic/stocks/public', function(message) {
            const stock = JSON.parse(message.body);
            // Afficher uniquement si on y est abonné
            if (stockSymbols.includes(stock.symbol)) {
                displayStockPrice(stock);
            }
        });
        
        // Charger les abonnements
        loadSubscriptions();
        
    }, function(error) {
        console.error('Erreur WebSocket:', error);
        document.getElementById('connectionStatus').innerHTML = 
            '<i class="fas fa-times-circle"></i> Erreur de connexion';
        document.getElementById('connectionStatus').className = 'status disconnected';
        addLog('❌ Erreur de connexion WebSocket');
    });
}

// S'abonner à un symbole
function subscribe() {
    const symbol = document.getElementById('stockSymbol').value.trim().toUpperCase();
    if (!symbol) {
        alert('Veuillez entrer un symbole boursier');
        return;
    }
    
    fetch(`/api/brokers/${brokerId}/subscribe/${symbol}`, {
        method: 'POST'
    })
    .then(response => {
        if (response.ok) {
            addLog(`✅ Abonné à ${symbol}`);
            loadSubscriptions();
        } else {
            addLog(`❌ Erreur lors de l'abonnement à ${symbol}`);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        addLog('❌ Erreur réseau');
    });
}

// Se désabonner d'un symbole
function unsubscribe() {
    const symbol = document.getElementById('stockSymbol').value.trim().toUpperCase();
    if (!symbol) {
        alert('Veuillez entrer un symbole boursier');
        return;
    }
    
    fetch(`/api/brokers/${brokerId}/unsubscribe/${symbol}`, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            addLog(`❌ Désabonné de ${symbol}`);
            loadSubscriptions();
        }
    })
    .catch(error => {
        console.error('Error:', error);
        addLog('❌ Erreur réseau');
    });
}

// Abonnement rapide
function quickSubscribe(symbol) {
    document.getElementById('stockSymbol').value = symbol;
    subscribe();
}

// Charger les abonnements
function loadSubscriptions() {
    fetch(`/api/brokers/${brokerId}/subscriptions`)
        .then(response => response.json())
        .then(symbols => {
            let html = '<h3><i class="fas fa-star"></i> Vos Abonnements</h3>';
            
            if (symbols && symbols.length > 0) {
                html += '<div class="subscribed-symbols">';
                symbols.forEach(symbol => {
                    html += `<span class="symbol-tag">${symbol}</span>`;
                });
                html += '</div>';
            } else {
                html += '<p class="no-subscriptions">Vous n\'êtes abonné à aucun symbole</p>';
            }
            
            document.getElementById('subscriptionsList').innerHTML = html;
        })
        .catch(error => {
            console.error('Error loading subscriptions:', error);
            document.getElementById('subscriptionsList').innerHTML = 
                '<p class="error">Erreur de chargement des abonnements</p>';
        });
}

// Afficher un cours boursier
function displayStockPrice(stock) {
    const tbody = document.getElementById('stockPrices');
    
    // Vérifier si la ligne existe déjà
    let existingRow = document.getElementById(`row-${stock.symbol}`);
    
    if (existingRow) {
        // Mettre à jour la ligne existante
        existingRow.cells[1].textContent = `$${stock.currentPrice.toFixed(2)}`;
        existingRow.cells[2].textContent = stock.change >= 0 ? 
            `+${stock.change.toFixed(2)}` : `${stock.change.toFixed(2)}`;
        existingRow.cells[3].textContent = stock.changePercent >= 0 ? 
            `+${stock.changePercent.toFixed(2)}%` : `${stock.changePercent.toFixed(2)}%`;
        existingRow.cells[4].textContent = Math.round(stock.volume).toLocaleString();
        existingRow.cells[5].textContent = new Date(stock.timestamp).toLocaleTimeString();
        
        // Mettre à jour les classes de couleur
        const changeClass = stock.change >= 0 ? 'positive' : 'negative';
        existingRow.cells[2].className = changeClass;
        existingRow.cells[3].className = changeClass;
        
        // Effet de mise à jour
        existingRow.classList.add('new-price');
        setTimeout(() => existingRow.classList.remove('new-price'), 1000);
        
    } else {
        // Créer une nouvelle ligne
        const row = document.createElement('tr');
        row.id = `row-${stock.symbol}`;
        row.classList.add('new-price');
        
        const changeClass = stock.change >= 0 ? 'positive' : 'negative';
        const changeSign = stock.change >= 0 ? '+' : '';
        
        row.innerHTML = `
            <td><strong>${stock.symbol}</strong></td>
            <td><strong>$${stock.currentPrice.toFixed(2)}</strong></td>
            <td class="${changeClass}">${changeSign}${stock.change.toFixed(2)}</td>
            <td class="${changeClass}">${changeSign}${stock.changePercent.toFixed(2)}%</td>
            <td>${Math.round(stock.volume).toLocaleString()}</td>
            <td>${new Date(stock.timestamp).toLocaleTimeString()}</td>
            <td><span class="status-badge connected">Actif</span></td>
        `;
        
        // Ajouter en haut du tableau
        tbody.insertBefore(row, tbody.firstChild);
    }
    
    // Auto-scroll
    if (autoScrollEnabled) {
        tbody.parentNode.scrollTop = 0;
    }
}

// Basculer l'auto-scroll
function toggleAutoScroll() {
    autoScrollEnabled = !autoScrollEnabled;
    const btn = document.getElementById('autoScrollBtn');
    btn.innerHTML = autoScrollEnabled ? 
        '<i class="fas fa-scroll"></i> Auto-scroll: ON' : 
        '<i class="fas fa-scroll"></i> Auto-scroll: OFF';
}

// Effacer tous les cours
function clearPrices() {
    document.getElementById('stockPrices').innerHTML = '';
    addLog('🧹 Tous les cours ont été effacés');
}

// Ajouter un message au journal
function addLog(message) {
    const logsDiv = document.getElementById('logs');
    const logEntry = document.createElement('div');
    logEntry.innerHTML = `[${new Date().toLocaleTimeString()}] ${message}`;
    logsDiv.prepend(logEntry);
    
    // Limiter à 50 messages
    if (logsDiv.children.length > 50) {
        logsDiv.removeChild(logsDiv.lastChild);
    }
}
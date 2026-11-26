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
    
    if (!/^[A-Za-z0-9_-]{1,64}$/.test(brokerId)) {
        addLog('Identifiant invalide: lettres, chiffres, tirets et underscores uniquement.');
        return;
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
        document.getElementById('connectionStatus').textContent = 'Connecté en tant que: ' + brokerId;
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
    
    fetch(`/api/brokers/${encodeURIComponent(brokerId)}/subscribe/${encodeURIComponent(symbol)}`, {
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
    
    fetch(`/api/brokers/${encodeURIComponent(brokerId)}/unsubscribe/${encodeURIComponent(symbol)}`, {
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
    fetch(`/api/brokers/${encodeURIComponent(brokerId)}/subscriptions`)
        .then(response => response.json())
        .then(symbols => {
            renderSubscriptions(symbols);
        })
        .catch(error => {
            console.error('Error loading subscriptions:', error);
            document.getElementById('subscriptionsList').innerHTML = 
                '<p class="error">Erreur de chargement des abonnements</p>';
        });
}

// Les valeurs reçues sont toujours insérées comme texte, jamais comme HTML.
function renderSubscriptions(symbols) {
    const list = document.getElementById('subscriptionsList');
    list.replaceChildren();
    const heading = document.createElement('h3');
    heading.textContent = 'Vos Abonnements';
    list.appendChild(heading);
    const group = document.createElement('div');
    group.className = 'subscribed-symbols';
    for (const symbol of Array.isArray(symbols) ? symbols : []) {
        const tag = document.createElement('span');
        tag.className = 'symbol-tag';
        tag.textContent = String(symbol);
        group.appendChild(tag);
    }
    if (group.children.length) {
        list.appendChild(group);
    } else {
        const empty = document.createElement('p');
        empty.className = 'no-subscriptions';
        empty.textContent = "Vous n'êtes abonné à aucun symbole";
        list.appendChild(empty);
    }
}

function displayStockPrice(stock) {
    if (!stock || typeof stock.symbol !== 'string' ||
        !/^[A-Z][A-Z0-9.-]{0,14}$/.test(stock.symbol) ||
        !['currentPrice', 'change', 'changePercent', 'volume'].every(
            key => Number.isFinite(stock[key])) ||
        stock.currentPrice <= 0 || stock.volume < 0) return;
    const tbody = document.getElementById('stockPrices');
    let row = document.getElementById(`row-${stock.symbol}`);
    if (!row) {
        row = document.createElement('tr');
        row.id = `row-${stock.symbol}`;
        for (let i = 0; i < 7; i++) row.appendChild(document.createElement('td'));
        tbody.insertBefore(row, tbody.firstChild);
    }
    const sign = stock.change >= 0 ? '+' : '';
    const percentSign = stock.changePercent >= 0 ? '+' : '';
    const values = [
        stock.symbol, `$${stock.currentPrice.toFixed(2)}`,
        `${sign}${stock.change.toFixed(2)}`,
        `${percentSign}${stock.changePercent.toFixed(2)}%`,
        Math.round(stock.volume).toLocaleString(),
        new Date(stock.timestamp).toLocaleTimeString(), 'Actif',
    ];
    values.forEach((value, index) => { row.cells[index].textContent = value; });
    row.cells[2].className = stock.change >= 0 ? 'positive' : 'negative';
    row.cells[3].className = stock.changePercent >= 0 ? 'positive' : 'negative';
    row.classList.add('new-price');
    setTimeout(() => row.classList.remove('new-price'), 1000);
    if (autoScrollEnabled) tbody.parentNode.scrollTop = 0;
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
    logEntry.textContent = `[${new Date().toLocaleTimeString()}] ${message}`;
    logsDiv.prepend(logEntry);
    
    // Limiter à 50 messages
    if (logsDiv.children.length > 50) {
        logsDiv.removeChild(logsDiv.lastChild);
    }
}
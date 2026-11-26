const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

// Minimal DOM fixture: HTML parsing is forbidden so dynamic sinks fail the test.
function dashboard() {
    const elements = [];
    class Element {
        constructor(tag) {
            this.tagName = tag;
            this.children = [];
            this._text = '';
            this.classList = { add() {}, remove() {} };
            this.style = {};
            elements.push(this);
        }
        set innerHTML(value) { throw new Error('Unexpected HTML insertion: ' + value); }
        set textContent(value) { this._text = String(value); this.children = []; }
        get textContent() { return this._text + this.children.map(e => e.textContent).join(''); }
        get cells() { return this.children; }
        get firstChild() { return this.children[0] || null; }
        get lastChild() { return this.children.at(-1) || null; }
        appendChild(child) { child.parentNode = this; this.children.push(child); return child; }
        prepend(child) { child.parentNode = this; this.children.unshift(child); }
        insertBefore(child) { this.prepend(child); }
        replaceChildren() { this.children = []; this._text = ''; }
        removeChild(child) { this.children = this.children.filter(e => e !== child); }
    }
    const document = {
        createElement: tag => new Element(tag),
        getElementById: id => elements.find(e => e.id === id) || null
    };
    for (const id of ['logs', 'subscriptionsList', 'stockPrices', 'brokerId', 'stockSymbol']) {
        const element = new Element('div');
        element.id = id;
        element.value = '';
    }
    new Element('div').appendChild(document.getElementById('stockPrices'));
    const requests = [];
    const context = vm.createContext({
        document, window: {}, console, setTimeout() {}, setInterval() {}, alert() {},
        fetch(url) { requests.push(url); return Promise.resolve({ ok: false }); }
    });
    vm.runInContext(fs.readFileSync('src/main/resources/static/app.js', 'utf8'), context);
    return { context, document, requests };
}

test('hostile log content remains literal text', () => {
    const { context, document } = dashboard();
    const payload = '<img src=x onerror=alert(1)>';
    context.addLog(payload);
    const entry = document.getElementById('logs').firstChild;
    assert.ok(entry.textContent.endsWith(payload));
    assert.equal(entry.children.length, 0);
});

test('subscription names are rendered as text nodes', () => {
    const { context, document } = dashboard();
    context.renderSubscriptions(['<svg onload=alert(1)>']);
    const tag = document.getElementById('subscriptionsList').children[1].firstChild;
    assert.equal(tag.textContent, '<svg onload=alert(1)>');
    assert.equal(tag.children.length, 0);
});

test('malformed price messages do not create rows or throw', () => {
    const { context, document } = dashboard();
    const valid = { symbol: 'AAPL', currentPrice: 10, change: 1, changePercent: 1, volume: 1 };
    for (const stock of [null, {}, { ...valid, symbol: '<img>' },
            { ...valid, currentPrice: Infinity }, { ...valid, volume: -1 }]) {
        assert.doesNotThrow(() => context.displayStockPrice(stock));
    }
    assert.equal(document.getElementById('stockPrices').children.length, 0);
});

test('valid prices update an existing row and use each changes own sign', () => {
    const { context, document } = dashboard();
    const stock = { symbol: 'AAPL', currentPrice: 10, change: -1,
        changePercent: 2, volume: 1, timestamp: '2026-01-01T12:00:00' };
    context.displayStockPrice(stock);
    context.displayStockPrice({ ...stock, currentPrice: 12 });
    const table = document.getElementById('stockPrices');
    assert.equal(table.children.length, 1);
    assert.equal(table.firstChild.cells[0].textContent, 'AAPL');
    assert.equal(table.firstChild.cells[1].textContent, '$12.00');
    assert.equal(table.firstChild.cells[3].textContent, '+2.00%');
    assert.equal(table.firstChild.cells[3].className, 'positive');
});

test('hostile broker IDs are rejected before opening a socket', () => {
    const { context, document } = dashboard();
    document.getElementById('brokerId').value = '<img src=x onerror=alert(1)>';
    assert.doesNotThrow(() => context.connectBroker());
    assert.ok(document.getElementById('logs').textContent.includes('Identifiant invalide'));
});

test('subscription input cannot inject URL query or fragment components', () => {
    const { context, document, requests } = dashboard();
    document.getElementById('stockSymbol').value = 'AAPL?x=1#frag';
    context.subscribe();
    assert.equal(requests[0],
        '/api/brokers/courtier1/subscribe/AAPL%3FX%3D1%23FRAG');
});

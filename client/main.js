const socket = new WebSocket('ws://localhost:8080/play');
let nickname = prompt('Enter nickname') || 'player';
let pingStart = 0;

const app = new PIXI.Application({ background: '#000000', resizeTo: window });
document.body.appendChild(app.view);

const players = new Map();

socket.addEventListener('open', () => {
    socket.send(JSON.stringify({ nick: nickname }));
    document.getElementById('nickname').textContent = nickname;
});

socket.addEventListener('message', e => {
    const msg = JSON.parse(e.data);
    if (msg.players) {
        updatePlayers(msg.players);
    }
});

function updatePlayers(list) {
    list.forEach(p => {
        let sprite = players.get(p.id);
        if (!sprite) {
            sprite = createPlayerSprite(p);
            players.set(p.id, sprite);
            app.stage.addChild(sprite.container);
        }
        sprite.target = p;
    });
    // remove missing players
    players.forEach((sprite, id) => {
        if (!list.find(p => p.id === id)) {
            app.stage.removeChild(sprite.container);
            players.delete(id);
        }
    });
    const me = list.find(p => p.nickname === nickname);
    if (me) document.getElementById('mass').textContent = me.mass.toFixed(1);
}

function createPlayerSprite(p) {
    const container = new PIXI.Container();
    const body = new PIXI.Graphics();
    body.beginFill(p.color);
    body.drawCircle(0, 0, p.mass);
    body.endFill();
    container.addChild(body);
    const core = new PIXI.Graphics();
    core.beginFill(0xffffff);
    core.drawCircle(0, 0, p.mass * 0.4);
    core.endFill();
    container.addChild(core);
    return { container, body, core, target: p };
}

function lerp(a, b, t) {
    return a + (b - a) * t;
}

app.ticker.add((delta) => {
    players.forEach(sprite => {
        const p = sprite.target;
        if (!p) return;
        sprite.body.clear();
        sprite.body.beginFill(p.color);
        sprite.body.drawCircle(0, 0, p.mass);
        sprite.body.endFill();
        sprite.core.visible = p.hasCore;
        sprite.core.clear();
        sprite.core.beginFill(0xffffff);
        sprite.core.drawCircle(0, 0, p.mass * 0.4);
        sprite.core.endFill();
        sprite.container.x = lerp(sprite.container.x, p.x, 0.1);
        sprite.container.y = lerp(sprite.container.y, p.y, 0.1);
    });
});

const keys = {};
window.addEventListener('keydown', e => { keys[e.code] = true; sendInput(); });
window.addEventListener('keyup', e => { keys[e.code] = false; sendInput(); });

function sendInput() {
    const msg = {
        up: keys['KeyW'] || false,
        down: keys['KeyS'] || false,
        left: keys['KeyA'] || false,
        right: keys['KeyD'] || false,
        split: keys['Space'] || false
    };
    socket.send(JSON.stringify(msg));
}

setInterval(() => {
    pingStart = Date.now();
    socket.send(JSON.stringify({}));
}, 1000);

socket.addEventListener('message', () => {
    const ping = Date.now() - pingStart;
    document.getElementById('ping').textContent = ping;
});

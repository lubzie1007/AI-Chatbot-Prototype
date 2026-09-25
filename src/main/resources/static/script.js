const messagesEl = document.getElementById('messages');
const form = document.getElementById('chat-form');
const input = document.getElementById('input');
const suggestions = document.getElementById('suggestions');

// Basic session ID so backend can eventually track conversations
const sessionId = crypto.randomUUID();

function addMessage(role, text) {
  const wrap = document.createElement('div');
  wrap.className = `msg ${role}`;

  const label = document.createElement('div');
  label.className = 'label';
  label.textContent = role === 'user' ? 'ME' : 'OUR AI';

  const bubble = document.createElement('div');
  bubble.className = 'bubble';
  bubble.textContent = text;

  wrap.appendChild(label);
  wrap.appendChild(bubble);
  messagesEl.appendChild(wrap);
  messagesEl.scrollTop = messagesEl.scrollHeight;
  return bubble;
}

function addLoading() {
  const wrap = document.createElement('div');
  wrap.className = 'msg bot loading';

  const label = document.createElement('div');
  label.className = 'label';
  label.textContent = 'OUR AI';

  const bubble = document.createElement('div');
  bubble.className = 'bubble';
  bubble.innerHTML = `<span class="dots"><span></span><span></span><span></span></span>`;

  wrap.appendChild(label);
  wrap.appendChild(bubble);
  messagesEl.appendChild(wrap);
  messagesEl.scrollTop = messagesEl.scrollHeight;
  return wrap;
}

async function sendMessage(text) {
  if (!text.trim()) return;

  suggestions.classList.add('hidden');
  addMessage('user', text);
  const loadingEl = addLoading();

  try {
    const res = await fetch('/api/chat', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ message: text, sessionId })
    });

    const data = await res.json();
    loadingEl.remove();
    addMessage('bot', data.reply || 'No reply received.');
  } catch (err) {
    loadingEl.remove();
    addMessage('bot', 'Connection error: ' + err.message);
  }
}

form.addEventListener('submit', (e) => {
  e.preventDefault();
  const text = input.value;
  input.value = '';
  sendMessage(text);
});

document.querySelectorAll('.chip').forEach(chip => {
  chip.addEventListener('click', () => sendMessage(chip.dataset.msg));
});

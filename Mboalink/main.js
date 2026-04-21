function runAI(lang) {
  const trade = document.getElementById('aiTrade').value;
  const box   = document.getElementById('aiResult');
  box.style.display = 'block';
  box.innerHTML = '<p style="color:#777;padding:8px">Searching...</p>';

  fetch('/api/ai-match?trade=' + encodeURIComponent(trade))
    .then(r => r.json())
    .then(data => {
      if (!data.length) {
        box.innerHTML = '<p style="color:#b71c1c;padding:8px">No workers found for this trade.</p>';
        return;
      }
      box.innerHTML = data.map((m, i) => {
        const medal = i === 0 ? '🥇' : i === 1 ? '🥈' : '🥉';
        const reasons = m.reasons.map(r =>
          `<span style="background:#f0f4f0;color:#1a5c1a;padding:2px 8px;border-radius:12px;font-size:11px;margin-right:3px">${r}</span>`
        ).join('');
        return `
          <div style="padding:10px 0;border-bottom:1px solid #eee;display:flex;justify-content:space-between;align-items:flex-start;gap:10px">
            <div>
              <a href="/worker/${m.id}" style="color:#1a5c1a;font-weight:${i===0?'bold':'normal'};font-size:14px">
                ${medal} ${m.name} — ${m.trade}
              </a>
              <div style="font-size:12px;color:#777;margin-top:2px">
                📍 ${m.city} &nbsp;|&nbsp; ${m.rating} ★ &nbsp;|&nbsp; ${m.status}
              </div>
              <div style="margin-top:4px">${reasons}</div>
            </div>
            <span style="background:#1a5c1a;color:#fff;padding:3px 10px;border-radius:12px;font-size:12px;white-space:nowrap">Score ${m.score}</span>
          </div>`;
      }).join('');
    })
    .catch(() => { box.innerHTML = '<p style="color:#b71c1c;padding:8px">Network error.</p>'; });
}

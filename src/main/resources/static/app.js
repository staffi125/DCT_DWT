/* ── DOM handles ─────────────────────────────────────────────────── */
const fileInput  = document.getElementById('file');
const keepInput  = document.getElementById('keep');
const keepLabel  = document.getElementById('keepLabel');
const statusEl   = document.getElementById('status');
const expSection = document.getElementById('exp-section');

keepInput.addEventListener('input', () => {
    keepLabel.textContent = `${keepInput.value}%`;
});

document.getElementById('compress').addEventListener('click',
    () => run('/api/compress', true, document.getElementById('compress')));
document.getElementById('experiments').addEventListener('click',
    () => run('/api/experiments', false, document.getElementById('experiments')));

/* ── Network ─────────────────────────────────────────────────────── */
function buildFormData() {
    const data = new FormData();
    if (fileInput.files[0]) data.append('file', fileInput.files[0]);
    data.append('keepPercent', keepInput.value);
    return data;
}

async function run(url, isCompare, btn) {
    setBusy(btn, true);
    statusEl.textContent = 'Обчислення…';
    try {
        const res = await fetch(url, { method: 'POST', body: buildFormData() });
        if (!res.ok) throw new Error(`HTTP ${res.status}`);
        const json = await res.json();
        if (isCompare) {
            renderCompare(json);
            expSection.hidden = true;
        } else {
            renderExperiments(json);
        }
        statusEl.textContent = 'Готово.';
    } catch (err) {
        statusEl.textContent = `Помилка: ${err.message}`;
    } finally {
        setBusy(btn, false);
    }
}

function setBusy(btn, busy) {
    btn.disabled = busy;
    btn.querySelector('.btn-text').hidden = busy;
    btn.querySelector('.spinner').hidden  = !busy;
}

/* ── Single compare ──────────────────────────────────────────────── */
function renderCompare(data) {
    setImg('img-original', data.originalPng);
    setImg('img-dct',      data.dctPng);
    setImg('img-dwt',      data.dwtPng);
    document.getElementById('meta-dct').textContent = fmtMeta(data.dct);
    document.getElementById('meta-dwt').textContent = fmtMeta(data.dwt);
}

/* ── Experiments ─────────────────────────────────────────────────── */
function renderExperiments(data) {
    expSection.hidden = false;

    /* Visual rows */
    const visual = document.getElementById('exp-visual');
    visual.innerHTML = '';
    for (const level of data.levels) {
        const pct = level.keepPercent.toFixed(0);
        const block = document.createElement('div');
        block.className = 'exp-level';
        block.innerHTML = `
          <h3 class="exp-level-title">Рівень стиснення — збережено ${pct}% коефіцієнтів</h3>
          <div class="compare exp-compare">
            <figure>
              <figcaption>Original</figcaption>
              <img src="data:image/png;base64,${level.originalPng}" alt="Original ${pct}%">
            </figure>
            <figure>
              <figcaption>DCT — PSNR: ${fmtPsnr(level.dct.psnr)} дБ</figcaption>
              <img src="data:image/png;base64,${level.dctPng}" alt="DCT ${pct}%">
              <div class="meta">${fmtMeta(level.dct)}</div>
            </figure>
            <figure>
              <figcaption>DWT (Haar) — PSNR: ${fmtPsnr(level.dwt.psnr)} дБ</figcaption>
              <img src="data:image/png;base64,${level.dwtPng}" alt="DWT ${pct}%">
              <div class="meta">${fmtMeta(level.dwt)}</div>
            </figure>
          </div>`;
        visual.appendChild(block);
    }

    /* Table */
    const tbody = document.getElementById('exp-body');
    tbody.innerHTML = '';
    for (const level of data.levels) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>${level.keepPercent.toFixed(0)}</td>
          <td class="${psnrClass(level.dct.psnr, level.dwt.psnr)}">${fmtPsnr(level.dct.psnr)}</td>
          <td>${level.dct.keptSlots} / ${level.dct.coefficientRatio.toFixed(2)}</td>
          <td class="${psnrClass(level.dwt.psnr, level.dct.psnr)}">${fmtPsnr(level.dwt.psnr)}</td>
          <td>${level.dwt.keptSlots} / ${level.dwt.coefficientRatio.toFixed(2)}</td>`;
        tbody.appendChild(tr);
    }

    /* Conclusions */
    document.getElementById('conclusions').textContent = data.conclusions;

    /* Chart */
    drawPsnrChart(data.levels);
}

/* ── PSNR grouped bar chart (plain Canvas 2D) ────────────────────── */
function drawPsnrChart(levels) {
    const canvas = document.getElementById('psnr-chart');
    canvas.width  = canvas.parentElement.clientWidth || 700;
    const W = canvas.width, H = canvas.height;
    const ctx = canvas.getContext('2d');

    const PAD = { top: 20, right: 20, bottom: 48, left: 56 };
    const chartW = W - PAD.left - PAD.right;
    const chartH = H - PAD.top  - PAD.bottom;

    /* Determine Y axis range */
    const allPsnr = levels.flatMap(l => [l.dct.psnr, l.dwt.psnr]).filter(Number.isFinite);
    const minV = Math.max(0, Math.floor(Math.min(...allPsnr) - 4));
    const maxV = Math.ceil(Math.max(...allPsnr) + 4);

    ctx.clearRect(0, 0, W, H);

    /* Grid & Y axis */
    ctx.strokeStyle = '#2c3340';
    ctx.fillStyle   = '#9aa3b2';
    ctx.font        = '12px "Segoe UI", system-ui, sans-serif';
    ctx.textAlign   = 'right';
    const gridLines = 5;
    for (let i = 0; i <= gridLines; i++) {
        const v  = minV + (maxV - minV) * i / gridLines;
        const yy = PAD.top + chartH - (chartH * (v - minV) / (maxV - minV));
        ctx.beginPath();
        ctx.moveTo(PAD.left, yy);
        ctx.lineTo(PAD.left + chartW, yy);
        ctx.stroke();
        ctx.fillText(v.toFixed(1), PAD.left - 6, yy + 4);
    }

    /* Y label */
    ctx.save();
    ctx.translate(14, PAD.top + chartH / 2);
    ctx.rotate(-Math.PI / 2);
    ctx.textAlign = 'center';
    ctx.fillStyle = '#e8e6e1';
    ctx.fillText('PSNR, дБ', 0, 0);
    ctx.restore();

    /* Bars */
    const colors = { dct: '#7cb8ff', dwt: '#8fd6a8' };
    const n      = levels.length;
    const groupW = chartW / n;
    const barW   = groupW * 0.28;
    const gap    = groupW * 0.06;

    levels.forEach((level, gi) => {
        const groupX = PAD.left + gi * groupW + groupW / 2;
        const bars   = [
            { val: level.dct.psnr, color: colors.dct, label: 'DCT' },
            { val: level.dwt.psnr, color: colors.dwt, label: 'DWT' },
        ];
        bars.forEach((bar, bi) => {
            const x   = groupX + (bi - 0.5) * (barW + gap);
            const val = Number.isFinite(bar.val) ? bar.val : maxV;
            const bH  = chartH * (val - minV) / (maxV - minV);
            const y   = PAD.top + chartH - bH;

            ctx.fillStyle = bar.color;
            ctx.beginPath();
            ctx.roundRect(x, y, barW, bH, [4, 4, 0, 0]);
            ctx.fill();

            /* Value label above bar */
            ctx.fillStyle   = '#e8e6e1';
            ctx.textAlign   = 'center';
            ctx.font        = '11px "Segoe UI", system-ui, sans-serif';
            ctx.fillText(fmtPsnr(bar.val), x + barW / 2, y - 5);
        });

        /* X axis label */
        ctx.fillStyle = '#9aa3b2';
        ctx.textAlign = 'center';
        ctx.font      = '12px "Segoe UI", system-ui, sans-serif';
        ctx.fillText(`${level.keepPercent.toFixed(0)}%`, groupX, PAD.top + chartH + 18);
    });

    /* Legend */
    const legendY = PAD.top + chartH + 36;
    [['DCT', colors.dct], ['DWT (Haar)', colors.dwt]].forEach(([label, color], i) => {
        const lx = PAD.left + 20 + i * 130;
        ctx.fillStyle = color;
        ctx.fillRect(lx, legendY - 10, 14, 14);
        ctx.fillStyle = '#e8e6e1';
        ctx.textAlign = 'left';
        ctx.font      = '12px "Segoe UI", system-ui, sans-serif';
        ctx.fillText(label, lx + 18, legendY + 2);
    });
}

/* ── Helpers ─────────────────────────────────────────────────────── */
function setImg(id, b64) {
    document.getElementById(id).src = `data:image/png;base64,${b64}`;
}

function fmtMeta(m) {
    return `PSNR: ${fmtPsnr(m.psnr)} дБ
Збережено: ${m.keptSlots} з ${m.totalCoefficients} (${m.keptPercent.toFixed(2)}%)
Ненульових: ${m.nonZero}
Ratio N/K: ${m.coefficientRatio.toFixed(2)}`;
}

function fmtPsnr(v) {
    return Number.isFinite(v) ? v.toFixed(2) : '∞';
}

function psnrClass(a, b) {
    return a >= b ? 'cell-win' : '';
}

/* Auto-load on open */
run('/api/compress', true, document.getElementById('compress'));


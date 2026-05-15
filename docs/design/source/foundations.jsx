// MoneyM Foundation Cards
// Typography, color tokens, category colors, spacing & radii.

// ─── Aesthetic Principles ────────────────────────────────────
function PrinciplesCard() {
  const principles = [
    { n: '01', t: 'Neutral first', d: 'Black, white, gray. The interface itself is silent — content does the talking.' },
    { n: '02', t: 'Color only for category', d: 'Color is reserved for category identity. Used as small dots and chip backgrounds, never as decoration.' },
    { n: '03', t: 'Green for affirm only', d: 'A single accent green marks "save" and income — never used for surfaces or chrome.' },
    { n: '04', t: 'Platform agnostic', d: 'No Material lozenges, ripples, or floating action buttons. Patterns that read native on both iOS and Android.' },
    { n: '05', t: 'Monospaced money', d: 'All currency values use Geist Mono with tabular figures — numbers align in columns.' },
  ];
  return (
    <div className="foundation-card" style={{ width: '100%', height: '100%' }}>
      <h2>Aesthetic Principles</h2>
      <div style={{ display: 'grid', gap: 24 }}>
        {principles.map(p => (
          <div key={p.n} style={{ display: 'grid', gridTemplateColumns: '52px 1fr', gap: 16 }}>
            <div className="mm-mono" style={{ fontSize: 13, color: '#A3A3A3', fontWeight: 500, paddingTop: 2 }}>{p.n}</div>
            <div>
              <div style={{ fontSize: 17, fontWeight: 600, letterSpacing: -0.2, marginBottom: 4 }}>{p.t}</div>
              <div style={{ fontSize: 14, lineHeight: 1.5, color: '#6B6B6B', maxWidth: 520 }}>{p.d}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Typography card ─────────────────────────────────────────
function TypeCard() {
  const scale = [
    { token: 'display',  size: 56, weight: 600, sample: 'EUR 110,311', mono: true, tracking: -1.6 },
    { token: 'title-1',  size: 28, weight: 600, sample: 'Transactions', tracking: -0.6 },
    { token: 'title-2',  size: 22, weight: 600, sample: 'May 2026', tracking: -0.4 },
    { token: 'title-3',  size: 17, weight: 600, sample: 'Spending by category', tracking: -0.2 },
    { token: 'body',     size: 15, weight: 500, sample: 'Entertainment', tracking: -0.1 },
    { token: 'body-mono',size: 15, weight: 500, sample: '− EUR 333.00', mono: true, tracking: -0.2 },
    { token: 'caption',  size: 13, weight: 500, sample: 'Friday, May 15', tracking: 0 },
    { token: 'micro',    size: 11, weight: 600, sample: 'SECURITY', uppercase: true, tracking: 0.08 },
  ];
  return (
    <div className="foundation-card" style={{ width: '100%', height: '100%' }}>
      <h2>Typography — Geist / Geist Mono</h2>
      <div style={{ display: 'grid', gap: 18 }}>
        {scale.map(r => (
          <div key={r.token} style={{
            display: 'grid', gridTemplateColumns: '120px 1fr 80px',
            alignItems: 'baseline', gap: 24,
            paddingBottom: 14,
            borderBottom: '1px solid #F0F0F0',
          }}>
            <div className="mm-mono" style={{ fontSize: 11, color: '#A3A3A3', letterSpacing: 0.04 }}>{r.token}</div>
            <div style={{
              fontFamily: r.mono ? "'Geist Mono', monospace" : "'Geist', sans-serif",
              fontSize: r.size,
              fontWeight: r.weight,
              letterSpacing: r.tracking + 'px',
              textTransform: r.uppercase ? 'uppercase' : 'none',
              color: '#0A0A0A',
              lineHeight: 1.15,
            }}>{r.sample}</div>
            <div className="mm-mono" style={{ fontSize: 11, color: '#A3A3A3', textAlign: 'right' }}>{r.size}/{r.weight}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─── Neutrals card ───────────────────────────────────────────
function NeutralsCard() {
  const light = [
    { tok: '--bg',           hex: '#FFFFFF', label: 'Background' },
    { tok: '--surface',      hex: '#FAFAFA', label: 'Surface' },
    { tok: '--surface-2',    hex: '#F4F4F4', label: 'Surface 2' },
    { tok: '--border',       hex: '#ECECEC', label: 'Border' },
    { tok: '--border-strong',hex: '#D4D4D4', label: 'Border strong' },
    { tok: '--text-3',       hex: '#A3A3A3', label: 'Text tertiary' },
    { tok: '--text-2',       hex: '#6B6B6B', label: 'Text secondary' },
    { tok: '--text',         hex: '#0A0A0A', label: 'Text primary' },
  ];
  const dark = [
    { tok: '--bg',           hex: '#0A0A0A', label: 'Background' },
    { tok: '--surface',      hex: '#141414', label: 'Surface' },
    { tok: '--surface-2',    hex: '#1C1C1C', label: 'Surface 2' },
    { tok: '--border',       hex: '#232323', label: 'Border' },
    { tok: '--border-strong',hex: '#353535', label: 'Border strong' },
    { tok: '--text-3',       hex: '#6B6B6B', label: 'Text tertiary' },
    { tok: '--text-2',       hex: '#A3A3A3', label: 'Text secondary' },
    { tok: '--text',         hex: '#FAFAFA', label: 'Text primary' },
  ];
  const col = (label, swatches, dark) => (
    <div style={{
      background: dark ? '#0A0A0A' : '#FFFFFF',
      color: dark ? '#FAFAFA' : '#0A0A0A',
      borderRadius: 12, padding: 24,
      border: '1px solid ' + (dark ? '#232323' : '#ECECEC'),
    }}>
      <div style={{
        fontSize: 11, fontWeight: 600, letterSpacing: 0.08,
        textTransform: 'uppercase', marginBottom: 16,
        color: dark ? '#6B6B6B' : '#A3A3A3',
      }}>{label}</div>
      <div style={{ display: 'grid', gap: 10 }}>
        {swatches.map(s => (
          <div key={s.tok} style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{
              width: 28, height: 28, borderRadius: 6,
              background: s.hex,
              border: '1px solid ' + (dark ? 'rgba(255,255,255,0.06)' : 'rgba(0,0,0,0.06)'),
            }} />
            <div style={{ flex: 1, fontSize: 13, fontWeight: 500 }}>{s.label}</div>
            <div className="mm-mono" style={{ fontSize: 11, opacity: 0.7 }}>{s.hex}</div>
          </div>
        ))}
      </div>
    </div>
  );
  return (
    <div className="foundation-card" style={{ width: '100%', height: '100%' }}>
      <h2>Neutrals — Light & Dark</h2>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
        {col('Light', light, false)}
        {col('Dark', dark, true)}
      </div>
      <div style={{ marginTop: 20, padding: '14px 16px', background: '#F4F4F4', borderRadius: 10, fontSize: 13, color: '#6B6B6B', lineHeight: 1.5 }}>
        The two ramps mirror each other so semantic tokens (--text, --surface) work identically in both modes.
      </div>
    </div>
  );
}

// ─── Category palette card ───────────────────────────────────
function CategoriesCard() {
  const cats = [
    { name: 'Health',        hex: '#C2566B' },
    { name: 'Entertainment', hex: '#8B6FB0' },
    { name: 'Salary',        hex: '#4A8E5C' },
    { name: 'Transport',     hex: '#4F8694' },
    { name: 'Utilities',     hex: '#B89148' },
    { name: 'Groceries',     hex: '#7A9572' },
    { name: 'Eating out',    hex: '#C97A4F' },
    { name: 'Rent',          hex: '#5A7BA8' },
    { name: 'Shopping',      hex: '#B07089' },
    { name: 'Other',         hex: '#8A8A8A' },
  ];
  return (
    <div className="foundation-card" style={{ width: '100%', height: '100%' }}>
      <h2>Category Palette</h2>
      <div style={{ fontSize: 13, color: '#6B6B6B', marginBottom: 24, maxWidth: 520, lineHeight: 1.5 }}>
        Muted, mid-tone hues — readable on both light and dark backgrounds without adjustment. Used as 8px dots in lists and as fill colors in charts.
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 12 }}>
        {cats.map(c => (
          <div key={c.name} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '12px 14px', background: '#FAFAFA',
            border: '1px solid #ECECEC', borderRadius: 10,
          }}>
            <div style={{ width: 8, height: 8, borderRadius: 999, background: c.hex }} />
            <div style={{ flex: 1, fontSize: 14, fontWeight: 500 }}>{c.name}</div>
            <div className="mm-mono" style={{ fontSize: 11, color: '#A3A3A3' }}>{c.hex}</div>
          </div>
        ))}
      </div>
      <div style={{
        marginTop: 20, padding: 20,
        background: '#0A0A0A', color: '#FAFAFA',
        borderRadius: 10,
      }}>
        <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.08, textTransform: 'uppercase', color: '#6B6B6B', marginBottom: 12 }}>On dark</div>
        <div style={{ display: 'flex', gap: 10, flexWrap: 'wrap' }}>
          {cats.map(c => (
            <div key={c.name} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
              <div style={{ width: 8, height: 8, borderRadius: 999, background: c.hex }} />
              <div style={{ fontSize: 12 }}>{c.name}</div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// ─── Spacing, radii, accent ──────────────────────────────────
function SpacingCard() {
  const spaces = [4, 8, 12, 16, 20, 24, 32, 40, 48];
  const radii = [
    { name: 'xs',   v: 6  },
    { name: 'sm',   v: 8  },
    { name: 'md',   v: 12 },
    { name: 'lg',   v: 16 },
    { name: 'xl',   v: 20 },
    { name: 'pill', v: 999 },
  ];
  return (
    <div className="foundation-card" style={{ width: '100%', height: '100%' }}>
      <h2>Spacing, Radius & Accent</h2>

      <div style={{ marginBottom: 28 }}>
        <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>Spacing (4px base)</div>
        <div style={{ display: 'flex', alignItems: 'flex-end', gap: 12 }}>
          {spaces.map(s => (
            <div key={s} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
              <div style={{ width: s, height: s, background: '#0A0A0A', borderRadius: 2 }} />
              <div className="mm-mono" style={{ fontSize: 10, color: '#A3A3A3' }}>{s}</div>
            </div>
          ))}
        </div>
      </div>

      <div style={{ marginBottom: 28 }}>
        <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>Radius</div>
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          {radii.map(r => (
            <div key={r.name} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
              <div style={{
                width: 56, height: 56,
                background: '#FAFAFA', border: '1px solid #ECECEC',
                borderRadius: r.v,
              }} />
              <div className="mm-mono" style={{ fontSize: 11, color: '#6B6B6B' }}>{r.name}</div>
              <div className="mm-mono" style={{ fontSize: 10, color: '#A3A3A3' }}>{r.v === 999 ? '∞' : r.v}</div>
            </div>
          ))}
        </div>
      </div>

      <div>
        <div style={{ fontSize: 13, fontWeight: 600, marginBottom: 12 }}>Accent — Green</div>
        <div style={{ display: 'flex', gap: 12, alignItems: 'center' }}>
          <div style={{ width: 56, height: 56, background: '#16A34A', borderRadius: 12 }} />
          <div style={{ flex: 1 }}>
            <div className="mm-mono" style={{ fontSize: 12, color: '#0A0A0A', marginBottom: 4 }}>#16A34A</div>
            <div style={{ fontSize: 13, color: '#6B6B6B', lineHeight: 1.5 }}>
              Reserved for income amounts and the "save" confirmation. Avoid using on backgrounds, chips, or chrome.
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

Object.assign(window, { PrinciplesCard, TypeCard, NeutralsCard, CategoriesCard, SpacingCard });

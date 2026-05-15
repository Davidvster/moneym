// MoneyM Component Showcase Cards
// Up-to-date showcase reflecting the current design system.
// Each card renders in both light and dark side-by-side.

const {
  MMTheme, Icon, CATS, CatDot, CategoryIcon,
  Button, IconButton, Segmented, Chip, Field, Toggle, Row,
  SectionLabel, TabBar, AppStatusBar, Card, Money, MiniBars,
} = window;

// ─── Layout helpers ──────────────────────────────────────────
function DualPane({ children, padding = 24 }) {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', height: '100%' }}>
      <MMTheme dark={false} style={{ padding, borderRight: '1px solid #ECECEC' }}>
        <div style={{
          fontSize: 10, fontWeight: 600, letterSpacing: 0.1,
          textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 16,
        }}>Light</div>
        {children(false)}
      </MMTheme>
      <MMTheme dark={true} style={{ padding }}>
        <div style={{
          fontSize: 10, fontWeight: 600, letterSpacing: 0.1,
          textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 16,
        }}>Dark</div>
        {children(true)}
      </MMTheme>
    </div>
  );
}

function ComponentLabel({ children, mt = 16 }) {
  return (
    <div style={{
      fontSize: 10, fontWeight: 600, letterSpacing: 0.08,
      textTransform: 'uppercase', color: 'var(--text-3)',
      marginBottom: 10, marginTop: mt,
    }}>{children}</div>
  );
}

// ─── Buttons card (with disabled) ────────────────────────────
function ButtonsCard() {
  const content = (dark) => (
    <div>
      <ComponentLabel mt={0}>Primary actions</ComponentLabel>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <Button variant="primary">Save</Button>
        <Button variant="accent" leading={<Icon name="check" size={16} strokeWidth={2} />}>Add transaction</Button>
        <Button variant="secondary">Cancel</Button>
      </div>

      <ComponentLabel>Tertiary</ComponentLabel>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <Button variant="outline">Outline</Button>
        <Button variant="ghost">Ghost</Button>
        <Button variant="danger" leading={<Icon name="trash" size={16} />}>Delete</Button>
      </div>

      <ComponentLabel>Disabled (every variant uses the same flat token)</ComponentLabel>
      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
        <Button variant="primary" disabled>Save</Button>
        <Button variant="accent"  disabled leading={<Icon name="check" size={16} />}>Add transaction</Button>
        <Button variant="danger"  disabled leading={<Icon name="trash" size={16} />}>Delete</Button>
      </div>

      <ComponentLabel>Sizes</ComponentLabel>
      <div style={{ display: 'flex', gap: 8, alignItems: 'center', flexWrap: 'wrap' }}>
        <Button variant="primary" size="sm">Small · 32</Button>
        <Button variant="primary" size="md">Medium · 44</Button>
        <Button variant="primary" size="lg">Large · 52</Button>
      </div>

      <ComponentLabel>Icon buttons</ComponentLabel>
      <div style={{ display: 'flex', gap: 4, alignItems: 'center' }}>
        <IconButton icon="close" />
        <IconButton icon="chevronLeft" />
        <IconButton icon="chevronRight" />
        <IconButton icon="search" />
        <IconButton icon="calendar" />
        <IconButton icon="check" accent />
        <IconButton icon="trash" danger />
      </div>
    </div>
  );
  return <DualPane>{content}</DualPane>;
}

// ─── Toggles & switches (with disabled) ──────────────────────
function TogglesCard() {
  const content = (dark) => (
    <div>
      <ComponentLabel mt={0}>Toggle</ComponentLabel>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 12 }}>
        <RowMini left={<Toggle on />}        label="On"            sub="Enabled · tap to disable" />
        <RowMini left={<Toggle on={false} />} label="Off"           sub="Enabled · tap to enable" />
        <RowMini left={<Toggle on disabled />}        label="On · disabled"  sub="Cannot be toggled" />
        <RowMini left={<Toggle on={false} disabled />} label="Off · disabled" sub="Cannot be toggled" />
      </div>

      <ComponentLabel>In a settings row</ComponentLabel>
      <Card style={{ overflow: 'hidden' }}>
        <Row>
          <Icon name="lock" size={18} />
          <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Enable PIN lock</div>
          <Toggle on />
        </Row>
        <Row>
          <Icon name="fingerprint" size={18} style={{ opacity: 0.5 }} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 15, fontWeight: 500, color: 'var(--text-2)' }}>Unlock with biometrics</div>
            <div style={{ fontSize: 12, color: 'var(--text-3)' }}>Enable PIN lock first</div>
          </div>
          <Toggle on={false} disabled />
        </Row>
        <Row divider={false}>
          <Icon name="globe" size={18} />
          <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Use device language</div>
          <Toggle on={false} />
        </Row>
      </Card>
    </div>
  );
  return <DualPane>{content}</DualPane>;
}

// Mini helper — single row showing a leading element + label + sub
function RowMini({ left, label, sub }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
      {left}
      <div>
        <div style={{ fontSize: 14, fontWeight: 500 }}>{label}</div>
        {sub && <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 1 }}>{sub}</div>}
      </div>
    </div>
  );
}

// ─── Segmented + Chip card ───────────────────────────────────
function SegmentedChipsCard() {
  function Demo({ dark }) {
    const [seg, setSeg] = React.useState('All');
    const [type, setType] = React.useState('Expense');
    const [pct, setPct] = React.useState('percent');
    const [cat, setCat] = React.useState('Entertainment');
    return (
      <div>
        <ComponentLabel mt={0}>Segmented · pill</ComponentLabel>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 12, alignItems: 'flex-start' }}>
          <Segmented options={['All', 'Expenses', 'Income']} value={seg} onChange={setSeg} />
          <Segmented options={['Expense', 'Income']} value={type} onChange={setType} />
          <Segmented
            options={[{ value: 'percent', label: '%' }, { value: 'amount', label: 'EUR' }]}
            value={pct} onChange={setPct} size="sm"
          />
        </div>

        <ComponentLabel>Filter chips</ComponentLabel>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {['All', 'Eating out', 'Entertainment', 'Groceries', 'Transport'].map(c => (
            <Chip key={c} selected={c === 'Eating out'}>{c}</Chip>
          ))}
        </div>

        <ComponentLabel>Category chips (with icon tile)</ComponentLabel>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {['Eating out', 'Entertainment', 'Groceries', 'Health', 'Rent', 'Salary'].map(c => (
            <Chip
              key={c}
              selected={c === cat}
              leading={<CategoryIcon category={c} size={20} radius={6} variant="tile" />}
              onClick={() => setCat(c)}
            >{c}</Chip>
          ))}
        </div>
      </div>
    );
  }
  return <DualPane>{(dark) => <Demo dark={dark} />}</DualPane>;
}

// ─── Fields card ─────────────────────────────────────────────
function FieldsCard() {
  const content = (dark) => (
    <div>
      <ComponentLabel mt={0}>Text fields</ComponentLabel>
      <div style={{ display: 'grid', gap: 12 }}>
        <Field label="Amount" value="333.00" mono prefix="EUR" />
        <Field label="Date" value="May 15, 2026" />
        <Field label="Note (optional)" placeholder="Add a note…" />
      </div>
    </div>
  );
  return <DualPane>{content}</DualPane>;
}

// ─── CategoryIcon variants ───────────────────────────────────
function CategoryIconsCard() {
  const cats = ['Entertainment', 'Transport', 'Health', 'Utilities', 'Groceries', 'Eating out', 'Salary', 'Shopping'];
  const variants = [
    { v: 'tile', label: 'Tile · default',   sub: 'Filled colored square, white icon' },
    { v: 'soft', label: 'Soft',             sub: 'Tinted bg, colored icon' },
    { v: 'bar',  label: 'Bar',              sub: '4dp vertical accent bar' },
    { v: 'dot',  label: 'Dot',              sub: '8dp colored circle' },
  ];
  const renderVariant = (v) => (
    <div style={{ marginBottom: 14 }}>
      <ComponentLabel mt={0}>{variants.find(x => x.v === v).label}</ComponentLabel>
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12, alignItems: 'center' }}>
        {cats.map(c => (
          <div key={c} title={c}>
            <CategoryIcon category={c} size={36} radius={10} variant={v} />
          </div>
        ))}
      </div>
    </div>
  );
  return (
    <DualPane>{(dark) => (
      <div>
        {variants.map(({ v }) => renderVariant(v))}
      </div>
    )}</DualPane>
  );
}

// ─── List rows card (transaction + settings) ─────────────────
function ListRowsCard() {
  const content = (dark) => (
    <div>
      <ComponentLabel mt={0}>Transaction row · default (icon tile)</ComponentLabel>
      <Card style={{ overflow: 'hidden' }}>
        <Row padding="12px 16px">
          <CategoryIcon category="Entertainment" size={38} radius={11} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 15, fontWeight: 500, letterSpacing: -0.1 }}>Royal Albert Hall</div>
            <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 2 }}>Entertainment</div>
          </div>
          <Money value={333} sign="-" size={15} />
        </Row>
        <Row padding="12px 16px">
          <CategoryIcon category="Salary" size={38} radius={11} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 15, fontWeight: 500, letterSpacing: -0.1 }}>May payroll</div>
            <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 2 }}>Salary</div>
          </div>
          <Money value={3500} sign="+" size={15} weight={600} style={{ color: 'var(--accent)' }} />
        </Row>
        <Row padding="12px 16px" divider={false}>
          <CategoryIcon category="Groceries" size={38} radius={11} />
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 15, fontWeight: 500, letterSpacing: -0.1 }}>Weekly shop · Edeka</div>
            <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 2 }}>Groceries</div>
          </div>
          <Money value={87.40} sign="-" size={15} />
        </Row>
      </Card>

      <ComponentLabel>Settings row variants</ComponentLabel>
      <Card style={{ overflow: 'hidden' }}>
        <Row>
          <Icon name="lock" size={18} />
          <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Enable PIN lock</div>
          <Toggle on />
        </Row>
        <Row>
          <Icon name="globe" size={18} />
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 15, fontWeight: 500 }}>App language</div>
            <div style={{ fontSize: 13, color: 'var(--text-2)' }}>English</div>
          </div>
          <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
        </Row>
        <Row divider={false}>
          <Icon name="sliders" size={18} />
          <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Transaction list</div>
          <Segmented options={['Tile', 'Bar', 'Dot']} value="Tile" size="sm" />
        </Row>
      </Card>
    </div>
  );
  return <DualPane>{content}</DualPane>;
}

// ─── Money display ───────────────────────────────────────────
function MoneyCard() {
  const content = (dark) => (
    <div>
      <ComponentLabel mt={0}>Money — sign + currency + tabular figures</ComponentLabel>
      <div style={{ display: 'grid', gap: 14 }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
          <Money value={3500} sign="+" size={28} weight={600} style={{ color: 'var(--accent)' }} />
          <span style={{ fontSize: 12, color: 'var(--text-3)' }}>income · accent green</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
          <Money value={333} sign="-" size={28} weight={600} />
          <span style={{ fontSize: 12, color: 'var(--text-3)' }}>expense · neutral text</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
          <Money value={895.90} size={28} weight={600} />
          <span style={{ fontSize: 12, color: 'var(--text-3)' }}>total · no sign</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 16 }}>
          <Money value={18.50} sign="-" size={28} weight={600} muted />
          <span style={{ fontSize: 12, color: 'var(--text-3)' }}>muted</span>
        </div>
      </div>

      <ComponentLabel>Column alignment (tabular nums)</ComponentLabel>
      <div style={{ display: 'grid', gap: 4, fontFamily: 'var(--font-mono)' }}>
        {[1.23, 18.50, 333.00, 3500.00, 110311.00].map(v => (
          <div key={v} style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Money value={v} sign={v > 100 ? '+' : '-'} size={14} weight={500}
              style={v > 100 ? { color: 'var(--accent)' } : {}} />
          </div>
        ))}
      </div>
    </div>
  );
  return <DualPane>{content}</DualPane>;
}

// ─── Tab bar + status bar card ───────────────────────────────
function NavCard() {
  return (
    <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', height: '100%' }}>
      <MMTheme dark={false} style={{ borderRight: '1px solid #ECECEC', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '24px 24px 16px' }}>
          <div style={{ fontSize: 10, fontWeight: 600, letterSpacing: 0.1, textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 16 }}>Light</div>
          <AppStatusBar />
          <ComponentLabel>Tab bar — Transactions active</ComponentLabel>
        </div>
        <div style={{ flex: 1 }} />
        <TabBar active="transactions" />
      </MMTheme>
      <MMTheme dark={true} style={{ display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '24px 24px 16px' }}>
          <div style={{ fontSize: 10, fontWeight: 600, letterSpacing: 0.1, textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 16 }}>Dark</div>
          <AppStatusBar />
          <ComponentLabel>Tab bar — Overview active</ComponentLabel>
        </div>
        <div style={{ flex: 1 }} />
        <TabBar active="overview" />
      </MMTheme>
    </div>
  );
}

// ─── Numeric keypad card ─────────────────────────────────────
function KeypadCard() {
  function Pad({ dark }) {
    const keys = ['1','2','3','4','5','6','7','8','9'];
    return (
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 80px)', gap: 14, maxWidth: 280 }}>
        {keys.map(k => (
          <button key={k} style={{
            width: 80, height: 72, fontSize: 28, fontWeight: 400,
            fontFamily: 'var(--font-sans)',
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--r-lg)',
            color: 'var(--text)',
            cursor: 'pointer',
          }}>{k}</button>
        ))}
        <button style={{
          width: 80, height: 72, borderRadius: 'var(--r-lg)',
          background: 'transparent', border: 'none',
          color: 'var(--text)', display: 'flex',
          alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon name="faceId" size={28} strokeWidth={1.4} />
        </button>
        <button style={{
          width: 80, height: 72, fontSize: 28, fontWeight: 400,
          fontFamily: 'var(--font-sans)',
          background: 'var(--surface)',
          border: '1px solid var(--border)',
          borderRadius: 'var(--r-lg)',
          color: 'var(--text)',
          cursor: 'pointer',
        }}>0</button>
        <button style={{
          width: 80, height: 72, borderRadius: 'var(--r-lg)',
          background: 'transparent', border: 'none',
          color: 'var(--text-2)', display: 'flex',
          alignItems: 'center', justifyContent: 'center',
        }}>
          <Icon name="backspace" size={22} strokeWidth={1.5} />
        </button>
      </div>
    );
  }
  return (
    <DualPane>{(dark) => (
      <div>
        <ComponentLabel mt={0}>Numeric keypad — rounded squares · 80×72 dp</ComponentLabel>
        <Pad dark={dark} />
      </div>
    )}</DualPane>
  );
}

// ─── Charts: donut + sparkline + bar ─────────────────────────
function Donut({ size = 140, stroke = 22, data }) {
  const total = data.reduce((s, d) => s + d.value, 0);
  const r = (size - stroke) / 2;
  const c = 2 * Math.PI * r;
  let offset = 0;
  return (
    <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
      <circle cx={size/2} cy={size/2} r={r} fill="none" stroke="var(--divider)" strokeWidth={stroke} />
      {data.map((d, i) => {
        const dash = (d.value / total) * c;
        const el = (
          <circle key={i}
            cx={size/2} cy={size/2} r={r} fill="none"
            stroke={d.color} strokeWidth={stroke}
            strokeDasharray={`${dash} ${c - dash}`}
            strokeDashoffset={-offset}
            transform={`rotate(-90 ${size/2} ${size/2})`}
          />
        );
        offset += dash;
        return el;
      })}
    </svg>
  );
}

function ChartsCard() {
  const donutData = [
    { name: 'Entertainment', value: 38, color: '#8B6FB0' },
    { name: 'Transport',     value: 24, color: '#4F8694' },
    { name: 'Health',        value: 14, color: '#C2566B' },
    { name: 'Utilities',     value: 12, color: '#B89148' },
    { name: 'Groceries',     value: 10, color: '#7A9572' },
    { name: 'Eating out',    value: 2,  color: '#C97A4F' },
  ];
  const months = ['J','F','M','A','M','J','J','A','S','O','N','D'];
  const bars = [42, 65, 38, 55, 343, 0, 0, 0, 0, 0, 0, 0];
  return (
    <DualPane>{(dark) => (
      <div>
        <ComponentLabel mt={0}>Donut</ComponentLabel>
        <div style={{ display: 'flex', gap: 24, alignItems: 'center' }}>
          <Donut data={donutData} size={120} stroke={16} />
          <div style={{ flex: 1, display: 'grid', gap: 6 }}>
            {donutData.map(d => (
              <div key={d.name} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12 }}>
                <div style={{ width: 6, height: 6, borderRadius: 999, background: d.color }} />
                <div style={{ flex: 1 }}>{d.name}</div>
                <div className="mm-mono" style={{ color: 'var(--text-2)', fontSize: 11 }}>{d.value}%</div>
              </div>
            ))}
          </div>
        </div>

        <ComponentLabel>Per-category sparkline (MiniBars)</ComponentLabel>
        <div style={{ display: 'grid', gap: 14 }}>
          {donutData.slice(0, 3).map(d => (
            <div key={d.name}>
              <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 6 }}>
                <span style={{ fontSize: 12, fontWeight: 500 }}>{d.name}</span>
                <span className="mm-mono" style={{ fontSize: 11, color: 'var(--text-3)' }}>12 months</span>
              </div>
              <MiniBars
                data={[18, 32, 12, 40, 60, 24, 30, 14, 22, 38, 50, 28]}
                color={d.color}
                height={24}
                highlight={4}
              />
            </div>
          ))}
        </div>
      </div>
    )}</DualPane>
  );
}

Object.assign(window, {
  ButtonsCard, TogglesCard, SegmentedChipsCard, FieldsCard,
  CategoryIconsCard, ListRowsCard, MoneyCard,
  NavCard, KeypadCard, ChartsCard, Donut,
});

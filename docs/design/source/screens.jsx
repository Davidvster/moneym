// MoneyM Screens — Full app screens, light + dark, platform-agnostic.

const {
  MMTheme, Icon, CATS, CatDot, CategoryIcon,
  Button, IconButton, Segmented, Chip, Field, Toggle, Row,
  SectionLabel, TabBar, AppStatusBar, Card, Money, MiniBars,
  Donut,
} = window;

// Render a single transaction row honoring display preferences
function TxRow({ tx, prefs = {}, divider = true }) {
  const {
    style = 'icon-tile',   // 'icon-tile' | 'soft-icon' | 'bar' | 'dot' | 'none'
    showCategoryName = true,
    showNote = true,
    density = 'comfortable',
  } = prefs;
  const padding = density === 'compact' ? '10px 20px' : '14px 20px';
  const hasNote = !!tx.note;
  // Primary text = note (if present); secondary = category.
  // If no note, category becomes primary.
  const primary = hasNote && showNote ? tx.note : tx.cat;
  const showSecondary = hasNote && showNote && showCategoryName;

  let leading;
  if (style === 'icon-tile') leading = <CategoryIcon category={tx.cat} size={38} radius={11} variant="tile" />;
  else if (style === 'soft-icon') leading = <CategoryIcon category={tx.cat} size={38} radius={11} variant="soft" />;
  else if (style === 'bar')  leading = <CategoryIcon category={tx.cat} size={38} variant="bar" />;
  else if (style === 'dot')  leading = <CatDot category={tx.cat} size={10} />;
  else leading = null;

  return (
    <Row padding={padding} divider={divider}>
      {leading}
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 15, fontWeight: 500, letterSpacing: -0.1, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          {primary}
        </div>
        {showSecondary && (
          <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 2 }}>{tx.cat}</div>
        )}
      </div>
      <Money
        value={tx.amount}
        sign={tx.amount > 0 ? '+' : '-'}
        size={15}
        weight={tx.amount > 0 ? 600 : 500}
        style={tx.amount > 0 ? { color: 'var(--accent)' } : {}}
      />
    </Row>
  );
}

// ─── Screen shell ────────────────────────────────────────────
function Screen({ dark = false, children, time = '9:41', label, scroll = false }) {
  return (
    <MMTheme dark={dark} style={{ display: 'flex', flexDirection: 'column' }}>
      <AppStatusBar dark={dark} time={time} />
      <div style={{
        flex: 1, display: 'flex', flexDirection: 'column',
        overflow: scroll ? 'auto' : 'visible',
        minHeight: 0,
      }}>
        {children}
      </div>
      {label && (
        <div style={{ display: 'flex', justifyContent: 'center', padding: '8px 0' }}>
          <div style={{ width: 134, height: 5, borderRadius: 999, background: dark ? '#fff' : '#0A0A0A', opacity: dark ? 0.6 : 0.85 }} />
        </div>
      )}
    </MMTheme>
  );
}

// ─── 01 — PIN screen ─────────────────────────────────────────
function PinScreen({ dark = false }) {
  const filled = 2;
  const keys = ['1','2','3','4','5','6','7','8','9'];
  return (
    <Screen dark={dark} label>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', padding: '0 32px' }}>
        {/* App lockup */}
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, marginBottom: 48 }}>
          <div style={{
            width: 56, height: 56, borderRadius: 16,
            background: 'var(--text)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            marginBottom: 12,
          }}>
            <span style={{
              fontFamily: 'var(--font-sans)', fontWeight: 700,
              fontSize: 28, color: 'var(--bg)', letterSpacing: -1,
            }}>M</span>
          </div>
          <div style={{ fontSize: 22, fontWeight: 600, letterSpacing: -0.4 }}>MoneyM</div>
          <div style={{ fontSize: 14, color: 'var(--text-2)' }}>Enter your PIN</div>
        </div>

        {/* Dots */}
        <div style={{ display: 'flex', gap: 18, marginBottom: 48 }}>
          {[0,1,2,3].map(i => (
            <div key={i} style={{
              width: 12, height: 12, borderRadius: 999,
              background: i < filled ? 'var(--text)' : 'transparent',
              border: `1.5px solid ${i < filled ? 'var(--text)' : 'var(--border-strong)'}`,
            }} />
          ))}
        </div>

        {/* Keypad — rounded squares to match the keypad component */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 80px)', gap: 14, marginBottom: 24 }}>
          {keys.map(k => (
            <button key={k} style={{
              width: 80, height: 72, borderRadius: 'var(--r-lg)',
              background: 'var(--surface)',
              border: '1px solid var(--border)',
              color: 'var(--text)',
              fontSize: 28, fontWeight: 400,
              fontFamily: 'var(--font-sans)',
              cursor: 'pointer',
            }}>{k}</button>
          ))}
          <button style={{
            width: 80, height: 72, borderRadius: 'var(--r-lg)', border: 'none',
            background: 'transparent', color: 'var(--text)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            cursor: 'pointer',
          }}>
            <Icon name="faceId" size={28} strokeWidth={1.4} />
          </button>
          <button style={{
            width: 80, height: 72, borderRadius: 'var(--r-lg)',
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            color: 'var(--text)',
            fontSize: 28, fontWeight: 400,
            fontFamily: 'var(--font-sans)',
            cursor: 'pointer',
          }}>0</button>
          <button style={{
            width: 80, height: 72, borderRadius: 'var(--r-lg)', border: 'none',
            background: 'transparent', color: 'var(--text-2)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            cursor: 'pointer',
          }}>
            <Icon name="backspace" size={22} strokeWidth={1.5} />
          </button>
        </div>
      </div>
    </Screen>
  );
}

// ─── 02 — Transactions ───────────────────────────────────────
function TransactionsScreen({ dark = false, prefs }) {
  const tx = [
    { cat: 'Health',        note: 'Pharmacy refill',       amount: -123.00 },
    { cat: 'Entertainment', note: 'Royal Albert Hall',     amount: -333.00 },
    { cat: 'Salary',        note: 'May payroll',           amount: +3500.00 },
    { cat: 'Transport',     note: 'Monthly metro pass',    amount: -213.00 },
    { cat: 'Utilities',     note: 'Electricity bill',      amount: -111.00 },
    { cat: 'Groceries',     note: 'Weekly shop · Edeka',   amount: -87.40 },
    { cat: 'Entertainment', note: 'Spotify Family',        amount: -9.99 },
    { cat: 'Eating out',    note: 'Lunch with Sara',       amount: -18.50 },
  ];
  const [seg, setSeg] = React.useState('All');

  return (
    <Screen dark={dark} label scroll={false}>
      {/* Header */}
      <div style={{ padding: '4px 16px 12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <h1 style={{ margin: 0, fontSize: 28, fontWeight: 600, letterSpacing: -0.6 }}>Transactions</h1>
          <IconButton icon="search" />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 12, marginBottom: 16 }}>
          <IconButton icon="chevronLeft" size={32} />
          <div style={{ fontSize: 15, fontWeight: 500, minWidth: 96, textAlign: 'center' }}>May 2026</div>
          <IconButton icon="chevronRight" size={32} />
          <div style={{ flex: 1 }} />
          <div style={{ textAlign: 'right' }}>
            <div style={{ fontSize: 11, color: 'var(--text-3)', letterSpacing: 0.04, textTransform: 'uppercase', fontWeight: 600 }}>Net</div>
            <Money value={3287} sign="+" size={17} weight={600} style={{ color: 'var(--accent)' }} />
          </div>
        </div>
        <Segmented options={['All', 'Expenses', 'Income']} value={seg} onChange={setSeg} />
      </div>

      {/* List */}
      <div style={{ flex: 1, overflow: 'visible' }}>
        <div style={{ padding: '12px 20px 6px', fontSize: 11, color: 'var(--text-3)', fontWeight: 600, letterSpacing: 0.08, textTransform: 'uppercase' }}>
          Friday, May 15
        </div>
        {tx.slice(0, 4).map((t, i) => (
          <TxRow key={i} tx={t} prefs={prefs} />
        ))}

        <div style={{ padding: '12px 20px 6px', fontSize: 11, color: 'var(--text-3)', fontWeight: 600, letterSpacing: 0.08, textTransform: 'uppercase' }}>
          Thursday, May 14
        </div>
        {tx.slice(4).map((t, i) => (
          <TxRow key={i} tx={t} prefs={prefs} />
        ))}
      </div>

      {/* "+ New" pinned at bottom — platform-neutral, not a Material FAB */}
      <div style={{
        padding: '12px 16px 16px',
        borderTop: '1px solid var(--divider)',
        background: 'var(--bg)',
      }}>
        <Button variant="primary" size="lg" fullWidth leading={<Icon name="plus" size={18} />}>
          New transaction
        </Button>
      </div>

      <TabBar active="transactions" />
    </Screen>
  );
}

// ─── 03 — Add Transaction ────────────────────────────────────
function AddTxScreen({ dark = false, editing = false }) {
  const [type, setType] = React.useState('Expense');
  const [cat, setCat] = React.useState(editing ? 'Entertainment' : null);
  const cats = ['Eating out','Entertainment','Groceries','Health','Other (expense)','Other (income)','Rent','Salary','Shopping','Transport','Utilities'];

  return (
    <Screen dark={dark} label>
      {/* Modal header */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 4,
        padding: '4px 12px 12px',
      }}>
        <IconButton icon="close" />
        <div style={{ flex: 1, fontSize: 17, fontWeight: 600, letterSpacing: -0.2, textAlign: 'center' }}>
          {editing ? 'Edit Transaction' : 'New Transaction'}
        </div>
        {editing ? <IconButton icon="trash" danger /> : <div style={{ width: 40 }} />}
      </div>

      <div style={{ flex: 1, overflow: 'visible', padding: '8px 20px 100px' }}>
        {/* Expense / Income */}
        <Segmented options={['Expense', 'Income']} value={type} onChange={setType} style={{ marginBottom: 24 }} />

        {/* Big amount display */}
        <div style={{ textAlign: 'center', padding: '12px 0 24px' }}>
          <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.08, textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 8 }}>Amount</div>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'center', gap: 10 }}>
            <span style={{ fontSize: 15, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }}>EUR</span>
            <span className="mm-mono" style={{
              fontSize: 52, fontWeight: 600, letterSpacing: -2,
              color: editing ? 'var(--text)' : 'var(--text-3)',
            }}>{editing ? '333.00' : '0.00'}</span>
          </div>
        </div>

        {/* Fields */}
        <div style={{ display: 'grid', gap: 12, marginBottom: 24 }}>
          <Field label="Date" value="May 15, 2026" />
          <Field label="Note (optional)" value={editing ? 'Concert at the Royal' : ''} placeholder="Add a note..." />
        </div>

        {/* Category */}
        <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.08, textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 12 }}>Category</div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 8 }}>
          {cats.map(c => (
            <Chip
              key={c}
              selected={c === cat}
              leading={<CategoryIcon category={c} size={20} radius={6} variant="tile" />}
              onClick={() => setCat(c)}
            >{c}</Chip>
          ))}
        </div>
      </div>

      {/* Prominent save bar */}
      <div style={{
        padding: '12px 16px 16px',
        borderTop: '1px solid var(--divider)',
        background: 'var(--bg)',
      }}>
        <Button
          variant="accent"
          size="lg"
          fullWidth
          leading={<Icon name="check" size={18} strokeWidth={2} />}
        >
          {editing ? 'Save changes' : 'Add transaction'}
        </Button>
      </div>
    </Screen>
  );
}

// ─── 05 — Overview (Month) ───────────────────────────────────
function OverviewMonthScreen({ dark = false }) {
  // Per-category data — daily series for May (31 days). Day 15 = today.
  const monthCats = [
    { name: 'Entertainment', cat: 'Entertainment', color: '#8B6FB0', amount: 343.00, value: 38, txCount: 2,
      series: [0,0,0,0,9.99,0,0,0,0,0,0,0,0,0,333,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] },
    { name: 'Transport',     cat: 'Transport',     color: '#4F8694', amount: 213.00, value: 24, txCount: 1,
      series: [0,0,0,0,213,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] },
    { name: 'Health',        cat: 'Health',        color: '#C2566B', amount: 123.00, value: 14, txCount: 1,
      series: [0,0,0,0,0,0,0,0,0,0,0,0,0,0,123,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] },
    { name: 'Utilities',     cat: 'Utilities',     color: '#B89148', amount: 111.00, value: 12, txCount: 1,
      series: [0,0,0,0,0,0,0,0,0,111,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] },
    { name: 'Groceries',     cat: 'Groceries',     color: '#7A9572', amount: 87.40,  value: 10, txCount: 4,
      series: [0,0,22.50,0,0,0,0,0,0,18.40,0,0,0,0,0,0,21.20,0,0,0,0,0,0,25.30,0,0,0,0,0,0,0] },
    { name: 'Eating out',    cat: 'Eating out',    color: '#C97A4F', amount: 18.50,  value: 2,  txCount: 1,
      series: [0,0,0,0,0,0,0,0,0,0,0,0,0,18.50,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0] },
  ];
  const total = monthCats.reduce((s, c) => s + c.amount, 0);

  // Cumulative chart — sum across all categories per day
  const daily = monthCats.reduce((acc, c) => acc.map((v, i) => v + c.series[i]), Array(31).fill(0));
  let a = 0; const cum = daily.map(v => { a += v; return a; });
  const todayIdx = 14;
  const maxCum = Math.max(...cum.slice(0, todayIdx + 1));

  return (
    <Screen dark={dark} label>
      {/* Header */}
      <div style={{ padding: '4px 16px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <h1 style={{ margin: 0, fontSize: 28, fontWeight: 600, letterSpacing: -0.6 }}>Overview</h1>
          <Segmented options={['Month', 'Year']} value="Month" />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 14 }}>
          <IconButton icon="chevronLeft" size={32} />
          <div style={{ fontSize: 15, fontWeight: 500, minWidth: 96, textAlign: 'center' }}>May 2026</div>
          <IconButton icon="chevronRight" size={32} />
        </div>
      </div>

      <div style={{ flex: 1, padding: '0 16px 24px' }}>
        {/* Summary */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 16 }}>
          <Card padded style={{ padding: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
              <Icon name="arrowDown" size={12} stroke="var(--accent)" strokeWidth={2} />
              <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.06, textTransform: 'uppercase', color: 'var(--text-3)' }}>Income</div>
            </div>
            <Money value={3500} size={20} weight={600} style={{ color: 'var(--accent)' }} />
          </Card>
          <Card padded style={{ padding: 16 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
              <Icon name="arrowUp" size={12} strokeWidth={2} />
              <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.06, textTransform: 'uppercase', color: 'var(--text-3)' }}>Expenses</div>
            </div>
            <Money value={total} size={20} weight={600} />
          </Card>
        </div>

        {/* Spending by category */}
        <SpendingByCategoryCard categories={monthCats} total={total} />

        {/* Cumulative chart */}
        <Card padded style={{ padding: 20, marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 4 }}>
            <div style={{ fontSize: 15, fontWeight: 600, letterSpacing: -0.2 }}>Cumulative spend</div>
            <div className="mm-mono" style={{ fontSize: 11, color: 'var(--text-3)' }}>EUR</div>
          </div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8, marginBottom: 16 }}>
            <Money value={cum[todayIdx]} size={22} weight={600} />
            <span style={{ fontSize: 12, color: 'var(--text-2)' }}>through day {todayIdx + 1}</span>
          </div>
          <CumulativeChart cum={cum} today={todayIdx} max={maxCum} height={120} />
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 8, fontSize: 10, color: 'var(--text-3)' }} className="mm-mono">
            <span>1</span><span>8</span><span>15</span><span>22</span><span>31</span>
          </div>
        </Card>

        {/* Per-category daily trends */}
        <CategoryTrendsCard
          categories={monthCats}
          mode="monthly"
          highlight={todayIdx}
          xLabels={['1','8','15','22','31']}
        />
      </div>

      <TabBar active="overview" />
    </Screen>
  );
}

// ─── 06 — Overview (Year) ────────────────────────────────────
function OverviewYearScreen({ dark = false }) {
  const months = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec'];
  // Per-category monthly series (12 values)
  const yearCats = [
    { name: 'Entertainment', cat: 'Entertainment', color: '#8B6FB0', amount: 604, value: 32, txCount: 11,
      series: [42, 65, 38, 55, 343, 0, 0, 0, 0, 0, 0, 0] },
    { name: 'Transport',     cat: 'Transport',     color: '#4F8694', amount: 1065, value: 25, txCount: 5,
      series: [213, 213, 213, 213, 213, 0, 0, 0, 0, 0, 0, 0] },
    { name: 'Utilities',     cat: 'Utilities',     color: '#B89148', amount: 549, value: 13, txCount: 5,
      series: [108, 112, 109, 109, 111, 0, 0, 0, 0, 0, 0, 0] },
    { name: 'Groceries',     cat: 'Groceries',     color: '#7A9572', amount: 392, value: 9, txCount: 18,
      series: [78, 82, 65, 79, 88, 0, 0, 0, 0, 0, 0, 0] },
    { name: 'Health',        cat: 'Health',        color: '#C2566B', amount: 253, value: 6, txCount: 4,
      series: [80, 0, 50, 0, 123, 0, 0, 0, 0, 0, 0, 0] },
    { name: 'Eating out',    cat: 'Eating out',    color: '#C97A4F', amount: 188, value: 4, txCount: 9,
      series: [42, 28, 35, 64, 19, 0, 0, 0, 0, 0, 0, 0] },
  ];
  const total = yearCats.reduce((s, c) => s + c.amount, 0);
  const monthlyTotals = months.map((_, i) => yearCats.reduce((s, c) => s + c.series[i], 0));
  const max = Math.max(...monthlyTotals, 1);

  return (
    <Screen dark={dark} label>
      <div style={{ padding: '4px 16px 16px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <h1 style={{ margin: 0, fontSize: 28, fontWeight: 600, letterSpacing: -0.6 }}>Overview</h1>
          <Segmented options={['Month', 'Year']} value="Year" />
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 4, marginTop: 14 }}>
          <IconButton icon="chevronLeft" size={32} />
          <div style={{ fontSize: 15, fontWeight: 500, minWidth: 96, textAlign: 'center' }}>2026</div>
          <IconButton icon="chevronRight" size={32} />
        </div>
      </div>

      <div style={{ flex: 1, padding: '0 16px 24px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, marginBottom: 16 }}>
          <Card padded style={{ padding: 16 }}>
            <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.06, textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 6 }}>Income</div>
            <Money value={17500} size={20} weight={600} style={{ color: 'var(--accent)' }} />
          </Card>
          <Card padded style={{ padding: 16 }}>
            <div style={{ fontSize: 11, fontWeight: 600, letterSpacing: 0.06, textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 6 }}>Expenses</div>
            <Money value={total} size={20} weight={600} />
          </Card>
        </div>

        {/* Spending by category */}
        <SpendingByCategoryCard categories={yearCats} total={total} />

        {/* Total monthly bars */}
        <Card padded style={{ padding: 20, marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
            <div style={{ fontSize: 15, fontWeight: 600, letterSpacing: -0.2 }}>Monthly spending</div>
            <div className="mm-mono" style={{ fontSize: 11, color: 'var(--text-3)' }}>EUR</div>
          </div>
          <div style={{ display: 'flex', alignItems: 'flex-end', gap: 6, height: 140 }}>
            {monthlyTotals.map((v, i) => (
              <div key={i} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 8 }}>
                <div style={{ flex: 1, display: 'flex', alignItems: 'flex-end', width: '100%', justifyContent: 'center' }}>
                  <div style={{
                    width: '70%',
                    height: v ? Math.max(2, (v / max) * 100) : 2,
                    background: i === 4 ? 'var(--text)' : 'var(--border-strong)',
                    borderRadius: 2,
                    opacity: v ? 1 : 0.4,
                  }} />
                </div>
                <div style={{
                  fontSize: 10,
                  color: i === 4 ? 'var(--text)' : 'var(--text-3)',
                  fontWeight: i === 4 ? 600 : 500,
                  fontFamily: 'var(--font-mono)',
                }}>{months[i]}</div>
              </div>
            ))}
          </div>
        </Card>

        {/* Per-category monthly trends */}
        <CategoryTrendsCard
          categories={yearCats}
          mode="yearly"
          highlight={4}
          xLabels={['Jan','Apr','Jul','Oct','Dec']}
        />
      </div>

      <TabBar active="overview" />
    </Screen>
  );
}

// ─── 07 — Settings ───────────────────────────────────────────
function SettingsScreen({ dark = false }) {
  return (
    <Screen dark={dark} label>
      <div style={{ padding: '4px 20px 16px' }}>
        <h1 style={{ margin: 0, fontSize: 28, fontWeight: 600, letterSpacing: -0.6 }}>Settings</h1>
      </div>

      <div style={{ flex: 1, overflow: 'visible', padding: '0 16px 20px' }}>
        <SectionLabel style={{ padding: '8px 4px 8px' }}>Appearance</SectionLabel>
        <Card style={{ overflow: 'hidden', marginBottom: 8 }}>
          <Row padding="14px 16px">
            <Icon name={dark ? 'moon' : 'sun'} size={18} />
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Theme</div>
            <Segmented options={['Light','Dark','Auto']} value={dark ? 'Dark' : 'Light'} size="sm" />
          </Row>
          <Row padding="14px 16px" divider={false}>
            <Icon name="sliders" size={18} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 500 }}>Transaction list</div>
              <div style={{ fontSize: 13, color: 'var(--text-2)' }}>Icon tile · with note</div>
            </div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
        </Card>

        <SectionLabel style={{ padding: '16px 4px 8px' }}>Security</SectionLabel>
        <Card style={{ overflow: 'hidden', marginBottom: 8 }}>
          <Row padding="14px 16px">
            <Icon name="lock" size={18} />
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Enable PIN lock</div>
            <Toggle on />
          </Row>
          <Row padding="14px 16px">
            <Icon name="fingerprint" size={18} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 500 }}>Unlock with biometrics</div>
              <div style={{ fontSize: 13, color: 'var(--text-2)' }}>Face ID / Fingerprint</div>
            </div>
            <Toggle on />
          </Row>
          <Row padding="14px 16px">
            <Icon name="lock" size={18} style={{ opacity: 0 }} />
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Change PIN</div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
          <Row padding="14px 16px" divider={false}>
            <Icon name="lock" size={18} style={{ opacity: 0 }} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 500 }}>Lock after</div>
              <div style={{ fontSize: 13, color: 'var(--text-2)' }}>Always</div>
            </div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
        </Card>

        <SectionLabel style={{ padding: '16px 4px 8px' }}>Preferences</SectionLabel>
        <Card style={{ overflow: 'hidden', marginBottom: 8 }}>
          <Row padding="14px 16px">
            <Icon name="info" size={18} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 500 }}>Default currency</div>
              <div style={{ fontSize: 13, color: 'var(--text-2)' }}>EUR — Euro</div>
            </div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
          <Row padding="14px 16px">
            <Icon name="globe" size={18} />
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 15, fontWeight: 500 }}>Language</div>
              <div style={{ fontSize: 13, color: 'var(--text-2)' }}>English</div>
            </div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
          <Row padding="14px 16px" divider={false}>
            <Icon name="list" size={18} />
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Manage categories</div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
        </Card>

        <SectionLabel style={{ padding: '16px 4px 8px' }}>Data</SectionLabel>
        <Card style={{ overflow: 'hidden' }}>
          <Row padding="14px 16px">
            <Icon name="download" size={18} />
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Export as JSON</div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
          <Row padding="14px 16px">
            <Icon name="download" size={18} />
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Export as CSV</div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
          <Row padding="14px 16px" divider={false}>
            <Icon name="folder" size={18} />
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Import data</div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </Row>
        </Card>

        <div style={{
          textAlign: 'center',
          fontSize: 11, color: 'var(--text-3)',
          padding: '24px 0', fontFamily: 'var(--font-mono)',
        }}>
          MoneyM v2.0 · build 2026.05.15
        </div>
      </div>

      <TabBar active="settings" />
    </Screen>
  );
}

// ─── Spending-by-category card with %/EUR toggle ─────────────
function SpendingByCategoryCard({ categories, total }) {
  const [mode, setMode] = React.useState('percent'); // 'percent' | 'amount'
  return (
    <Card padded style={{ padding: 20, marginBottom: 16 }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 16 }}>
        <div style={{ fontSize: 15, fontWeight: 600, letterSpacing: -0.2 }}>Spending by category</div>
        <Segmented
          options={[{ value: 'percent', label: '%' }, { value: 'amount', label: 'EUR' }]}
          value={mode}
          onChange={setMode}
          size="sm"
        />
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 20 }}>
        <Donut data={categories} size={130} stroke={18} />
        <div style={{ flex: 1, minWidth: 0 }}>
          {/* Total at top of list */}
          <div style={{
            display: 'flex', alignItems: 'center',
            paddingBottom: 8, marginBottom: 6,
            borderBottom: '1px solid var(--divider)',
          }}>
            <div style={{ flex: 1, fontSize: 11, fontWeight: 600, letterSpacing: 0.08, textTransform: 'uppercase', color: 'var(--text-3)' }}>Total</div>
            {mode === 'percent'
              ? <span className="mm-mono" style={{ fontSize: 13, fontWeight: 600 }}>100%</span>
              : <Money value={total} size={13} weight={600} />}
          </div>
          {/* Per-category */}
          <div style={{ display: 'grid', gap: 7 }}>
            {categories.map(c => (
              <div key={c.name} style={{ display: 'flex', alignItems: 'center', gap: 8, fontSize: 12 }}>
                <div style={{ width: 6, height: 6, borderRadius: 999, background: c.color, flexShrink: 0 }} />
                <div style={{ flex: 1, minWidth: 0, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.name}</div>
                <span className="mm-mono" style={{ color: 'var(--text-2)', fontSize: 11 }}>
                  {mode === 'percent'
                    ? `${c.value}%`
                    : c.amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                </span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </Card>
  );
}

// ─── Per-category trends list ────────────────────────────────
function CategoryTrendsCard({ categories, mode = 'monthly', highlight = -1, xLabels }) {
  return (
    <Card style={{ overflow: 'hidden', marginBottom: 16 }}>
      <div style={{
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '16px 20px 12px',
        borderBottom: '1px solid var(--divider)',
      }}>
        <div style={{ fontSize: 15, fontWeight: 600, letterSpacing: -0.2 }}>
          {mode === 'monthly' ? 'Daily trend by category' : 'Monthly trend by category'}
        </div>
      </div>
      {categories.map((c, i) => (
        <div key={c.name} style={{
          padding: '14px 20px',
          borderBottom: i < categories.length - 1 ? '1px solid var(--divider)' : 'none',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
            <CategoryIcon category={c.name} size={32} radius={9} />
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 500 }}>{c.name}</div>
              <div style={{ fontSize: 11, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }}>
                {c.txCount} transaction{c.txCount === 1 ? '' : 's'}
              </div>
            </div>
            <Money value={c.amount} size={14} weight={600} />
          </div>
          <MiniBars data={c.series} color={c.color} height={26} highlight={highlight} />
          {xLabels && (
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4, fontSize: 9, color: 'var(--text-3)', fontFamily: 'var(--font-mono)' }}>
              {xLabels.map(l => <span key={l}>{l}</span>)}
            </div>
          )}
        </div>
      ))}
    </Card>
  );
}
function CumulativeChart({ cum, today, max, height = 120 }) {
  const n = cum.length;
  const w = 320;
  const stepX = w / (n - 1);
  // Build path up through `today`; rest dashed projection
  const points = cum.map((v, i) => ({
    x: i * stepX,
    y: height - (v / (max || 1)) * height,
  }));
  const actualPts = points.slice(0, today + 1);
  const pathD = 'M ' + actualPts.map(p => `${p.x.toFixed(1)} ${p.y.toFixed(1)}`).join(' L ');
  const areaD = pathD + ` L ${actualPts[actualPts.length-1].x.toFixed(1)} ${height} L 0 ${height} Z`;
  const todayPt = points[today];

  return (
    <svg width="100%" height={height} viewBox={`0 0 ${w} ${height}`} preserveAspectRatio="none" style={{ overflow: 'visible' }}>
      {/* gridlines */}
      {[0.25, 0.5, 0.75].map(p => (
        <line key={p} x1="0" x2={w} y1={height * p} y2={height * p}
          stroke="var(--divider)" strokeWidth="1" strokeDasharray="2 4" />
      ))}
      {/* area fill */}
      <path d={areaD} fill="var(--text)" fillOpacity="0.06" />
      {/* line */}
      <path d={pathD} fill="none" stroke="var(--text)" strokeWidth="1.5"
        strokeLinejoin="round" strokeLinecap="round" vectorEffect="non-scaling-stroke" />
      {/* today marker */}
      <line x1={todayPt.x} x2={todayPt.x} y1={0} y2={height}
        stroke="var(--text-3)" strokeWidth="1" strokeDasharray="2 3" vectorEffect="non-scaling-stroke" />
      <circle cx={todayPt.x} cy={todayPt.y} r="4" fill="var(--bg)" stroke="var(--text)" strokeWidth="1.5" vectorEffect="non-scaling-stroke" />
    </svg>
  );
}

// ─── Generic screen header (back + title) ────────────────────
function ScreenHeader({ title, onBack = () => {}, trailing }) {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 4,
      padding: '4px 12px 12px',
      borderBottom: '1px solid var(--divider)',
    }}>
      <IconButton icon="chevronLeft" onClick={onBack} />
      <div style={{ flex: 1, fontSize: 17, fontWeight: 600, letterSpacing: -0.2 }}>{title}</div>
      {trailing}
    </div>
  );
}

// ─── 09 — Currency picker ────────────────────────────────────
function CurrencyPickerScreen({ dark = false }) {
  const currencies = [
    { code: 'EUR', name: 'Euro',                  sym: '€', region: 'Eurozone' },
    { code: 'USD', name: 'US Dollar',             sym: '$', region: 'United States' },
    { code: 'GBP', name: 'British Pound',         sym: '£', region: 'United Kingdom' },
    { code: 'CHF', name: 'Swiss Franc',           sym: 'Fr', region: 'Switzerland' },
    { code: 'JPY', name: 'Japanese Yen',          sym: '¥', region: 'Japan' },
    { code: 'CNY', name: 'Chinese Yuan',          sym: '¥', region: 'China' },
    { code: 'CAD', name: 'Canadian Dollar',       sym: '$', region: 'Canada' },
    { code: 'AUD', name: 'Australian Dollar',     sym: '$', region: 'Australia' },
    { code: 'SEK', name: 'Swedish Krona',         sym: 'kr', region: 'Sweden' },
    { code: 'NOK', name: 'Norwegian Krone',       sym: 'kr', region: 'Norway' },
    { code: 'PLN', name: 'Polish Złoty',          sym: 'zł', region: 'Poland' },
    { code: 'CZK', name: 'Czech Koruna',          sym: 'Kč', region: 'Czechia' },
    { code: 'INR', name: 'Indian Rupee',          sym: '₹', region: 'India' },
    { code: 'BRL', name: 'Brazilian Real',        sym: 'R$', region: 'Brazil' },
    { code: 'MXN', name: 'Mexican Peso',          sym: '$', region: 'Mexico' },
  ];
  const popular = ['EUR', 'USD', 'GBP', 'CHF'];
  const selected = 'EUR';

  const Row = ({ c, isSel }) => (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 14,
      padding: '14px 20px', minHeight: 60,
      borderBottom: '1px solid var(--divider)',
    }}>
      <div style={{
        width: 36, height: 36, borderRadius: 'var(--r-sm)',
        background: 'var(--surface-2)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 15, fontWeight: 500,
        fontFamily: 'var(--font-mono)',
      }}>{c.sym}</div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
          <span style={{ fontSize: 15, fontWeight: 500 }}>{c.code}</span>
          <span style={{ fontSize: 13, color: 'var(--text-2)' }}>{c.name}</span>
        </div>
        <div style={{ fontSize: 12, color: 'var(--text-3)', marginTop: 2 }}>{c.region}</div>
      </div>
      {isSel && <Icon name="check" size={18} stroke="var(--accent)" strokeWidth={2.2} />}
    </div>
  );

  return (
    <Screen dark={dark} label>
      <ScreenHeader title="Currency" />
      <div style={{ padding: '14px 16px 8px' }}>
        <Field placeholder="Search currency" prefix={<Icon name="search" size={16} stroke="var(--text-3)" />} />
      </div>

      <div style={{ flex: 1, overflow: 'visible' }}>
        <SectionLabel>Popular</SectionLabel>
        {currencies.filter(c => popular.includes(c.code)).map(c => <Row key={c.code} c={c} isSel={c.code === selected} />)}
        <SectionLabel>All currencies</SectionLabel>
        {currencies.filter(c => !popular.includes(c.code)).map(c => <Row key={c.code} c={c} isSel={c.code === selected} />)}
      </div>
    </Screen>
  );
}

// ─── 10 — Language picker ────────────────────────────────────
function LanguagePickerScreen({ dark = false }) {
  const languages = [
    { code: 'en', name: 'English',    native: 'English' },
    { code: 'de', name: 'German',     native: 'Deutsch' },
    { code: 'fr', name: 'French',     native: 'Français' },
    { code: 'es', name: 'Spanish',    native: 'Español' },
    { code: 'it', name: 'Italian',    native: 'Italiano' },
    { code: 'pt', name: 'Portuguese', native: 'Português' },
    { code: 'nl', name: 'Dutch',      native: 'Nederlands' },
    { code: 'pl', name: 'Polish',     native: 'Polski' },
    { code: 'cs', name: 'Czech',      native: 'Čeština' },
    { code: 'sv', name: 'Swedish',    native: 'Svenska' },
    { code: 'no', name: 'Norwegian',  native: 'Norsk' },
    { code: 'da', name: 'Danish',     native: 'Dansk' },
    { code: 'fi', name: 'Finnish',    native: 'Suomi' },
    { code: 'ja', name: 'Japanese',   native: '日本語' },
    { code: 'zh', name: 'Chinese',    native: '中文' },
    { code: 'ko', name: 'Korean',     native: '한국어' },
    { code: 'ar', name: 'Arabic',     native: 'العربية' },
  ];
  const selected = 'en';

  return (
    <Screen dark={dark} label>
      <ScreenHeader title="Language" />

      <div style={{ flex: 1, overflow: 'visible' }}>
        <div style={{
          padding: '20px 20px 16px',
          fontSize: 13, color: 'var(--text-2)', lineHeight: 1.5,
        }}>
          Choose a display language. Categories, currency formatting and dates follow this choice.
        </div>

        <div style={{ padding: '0 16px 8px' }}>
          <Row padding="14px 16px" style={{
            background: 'var(--surface)',
            border: '1px solid var(--border)',
            borderRadius: 'var(--r-md)',
          }}>
            <Icon name="globe" size={18} />
            <div style={{ flex: 1, fontSize: 14, fontWeight: 500 }}>Use device language</div>
            <Toggle on={false} />
          </Row>
        </div>

        <SectionLabel>All languages</SectionLabel>
        {languages.map((l, i) => (
          <div key={l.code} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '14px 20px', minHeight: 60,
            borderBottom: i < languages.length - 1 ? '1px solid var(--divider)' : 'none',
          }}>
            <div style={{
              width: 36, height: 36, borderRadius: 'var(--r-sm)',
              background: 'var(--surface-2)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: 11, fontWeight: 600, letterSpacing: 0.05,
              fontFamily: 'var(--font-mono)',
              textTransform: 'uppercase',
              color: 'var(--text-2)',
            }}>{l.code}</div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 15, fontWeight: 500 }}>{l.native}</div>
              <div style={{ fontSize: 13, color: 'var(--text-2)', marginTop: 2 }}>{l.name}</div>
            </div>
            {l.code === selected && <Icon name="check" size={18} stroke="var(--accent)" strokeWidth={2.2} />}
          </div>
        ))}
      </div>
    </Screen>
  );
}

// ─── 11 — Manage categories ──────────────────────────────────
function ManageCategoriesScreen({ dark = false, addNew = false }) {
  const [tab, setTab] = React.useState('Expense');
  const expense = [
    { name: 'Eating out',  color: '#C97A4F' },
    { name: 'Entertainment', color: '#8B6FB0' },
    { name: 'Groceries',   color: '#7A9572' },
    { name: 'Health',      color: '#C2566B' },
    { name: 'Rent',        color: '#5A7BA8' },
    { name: 'Shopping',    color: '#B07089' },
    { name: 'Transport',   color: '#4F8694' },
    { name: 'Utilities',   color: '#B89148' },
    { name: 'Other',       color: '#8A8A8A' },
  ];
  const income = [
    { name: 'Salary',      color: '#4A8E5C' },
    { name: 'Other',       color: '#8A8A8A' },
  ];
  const list = tab === 'Expense' ? expense : income;

  if (addNew) return <NewCategorySheet dark={dark} />;

  return (
    <Screen dark={dark} label>
      <ScreenHeader title="Categories" trailing={
        <Button variant="ghost" size="sm" style={{ height: 32, padding: '0 10px' }} leading={<Icon name="plus" size={16} />}>
          New
        </Button>
      } />

      <div style={{ padding: '16px 16px 12px', display: 'flex' }}>
        <Segmented options={['Expense', 'Income']} value={tab} onChange={setTab} />
      </div>

      <div style={{ flex: 1, overflow: 'visible' }}>
        <div style={{
          padding: '4px 20px 12px',
          fontSize: 12, color: 'var(--text-3)',
        }}>{list.length} {tab.toLowerCase()} categories · drag to reorder</div>

        {list.map((c, i) => (
          <div key={c.name} style={{
            display: 'flex', alignItems: 'center', gap: 14,
            padding: '12px 20px', minHeight: 60,
            borderBottom: i < list.length - 1 ? '1px solid var(--divider)' : 'none',
          }}>
            {/* drag handle */}
            <svg width="14" height="14" viewBox="0 0 14 14" style={{ color: 'var(--text-3)', flexShrink: 0 }}>
              <circle cx="4" cy="4" r="1.1" fill="currentColor"/>
              <circle cx="10" cy="4" r="1.1" fill="currentColor"/>
              <circle cx="4" cy="7" r="1.1" fill="currentColor"/>
              <circle cx="10" cy="7" r="1.1" fill="currentColor"/>
              <circle cx="4" cy="10" r="1.1" fill="currentColor"/>
              <circle cx="10" cy="10" r="1.1" fill="currentColor"/>
            </svg>
            <CategoryIcon category={c.name} size={36} radius={10} />
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>{c.name}</div>
            <Icon name="chevronRight" size={18} style={{ color: 'var(--text-3)' }} />
          </div>
        ))}

        {/* Add new at end */}
        <div style={{ padding: 20 }}>
          <Button variant="secondary" fullWidth leading={<Icon name="plus" size={16} />}>
            New {tab.toLowerCase()} category
          </Button>
        </div>
      </div>
    </Screen>
  );
}

// ─── 12 — New category sheet ─────────────────────────────────
function NewCategorySheet({ dark = false }) {
  const palette = [
    '#C2566B', '#8B6FB0', '#4A8E5C', '#4F8694', '#B89148',
    '#7A9572', '#C97A4F', '#5A7BA8', '#B07089', '#8A8A8A',
    '#D14C7A', '#6B5BC4', '#3F9E70', '#3A82A5', '#D88B33',
  ];
  const iconOptions = ['heart','film','car','bolt','basket','utensils','home','bag','tag','banknote','gift','sun','moon','globe','folder'];
  const [selectedColor, setColor] = React.useState('#4A8E5C');
  const [selectedIcon, setIcon] = React.useState('banknote');
  const selectedName = 'Subscriptions';

  return (
    <Screen dark={dark} label>
      {/* Backdrop suggesting modal sheet */}
      <div style={{
        flex: 1, position: 'relative',
        background: dark ? 'rgba(0,0,0,0.5)' : 'rgba(0,0,0,0.25)',
      }}>
        <div style={{
          position: 'absolute', bottom: 0, left: 0, right: 0,
          background: 'var(--bg)',
          borderRadius: '20px 20px 0 0',
          paddingBottom: 24,
          maxHeight: '92%',
          display: 'flex', flexDirection: 'column',
        }}>
          <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 0 4px' }}>
            <div style={{ width: 36, height: 4, borderRadius: 999, background: 'var(--border-strong)' }} />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '8px 12px 4px' }}>
            <IconButton icon="close" />
            <div style={{ flex: 1, fontSize: 17, fontWeight: 600, letterSpacing: -0.2, textAlign: 'center' }}>New category</div>
            <div style={{ width: 40 }} />
          </div>

          <div style={{ padding: '16px 20px 0', overflow: 'auto', flex: 1 }}>
            {/* Preview */}
            <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 24 }}>
              <div style={{
                display: 'inline-flex', alignItems: 'center', gap: 10,
                padding: '10px 18px 10px 12px', borderRadius: 999,
                background: 'var(--surface)',
                border: '1px solid var(--border)',
              }}>
                <div style={{
                  width: 28, height: 28, borderRadius: 8,
                  background: selectedColor, color: '#fff',
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                }}>
                  <Icon name={selectedIcon} size={16} stroke="#fff" strokeWidth={1.8} />
                </div>
                <span style={{ fontSize: 15, fontWeight: 500 }}>{selectedName}</span>
              </div>
            </div>

            <Field label="Name" value={selectedName} />

            <div style={{ marginTop: 20 }}>
              <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--text-2)', marginBottom: 8 }}>Type</div>
              <Segmented options={['Expense', 'Income']} value="Expense" />
            </div>

            <div style={{ marginTop: 24 }}>
              <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--text-2)', marginBottom: 12 }}>Color</div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                {palette.map(c => (
                  <button key={c} onClick={() => setColor(c)} style={{
                    width: 36, height: 36, borderRadius: 10,
                    background: c, border: 'none', cursor: 'pointer',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                    boxShadow: c === selectedColor ? '0 0 0 2px var(--bg), 0 0 0 4px var(--text)' : 'none',
                  }}>
                    {c === selectedColor && <Icon name="check" size={16} stroke="#fff" strokeWidth={2.5} />}
                  </button>
                ))}
              </div>
            </div>

            <div style={{ marginTop: 24 }}>
              <div style={{ fontSize: 12, fontWeight: 500, color: 'var(--text-2)', marginBottom: 12 }}>Icon</div>
              <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10 }}>
                {iconOptions.map(ic => {
                  const isSel = ic === selectedIcon;
                  return (
                    <button key={ic} onClick={() => setIcon(ic)} style={{
                      width: 44, height: 44, borderRadius: 10,
                      background: isSel ? selectedColor : 'var(--surface)',
                      border: `1px solid ${isSel ? selectedColor : 'var(--border)'}`,
                      cursor: 'pointer',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                    }}>
                      <Icon name={ic} size={20} stroke={isSel ? '#fff' : 'var(--text)'} strokeWidth={1.6} />
                    </button>
                  );
                })}
              </div>
            </div>
          </div>

          <div style={{ padding: '16px 20px 0', borderTop: '1px solid var(--divider)' }}>
            <Button variant="accent" size="lg" fullWidth leading={<Icon name="check" size={18} strokeWidth={2} />}>
              Create category
            </Button>
          </div>
        </div>
      </div>
    </Screen>
  );
}

// ─── 13 — Transaction list display preferences ───────────────
function TxListDisplayScreen({ dark = false }) {
  const [style, setStyle] = React.useState('icon-tile');
  const [showCat, setShowCat] = React.useState(true);
  const [showNote, setShowNote] = React.useState(true);
  const [density, setDensity] = React.useState('comfortable');
  const prefs = { style, showCategoryName: showCat, showNote, density };

  const sample = [
    { cat: 'Entertainment', note: 'Royal Albert Hall',  amount: -333.00 },
    { cat: 'Groceries',     note: 'Weekly shop · Edeka',amount: -87.40 },
    { cat: 'Salary',        note: 'May payroll',        amount: +3500.00 },
  ];

  const styleOptions = [
    { id: 'icon-tile', label: 'Icon tile',   sub: 'Filled color tile, white icon' },
    { id: 'soft-icon', label: 'Soft icon',   sub: 'Tinted tile, colored icon' },
    { id: 'bar',       label: 'Color bar',   sub: 'Vertical accent bar' },
    { id: 'dot',       label: 'Color dot',   sub: 'Subtle 8px dot' },
    { id: 'none',      label: 'Minimal',     sub: 'No color indicator' },
  ];

  return (
    <Screen dark={dark} label>
      <ScreenHeader title="Transaction list" />

      <div style={{ flex: 1, overflow: 'visible' }}>
        {/* Live preview */}
        <div style={{
          padding: '20px 16px 24px',
          background: 'var(--surface-2)',
          borderBottom: '1px solid var(--divider)',
        }}>
          <div style={{
            fontSize: 11, fontWeight: 600, letterSpacing: 0.08,
            textTransform: 'uppercase', color: 'var(--text-3)', marginBottom: 12,
            padding: '0 4px',
          }}>Preview</div>
          <Card style={{ overflow: 'hidden' }}>
            {sample.map((t, i) => (
              <TxRow key={i} tx={t} prefs={prefs} divider={i < sample.length - 1} />
            ))}
          </Card>
        </div>

        {/* Color indicator style */}
        <SectionLabel>Color indicator</SectionLabel>
        <Card style={{ overflow: 'hidden', margin: '0 16px' }}>
          {styleOptions.map((opt, i) => (
            <div
              key={opt.id}
              onClick={() => setStyle(opt.id)}
              style={{
                display: 'flex', alignItems: 'center', gap: 14,
                padding: '14px 16px',
                borderBottom: i < styleOptions.length - 1 ? '1px solid var(--divider)' : 'none',
                cursor: 'pointer',
              }}
            >
              {/* Mini sample of this option */}
              <div style={{ width: 38, display: 'flex', justifyContent: 'center' }}>
                {opt.id === 'icon-tile' && <CategoryIcon category="Entertainment" size={32} radius={9} variant="tile" />}
                {opt.id === 'soft-icon' && <CategoryIcon category="Entertainment" size={32} radius={9} variant="soft" />}
                {opt.id === 'bar'       && <CategoryIcon category="Entertainment" size={32} variant="bar" />}
                {opt.id === 'dot'       && <CatDot category="Entertainment" size={10} />}
                {opt.id === 'none'      && <div style={{ width: 32, height: 1, background: 'var(--border)' }} />}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 15, fontWeight: 500 }}>{opt.label}</div>
                <div style={{ fontSize: 12, color: 'var(--text-2)', marginTop: 2 }}>{opt.sub}</div>
              </div>
              <span style={{
                width: 22, height: 22, borderRadius: 999,
                border: `1.5px solid ${style === opt.id ? 'var(--accent)' : 'var(--border-strong)'}`,
                background: style === opt.id ? 'var(--accent)' : 'transparent',
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                flexShrink: 0,
              }}>
                {style === opt.id && <Icon name="check" size={12} stroke="#fff" strokeWidth={3} />}
              </span>
            </div>
          ))}
        </Card>

        {/* Content toggles */}
        <SectionLabel>Show</SectionLabel>
        <Card style={{ overflow: 'hidden', margin: '0 16px' }}>
          <div
            onClick={() => setShowCat(!showCat)}
            style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px', borderBottom: '1px solid var(--divider)', cursor: 'pointer' }}
          >
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Category name</div>
            <Toggle on={showCat} />
          </div>
          <div
            onClick={() => setShowNote(!showNote)}
            style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '14px 16px', cursor: 'pointer' }}
          >
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Note / description</div>
            <Toggle on={showNote} />
          </div>
        </Card>

        {/* Density */}
        <SectionLabel>Density</SectionLabel>
        <Card style={{ overflow: 'hidden', margin: '0 16px 24px' }}>
          <Row padding="10px 16px" divider={false}>
            <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>Row size</div>
            <Segmented options={['Compact', 'Comfortable']}
                       value={density === 'compact' ? 'Compact' : 'Comfortable'}
                       onChange={v => setDensity(v.toLowerCase())} />
          </Row>
        </Card>
      </div>
    </Screen>
  );
}

// ─── 04 — Category picker (modal sheet) ──────────────────────
function CategoryPickerScreen({ dark = false }) {
  const cats = ['Eating out','Entertainment','Groceries','Health','Rent','Salary','Shopping','Transport','Utilities','Other'];
  return (
    <Screen dark={dark} label>
      {/* Backdrop suggesting modal sheet */}
      <div style={{
        flex: 1, position: 'relative',
        background: dark ? 'rgba(0,0,0,0.5)' : 'rgba(0,0,0,0.25)',
      }}>
        <div style={{
          position: 'absolute', bottom: 0, left: 0, right: 0,
          background: 'var(--bg)',
          borderRadius: '20px 20px 0 0',
          paddingBottom: 24,
          maxHeight: '75%',
          overflow: 'auto',
        }}>
          {/* Grabber */}
          <div style={{ display: 'flex', justifyContent: 'center', padding: '10px 0 4px' }}>
            <div style={{ width: 36, height: 4, borderRadius: 999, background: 'var(--border-strong)' }} />
          </div>

          {/* Header */}
          <div style={{
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            padding: '8px 20px 12px',
          }}>
            <div style={{ fontSize: 17, fontWeight: 600, letterSpacing: -0.2 }}>Choose category</div>
            <IconButton icon="close" size={32} />
          </div>

          {/* Search */}
          <div style={{ padding: '0 20px 12px' }}>
            <Field placeholder="Search categories" />
          </div>

          {/* List */}
          {cats.map((c, i) => (
            <Row key={c} divider={i < cats.length - 1} padding="12px 20px">
              <CategoryIcon category={c} size={36} radius={10} />
              <div style={{ flex: 1, fontSize: 15, fontWeight: 500 }}>{c}</div>
              {c === 'Entertainment' && <Icon name="check" size={18} stroke="var(--accent)" strokeWidth={2} />}
            </Row>
          ))}

          {/* Add new */}
          <div style={{ padding: '16px 20px' }}>
            <Button variant="secondary" fullWidth leading={<Icon name="plus" size={16} />}>
              New category
            </Button>
          </div>
        </div>
      </div>
    </Screen>
  );
}

Object.assign(window, {
  Screen, ScreenHeader, CumulativeChart, TxRow,
  PinScreen, TransactionsScreen, AddTxScreen,
  OverviewMonthScreen, OverviewYearScreen, SettingsScreen,
  CategoryPickerScreen,
  CurrencyPickerScreen, LanguagePickerScreen,
  ManageCategoriesScreen, NewCategorySheet,
  TxListDisplayScreen,
});

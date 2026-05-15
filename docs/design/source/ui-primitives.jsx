// MoneyM UI Primitives — used by both the component showcase and the screens.
// All components consume CSS variables defined in tokens.css.

// ─── Theme wrapper ───────────────────────────────────────────
function MMTheme({ dark = false, children, style = {}, width, height }) {
  return (
    <div
      className={`mm-theme ${dark ? 'mm-dark' : 'mm-light'}`}
      style={{
        width: width || '100%',
        height: height || '100%',
        background: 'var(--bg)',
        color: 'var(--text)',
        overflow: 'hidden',
        ...style,
      }}
    >
      {children}
    </div>
  );
}

// ─── Icon set ────────────────────────────────────────────────
// Stroke-based, 1.6px, 24px viewBox. Plain, calm.
function Icon({ name, size = 20, stroke = 'currentColor', strokeWidth = 1.6, style }) {
  const paths = {
    chevronLeft:  <polyline points="15 6 9 12 15 18" />,
    chevronRight: <polyline points="9 6 15 12 9 18" />,
    chevronDown:  <polyline points="6 9 12 15 18 9" />,
    close:        <><line x1="6" y1="6" x2="18" y2="18"/><line x1="18" y1="6" x2="6" y2="18"/></>,
    check:        <polyline points="5 12 10 17 19 7" />,
    plus:         <><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></>,
    minus:        <line x1="5" y1="12" x2="19" y2="12"/>,
    trash:        <><polyline points="4 7 20 7"/><path d="M9 7V5a2 2 0 0 1 2-2h2a2 2 0 0 1 2 2v2"/><path d="M6 7l1 13a2 2 0 0 0 2 2h6a2 2 0 0 0 2-2l1-13"/></>,
    backspace:    <><path d="M21 5H9l-6 7 6 7h12a1 1 0 0 0 1-1V6a1 1 0 0 0-1-1z"/><line x1="14" y1="9" x2="18" y2="13"/><line x1="18" y1="9" x2="14" y2="13"/></>,
    calendar:     <><rect x="3" y="5" width="18" height="16" rx="2"/><line x1="3" y1="10" x2="21" y2="10"/><line x1="8" y1="3" x2="8" y2="7"/><line x1="16" y1="3" x2="16" y2="7"/></>,
    list:         <><line x1="8" y1="6" x2="20" y2="6"/><line x1="8" y1="12" x2="20" y2="12"/><line x1="8" y1="18" x2="20" y2="18"/><circle cx="4" cy="6" r="1" fill={stroke} stroke="none"/><circle cx="4" cy="12" r="1" fill={stroke} stroke="none"/><circle cx="4" cy="18" r="1" fill={stroke} stroke="none"/></>,
    chart:        <><path d="M4 19V9"/><path d="M10 19V5"/><path d="M16 19v-7"/><path d="M3 21h18"/></>,
    settings:     <><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></>,
    faceId:       <><path d="M5 9V7a2 2 0 0 1 2-2h2"/><path d="M19 9V7a2 2 0 0 0-2-2h-2"/><path d="M5 15v2a2 2 0 0 0 2 2h2"/><path d="M19 15v2a2 2 0 0 1-2 2h-2"/><line x1="9" y1="10" x2="9" y2="11"/><line x1="15" y1="10" x2="15" y2="11"/><path d="M12 9v4"/><path d="M9 15c.6.6 1.6 1 3 1s2.4-.4 3-1"/></>,
    lock:         <><rect x="5" y="11" width="14" height="10" rx="2"/><path d="M8 11V8a4 4 0 0 1 8 0v3"/></>,
    globe:        <><circle cx="12" cy="12" r="9"/><path d="M3 12h18"/><path d="M12 3a14 14 0 0 1 0 18 14 14 0 0 1 0-18"/></>,
    download:     <><path d="M12 3v13"/><polyline points="7 11 12 16 17 11"/><path d="M5 20h14"/></>,
    folder:       <path d="M4 7a2 2 0 0 1 2-2h3l2 2h7a2 2 0 0 1 2 2v9a2 2 0 0 1-2 2H6a2 2 0 0 1-2-2V7z"/>,
    info:         <><circle cx="12" cy="12" r="9"/><line x1="12" y1="11" x2="12" y2="16"/><circle cx="12" cy="8" r="0.5" fill={stroke}/></>,
    sun:          <><circle cx="12" cy="12" r="4"/><line x1="12" y1="2" x2="12" y2="4"/><line x1="12" y1="20" x2="12" y2="22"/><line x1="2" y1="12" x2="4" y2="12"/><line x1="20" y1="12" x2="22" y2="12"/><line x1="4.9" y1="4.9" x2="6.3" y2="6.3"/><line x1="17.7" y1="17.7" x2="19.1" y2="19.1"/><line x1="4.9" y1="19.1" x2="6.3" y2="17.7"/><line x1="17.7" y1="6.3" x2="19.1" y2="4.9"/></>,
    moon:         <path d="M21 12.8A9 9 0 1 1 11.2 3a7 7 0 0 0 9.8 9.8z"/>,
    search:       <><circle cx="11" cy="11" r="7"/><line x1="16.5" y1="16.5" x2="21" y2="21"/></>,
    arrowUp:      <><line x1="12" y1="19" x2="12" y2="5"/><polyline points="6 11 12 5 18 11"/></>,
    arrowDown:    <><line x1="12" y1="5" x2="12" y2="19"/><polyline points="6 13 12 19 18 13"/></>,

    // Category icons
    heart:        <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78L12 21.23l8.84-8.84a5.5 5.5 0 0 0 0-7.78z"/>,
    film:         <><rect x="3" y="4" width="18" height="16" rx="2"/><line x1="7" y1="4" x2="7" y2="20"/><line x1="17" y1="4" x2="17" y2="20"/><line x1="3" y1="9" x2="7" y2="9"/><line x1="17" y1="9" x2="21" y2="9"/><line x1="3" y1="15" x2="7" y2="15"/><line x1="17" y1="15" x2="21" y2="15"/></>,
    car:          <><path d="M5 17V12l2-5h10l2 5v5"/><line x1="3" y1="17" x2="21" y2="17"/><circle cx="7.5" cy="17.5" r="1.6"/><circle cx="16.5" cy="17.5" r="1.6"/></>,
    bolt:         <path d="M13 2L4 14h7l-1 8 9-12h-7l1-8z"/>,
    basket:       <><path d="M4 10l-1 10a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2l-1-10"/><polyline points="7 10 12 4 17 10"/><line x1="2" y1="10" x2="22" y2="10"/></>,
    utensils:     <><path d="M6 3v8a2 2 0 0 0 2 2v9"/><path d="M10 3v8a2 2 0 0 1-2 2"/><path d="M17 3c-2 0-3 1-3 3v6c0 1 1 2 2 2h1v8"/></>,
    home:         <><path d="M3 11l9-8 9 8"/><path d="M5 10v10a1 1 0 0 0 1 1h4v-7h4v7h4a1 1 0 0 0 1-1V10"/></>,
    bag:          <><path d="M6 7h12l-1 13a2 2 0 0 1-2 2H9a2 2 0 0 1-2-2L6 7z"/><path d="M9 7V5a3 3 0 0 1 6 0v2"/></>,
    tag:          <><path d="M20.59 13.41L13.41 20.6a2 2 0 0 1-2.83 0L3 13V3h10l7.59 7.59a2 2 0 0 1 0 2.82z"/><circle cx="7.5" cy="7.5" r="1.5"/></>,
    banknote:     <><rect x="2" y="6" width="20" height="12" rx="2"/><circle cx="12" cy="12" r="2.5"/><line x1="5.5" y1="9" x2="5.5" y2="9.01"/><line x1="18.5" y1="15" x2="18.5" y2="15.01"/></>,
    gift:         <><rect x="3" y="8" width="18" height="13" rx="1"/><line x1="12" y1="8" x2="12" y2="21"/><path d="M3 12h18"/><path d="M12 8c-1-3-3-5-5-5a2 2 0 0 0 0 5h5z"/><path d="M12 8c1-3 3-5 5-5a2 2 0 0 1 0 5h-5z"/></>,
    fingerprint:  <><path d="M12 11v3a8 8 0 0 1-1 4"/><path d="M8 11a4 4 0 0 1 8 0v3"/><path d="M5 11a7 7 0 0 1 14 0v1"/><path d="M19 14a14 14 0 0 1-.5 4"/><path d="M14 17a3 3 0 0 1-.5 2"/><path d="M12 7v0"/></>,
    eye:          <><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></>,
    sliders:      <><line x1="4" y1="6" x2="20" y2="6"/><line x1="4" y1="12" x2="20" y2="12"/><line x1="4" y1="18" x2="20" y2="18"/><circle cx="9" cy="6" r="2" fill="var(--bg)"/><circle cx="15" cy="12" r="2" fill="var(--bg)"/><circle cx="11" cy="18" r="2" fill="var(--bg)"/></>,
  };
  return (
    <svg
      width={size} height={size} viewBox="0 0 24 24"
      fill="none" stroke={stroke} strokeWidth={strokeWidth}
      strokeLinecap="round" strokeLinejoin="round"
      style={style}
    >
      {paths[name]}
    </svg>
  );
}

// ─── Category color helper ───────────────────────────────────
const CATS = {
  Health:        { color: 'var(--cat-health)',        hex: '#C2566B', icon: 'heart' },
  Entertainment: { color: 'var(--cat-entertainment)', hex: '#8B6FB0', icon: 'film' },
  Salary:        { color: 'var(--cat-salary)',        hex: '#4A8E5C', icon: 'banknote' },
  Transport:     { color: 'var(--cat-transport)',     hex: '#4F8694', icon: 'car' },
  Utilities:     { color: 'var(--cat-utilities)',     hex: '#B89148', icon: 'bolt' },
  Groceries:     { color: 'var(--cat-groceries)',     hex: '#7A9572', icon: 'basket' },
  'Eating out':  { color: 'var(--cat-eatingout)',     hex: '#C97A4F', icon: 'utensils' },
  Rent:          { color: 'var(--cat-rent)',          hex: '#5A7BA8', icon: 'home' },
  Shopping:      { color: 'var(--cat-shopping)',      hex: '#B07089', icon: 'bag' },
  Other:         { color: 'var(--cat-other)',         hex: '#8A8A8A', icon: 'tag' },
  'Other (expense)': { color: 'var(--cat-other)',     hex: '#8A8A8A', icon: 'tag' },
  'Other (income)':  { color: 'var(--cat-other)',     hex: '#8A8A8A', icon: 'gift' },
};

// ─── Category icon tile (filled colored square w/ white icon) ─
function CategoryIcon({ category, size = 36, radius = 10, variant = 'tile' }) {
  const cat = CATS[category] || CATS.Other;
  if (variant === 'tile') {
    return (
      <div style={{
        width: size, height: size, borderRadius: radius,
        background: cat.color, color: '#fff',
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}>
        <Icon name={cat.icon} size={Math.round(size * 0.55)} stroke="#fff" strokeWidth={1.8} />
      </div>
    );
  }
  if (variant === 'soft') {
    return (
      <div style={{
        width: size, height: size, borderRadius: radius,
        background: cat.color + '22',
        color: cat.color,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
      }}>
        <Icon name={cat.icon} size={Math.round(size * 0.55)} stroke={cat.color} strokeWidth={1.8} />
      </div>
    );
  }
  if (variant === 'bar') {
    return (
      <div style={{
        width: 4, height: size, borderRadius: 2,
        background: cat.color, flexShrink: 0,
      }} />
    );
  }
  // dot fallback
  return <CatDot category={category} size={10} />;
}

// ─── Small dot indicator (used in list rows) ─────────────────
function CatDot({ category, size = 8 }) {
  const cat = CATS[category] || CATS.Other;
  return (
    <span style={{
      width: size, height: size, borderRadius: 999,
      background: cat.color, flexShrink: 0,
    }} />
  );
}

// ─── Buttons ─────────────────────────────────────────────────
function Button({ variant = 'primary', size = 'md', children, leading, trailing, onClick, style, fullWidth, disabled = false }) {
  const sizes = {
    sm: { h: 32, px: 12, fs: 13 },
    md: { h: 44, px: 16, fs: 15 },
    lg: { h: 52, px: 20, fs: 16 },
  }[size];
  const variants = {
    primary:   { bg: 'var(--btn-primary-bg)',   fg: 'var(--btn-primary-fg)',   bd: 'transparent' },
    secondary: { bg: 'var(--btn-secondary-bg)', fg: 'var(--btn-secondary-fg)', bd: 'var(--border)' },
    ghost:     { bg: 'transparent',             fg: 'var(--text)',             bd: 'transparent' },
    outline:   { bg: 'transparent',             fg: 'var(--text)',             bd: 'var(--border-strong)' },
    accent:    { bg: 'var(--accent)',           fg: '#fff',                    bd: 'transparent' },
    danger:    { bg: 'transparent',             fg: '#DC2626',                 bd: 'var(--border)' },
  }[variant];
  // Disabled visual: surface-2 bg + text-3 fg, no shadow. Same for every
  // variant so the affordance is unambiguous.
  const disabledStyle = disabled ? {
    background: 'var(--surface-2)',
    color: 'var(--text-3)',
    border: '1px solid var(--border)',
    cursor: 'not-allowed',
  } : null;
  return (
    <button
      onClick={disabled ? undefined : onClick}
      disabled={disabled}
      style={{
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        gap: 8, height: sizes.h, padding: `0 ${sizes.px}px`,
        borderRadius: 'var(--r-md)',
        background: variants.bg, color: variants.fg,
        border: `1px solid ${variants.bd}`,
        fontFamily: 'var(--font-sans)', fontWeight: 500, fontSize: sizes.fs,
        letterSpacing: -0.1, cursor: 'pointer',
        width: fullWidth ? '100%' : 'auto',
        ...disabledStyle,
        ...style,
      }}
    >
      {leading}
      {children}
      {trailing}
    </button>
  );
}

// ─── Icon Button (header actions) ────────────────────────────
function IconButton({ icon, onClick, size = 40, style, danger = false, accent = false }) {
  let color = 'var(--text)';
  if (danger) color = '#DC2626';
  if (accent) color = 'var(--accent)';
  return (
    <button
      onClick={onClick}
      style={{
        width: size, height: size, borderRadius: 999,
        display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
        background: 'transparent', border: 'none', cursor: 'pointer',
        color, padding: 0,
        ...style,
      }}
    >
      <Icon name={icon} size={20} />
    </button>
  );
}

// ─── Segmented control ───────────────────────────────────────
function Segmented({ options, value, onChange, size = 'md', style }) {
  const h = size === 'sm' ? 32 : 36;
  return (
    <div style={{
      display: 'inline-flex', height: h, padding: 3,
      background: 'var(--surface-2)', borderRadius: 'var(--r-pill)',
      ...style,
    }}>
      {options.map(opt => {
        const v = typeof opt === 'string' ? opt : opt.value;
        const label = typeof opt === 'string' ? opt : opt.label;
        const active = v === value;
        return (
          <button
            key={v}
            onClick={() => onChange && onChange(v)}
            style={{
              height: h - 6, padding: '0 14px',
              border: 'none', borderRadius: 'var(--r-pill)',
              background: active ? 'var(--bg)' : 'transparent',
              color: active ? 'var(--text)' : 'var(--text-2)',
              fontFamily: 'var(--font-sans)', fontSize: 13,
              fontWeight: active ? 600 : 500,
              letterSpacing: -0.1, cursor: 'pointer',
              boxShadow: active ? '0 1px 2px rgba(0,0,0,0.06), 0 0 0 1px rgba(0,0,0,0.04)' : 'none',
              transition: 'background 0.15s, color 0.15s',
            }}
          >{label}</button>
        );
      })}
    </div>
  );
}

// ─── Chip / Tag (category select, filters) ───────────────────
function Chip({ children, selected = false, leading, onClick, style }) {
  return (
    <button
      onClick={onClick}
      style={{
        display: 'inline-flex', alignItems: 'center', gap: 8,
        height: 34, padding: leading ? '0 14px 0 12px' : '0 14px',
        borderRadius: 'var(--r-pill)',
        border: `1px solid ${selected ? 'var(--selected-bg)' : 'var(--chip-border)'}`,
        background: selected ? 'var(--selected-bg)' : 'transparent',
        color: selected ? 'var(--selected-fg)' : 'var(--text)',
        fontFamily: 'var(--font-sans)', fontSize: 13, fontWeight: 500,
        cursor: 'pointer', letterSpacing: -0.1,
        ...style,
      }}
    >
      {leading}
      {children}
    </button>
  );
}

// ─── Text Field ──────────────────────────────────────────────
function Field({ label, value, placeholder, suffix, prefix, mono = false, style, multiline = false }) {
  return (
    <div style={{ ...style }}>
      {label && (
        <div style={{
          fontSize: 12, fontWeight: 500, letterSpacing: 0.02,
          color: 'var(--text-2)', marginBottom: 8,
        }}>{label}</div>
      )}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 8,
        height: multiline ? 'auto' : 52, minHeight: multiline ? 96 : 52,
        padding: '0 16px',
        background: 'var(--surface)',
        border: '1px solid var(--border)',
        borderRadius: 'var(--r-md)',
      }}>
        {prefix && <span style={{ color: 'var(--text-3)', fontSize: 15 }}>{prefix}</span>}
        <span style={{
          flex: 1,
          fontFamily: mono ? 'var(--font-mono)' : 'var(--font-sans)',
          fontSize: 15, color: value ? 'var(--text)' : 'var(--text-3)',
          padding: multiline ? '14px 0' : 0,
        }}>{value || placeholder}</span>
        {suffix && <span style={{ color: 'var(--text-3)', fontSize: 13 }}>{suffix}</span>}
      </div>
    </div>
  );
}

// ─── Toggle switch ───────────────────────────────────────────
function Toggle({ on = false, disabled = false }) {
  return (
    <span style={{
      width: 44, height: 26, borderRadius: 999,
      background: on ? 'var(--text)' : 'var(--surface-2)',
      border: `1px solid ${on ? 'var(--text)' : 'var(--border)'}`,
      position: 'relative', flexShrink: 0,
      transition: 'background 0.2s',
      display: 'inline-block',
      opacity: disabled ? 0.45 : 1,
      cursor: disabled ? 'not-allowed' : 'pointer',
    }}>
      <span style={{
        position: 'absolute', top: 2, left: on ? 20 : 2,
        width: 20, height: 20, borderRadius: 999,
        background: 'var(--bg)',
        transition: 'left 0.2s',
        boxShadow: '0 1px 2px rgba(0,0,0,0.1)',
      }} />
    </span>
  );
}

// ─── List row ────────────────────────────────────────────────
function Row({ children, onClick, style, divider = true, padding = '14px 20px' }) {
  return (
    <div
      onClick={onClick}
      style={{
        display: 'flex', alignItems: 'center', gap: 12,
        padding, minHeight: 56,
        borderBottom: divider ? '1px solid var(--divider)' : 'none',
        cursor: onClick ? 'pointer' : 'default',
        ...style,
      }}
    >
      {children}
    </div>
  );
}

// ─── Section label ───────────────────────────────────────────
function SectionLabel({ children, style }) {
  return (
    <div style={{
      padding: '20px 20px 8px',
      fontSize: 11, fontWeight: 600, letterSpacing: 0.08,
      textTransform: 'uppercase',
      color: 'var(--text-3)',
      ...style,
    }}>{children}</div>
  );
}

// ─── Tab Bar (bottom nav) ────────────────────────────────────
function TabBar({ active = 'transactions', dark }) {
  const tabs = [
    { id: 'transactions', label: 'Transactions', icon: 'list' },
    { id: 'overview',     label: 'Overview',     icon: 'chart' },
    { id: 'settings',     label: 'Settings',     icon: 'settings' },
  ];
  return (
    <div style={{
      display: 'flex',
      borderTop: '1px solid var(--divider)',
      background: 'var(--bg)',
      paddingBottom: 24, paddingTop: 8,
    }}>
      {tabs.map(t => {
        const isActive = t.id === active;
        return (
          <div key={t.id} style={{
            flex: 1, display: 'flex', flexDirection: 'column',
            alignItems: 'center', gap: 4, padding: '8px 0',
            color: isActive ? 'var(--text)' : 'var(--text-3)',
          }}>
            <Icon name={t.icon} size={22} strokeWidth={isActive ? 1.8 : 1.5} />
            <span style={{
              fontSize: 10, fontWeight: isActive ? 600 : 500,
              letterSpacing: 0.02,
            }}>{t.label}</span>
          </div>
        );
      })}
    </div>
  );
}

// ─── App status bar (faux iOS / generic) ─────────────────────
function AppStatusBar({ dark = false, time = '9:41' }) {
  const c = 'var(--text)';
  return (
    <div style={{
      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
      padding: '14px 24px 8px', height: 44,
    }}>
      <span style={{ fontFamily: 'var(--font-sans)', fontWeight: 600, fontSize: 15, color: c }}>{time}</span>
      <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
        <svg width="17" height="11" viewBox="0 0 17 11">
          <rect x="0" y="7" width="3" height="4" rx="0.5" fill="currentColor"/>
          <rect x="4.5" y="5" width="3" height="6" rx="0.5" fill="currentColor"/>
          <rect x="9" y="2.5" width="3" height="8.5" rx="0.5" fill="currentColor"/>
          <rect x="13.5" y="0" width="3" height="11" rx="0.5" fill="currentColor"/>
        </svg>
        <svg width="24" height="11" viewBox="0 0 24 11" style={{ color: 'currentColor' }}>
          <rect x="0.5" y="0.5" width="20" height="10" rx="3" stroke="currentColor" strokeOpacity="0.4" fill="none"/>
          <rect x="2" y="2" width="14" height="7" rx="1.5" fill="currentColor"/>
          <rect x="21.5" y="3.5" width="1.5" height="4" rx="0.5" fill="currentColor" fillOpacity="0.4"/>
        </svg>
      </div>
    </div>
  );
}

// ─── Card (surface container) ────────────────────────────────
function Card({ children, style, padded = false }) {
  return (
    <div style={{
      background: 'var(--surface)',
      borderRadius: 'var(--r-lg)',
      border: '1px solid var(--border)',
      padding: padded ? 20 : 0,
      ...style,
    }}>
      {children}
    </div>
  );
}

// ─── Money display (mono, signed) ────────────────────────────
function Money({ value, sign = null, size = 15, weight = 500, currency = 'EUR', muted = false, style }) {
  const num = Math.abs(value).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
  const s = sign === '+' ? '+' : sign === '-' ? '−' : '';
  return (
    <span className="mm-mono" style={{
      fontSize: size, fontWeight: weight, letterSpacing: -0.2,
      color: muted ? 'var(--text-2)' : 'var(--text)',
      ...style,
    }}>
      {s}{currency} {num}
    </span>
  );
}

// ─── Mini bar chart (per-category sparkline) ─────────────────
function MiniBars({ data, color, height = 28, max, gap = 1.5, highlight = -1 }) {
  const m = max || Math.max(...data, 1);
  return (
    <div style={{ display: 'flex', alignItems: 'flex-end', gap, height, width: '100%' }}>
      {data.map((v, i) => (
        <div key={i} style={{
          flex: 1, minWidth: 0,
          height: v ? Math.max(1.5, (v / m) * height) : 1.5,
          background: color,
          borderRadius: 1.5,
          opacity: v ? (i === highlight ? 1 : 0.7) : 0.18,
        }} />
      ))}
    </div>
  );
}

Object.assign(window, {
  MMTheme, Icon, CATS, CatDot, CategoryIcon,
  Button, IconButton, Segmented, Chip, Field, Toggle, Row,
  SectionLabel, TabBar, AppStatusBar, Card, Money, MiniBars,
});

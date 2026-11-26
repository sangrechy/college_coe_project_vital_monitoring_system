/**
 * Shared presentational icons (inline SVG, stroke style).
 * UI only — no logic.
 */

const PATHS = {
  grid:    <><rect x="3" y="3" width="7" height="7" rx="1.8"/><rect x="14" y="3" width="7" height="7" rx="1.8"/><rect x="3" y="14" width="7" height="7" rx="1.8"/><rect x="14" y="14" width="7" height="7" rx="1.8"/></>,
  pulse:   <path d="M2 12h5l2.5-6 5 12 2.5-6H22"/>,
  heart:   <path d="M12 20.5s-7.5-4.6-9.4-9.8C1.4 7.3 3.3 4 6.8 4c2 0 3.6 1.1 5.2 3.1C13.600 5.100 15.200 4 17.200 4c3.500 0 5.400 3.300 4.200 6.700-1.900 5.200-9.400 9.800-9.400 9.800z"/>,
  chip:    <><rect x="6" y="6" width="12" height="12" rx="2.2"/><path d="M9 2.500v3.500M15 2.500v3.500M9 18v3.500M15 18v3.500M2.500 9H6M2.500 15H6M18 9h3.500M18 15h3.500"/></>,
  trend:   <><path d="M3 3v18h18"/><path d="M7 15l4-5 3 3.500 5-7"/></>,
  device:  <><rect x="6.500" y="2.500" width="11" height="19" rx="3.200"/><path d="M10.500 18h3"/></>,
  drop:    <path d="M12 3s6.500 6.700 6.500 11.200A6.500 6.500 0 0 1 12 20.700a6.500 6.500 0 0 1-6.500-6.500C5.500 9.700 12 3 12 3z"/>,
  thermo:  <path d="M14 14.800V5a2 2 0 1 0-4 0v9.800a4 4 0 1 0 4 0z"/>,
  motion:  <><path d="M3 12h3l2-5 4 10 2.500-7 1.500 2H21"/></>,
  wave:    <path d="M2 12c2.500 0 2.500-6 5-6s2.500 12 5 12 2.500-12 5-12 2.500 6 5 6"/>,
  check:   <path d="M5 12.500l4.500 4.500L19 7.500"/>,
  alert:   <><path d="M12 3.500l9.500 16.500h-19L12 3.500z"/><path d="M12 10v4.500M12 17.300v.2"/></>,
  info:    <><circle cx="12" cy="12" r="9"/><path d="M12 11v5.500M12 7.800v.2"/></>,
  idle:    <><circle cx="12" cy="12" r="9"/><path d="M8.500 12h7"/></>,
  eye:     <><path d="M2 12s3.600-6.500 10-6.500S22 12 22 12s-3.600 6.500-10 6.500S2 12 2 12z"/><circle cx="12" cy="12" r="2.800"/></>,
  bolt:    <path d="M13 2.500L4.500 13.500H11l-1 8 8.500-11H12l1-8z"/>,
  refresh: <><path d="M20 11a8 8 0 0 0-14.500-4M4 5v3.500h3.500"/><path d="M4 13a8 8 0 0 0 14.500 4M20 19v-3.500h-3.500"/></>,
  bluetooth: <path d="M7 7.500l10 9-5 4.500V3l5 4.500-10 9"/>,
};

export default function Icon({ name, size = 18, stroke = 1.8, style, className }) {
  return (
    <svg
      className={className}
      viewBox="0 0 24 24"
      width={size}
      height={size}
      fill="none"
      stroke="currentColor"
      strokeWidth={stroke}
      strokeLinecap="round"
      strokeLinejoin="round"
      style={{ flexShrink: 0, ...style }}
      aria-hidden="true"
    >
      {PATHS[name] || null}
    </svg>
  );
}

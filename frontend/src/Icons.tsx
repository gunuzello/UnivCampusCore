import type { ReactNode } from "react";
function Icon({ children, size = 19 }: { children: ReactNode; size?: number }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {children}
    </svg>
  );
}
export function Home({ size }: { size?: number }) {
  return (
    <Icon size={size}>
      <path d="M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z" />
    </Icon>
  );
}
export function Compass({ size }: { size?: number }) {
  return (
    <Icon size={size}>
      <circle cx="12" cy="12" r="9" />
      <path d="m15.5 8.5-2 5-5 2 2-5z" />
    </Icon>
  );
}
export function CalendarDays({ size }: { size?: number }) {
  return (
    <Icon size={size}>
      <rect x="3" y="4" width="18" height="18" rx="2" />
      <path d="M16 2v4M8 2v4M3 10h18" />
    </Icon>
  );
}
export function Users({ size }: { size?: number }) {
  return (
    <Icon size={size}>
      <path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M22 21v-2a4 4 0 0 0-3-3.87" />
      <circle cx="9" cy="7" r="4" />
      <path d="M16 3.13a4 4 0 0 1 0 7.75" />
    </Icon>
  );
}
export function UserRound({ size }: { size?: number }) {
  return (
    <Icon size={size}>
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21v-2a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v2" />
    </Icon>
  );
}

export function Moon({ size }: { size?: number }) {
  return (
    <Icon size={size}>
      <path d="M20.5 13.1A9 9 0 0 1 10.9 3.5a9 9 0 1 0 9.6 9.6Z" />
    </Icon>
  );
}
export function Sun({ size }: { size?: number }) {
  return (
    <Icon size={size}>
      <circle cx="12" cy="12" r="4" />
      <path d="M12 2v2m0 16v2M2 12h2m16 0h2M5 5l1.5 1.5m11 11L19 19M5 19l1.5-1.5m11-11L19 5" />
    </Icon>
  );
}
export function Bell({ size }: { size?: number }) {
  return (
    <Icon size={size}>
      <path d="M18 8a6 6 0 0 0-12 0c0 7-3 7-3 9h18c0-2-3-2-3-9M10 21h4" />
    </Icon>
  );
}

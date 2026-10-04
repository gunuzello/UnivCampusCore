import { useState, type ReactNode } from "react";
export function ErrorMessage({ error }: { error: unknown }) {
  return error ? (
    <p role="alert" className="error">
      {error instanceof Error ? error.message : "요청을 처리하지 못했습니다."}
    </p>
  ) : null;
}
export function Empty({ children }: { children: ReactNode }) {
  return <div className="empty">{children}</div>;
}
export function Panel({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="card">
      <h2>{title}</h2>
      {children}
    </section>
  );
}
export function Field({ label, children }: { label: string; children: ReactNode }) {
  return (
    <label className="field">
      <span>{label}</span>
      {children}
    </label>
  );
}
export function Action({
  label,
  onAction,
  danger = false,
}: {
  label: string;
  onAction: () => Promise<unknown>;
  danger?: boolean;
}) {
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>();
  return (
    <span className="action">
      <button
        className={danger ? "danger" : "secondary"}
        disabled={busy}
        onClick={async () => {
          setBusy(true);
          setError(undefined);
          try {
            await onAction();
          } catch (e) {
            setError(e);
          } finally {
            setBusy(false);
          }
        }}
      >
        {busy ? "처리 중…" : label}
      </button>
      <ErrorMessage error={error} />
    </span>
  );
}
export function Status({ value }: { value: string }) {
  const labels: Record<string, string> = {
    REQUESTED: "수령 대기",
    BORROWED: "대여 중",
    RETURNED: "반납 완료",
    DRAFT: "작성 중",
    PUBLISHED: "공개",
    CLOSED: "마감",
    COMPLETED: "완료",
    CANCELLED: "취소",
    SUBMITTED: "접수",
    REVIEWING: "검토 중",
    ACCEPTED: "합격",
    REJECTED: "불합격",
    REGISTERED: "신청 완료",
    ATTENDED: "참가 완료",
    ABSENT: "불참",
    MEMBER: "구성원",
    STAFF: "운영진",
    LEADER: "대표",
  };
  return <span className={"badge " + value.toLowerCase()}>{labels[value] || value}</span>;
}
export function localInput(value: string) {
  return value.slice(0, 16);
}
export function instant(value: string) {
  return new Date(value).toISOString();
}
export function date(value: string) {
  return new Date(value).toLocaleString("ko-KR", { dateStyle: "medium", timeStyle: "short" });
}

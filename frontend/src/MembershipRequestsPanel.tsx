import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Organization, type Profile } from "./api";
import { Panel, Field, Empty, ErrorMessage, Action, date } from "./ui";
type JoinRequest = {
  id: number;
  user: Profile;
  message: string;
  status: string;
  createdAt: string;
  resolvedAt: string | null;
};
const labels: Record<string, string> = {
  PENDING: "검토 중",
  ACCEPTED: "가입 승인",
  REJECTED: "반려",
  CANCELLED: "취소",
};
export default function MembershipRequestsPanel({ org }: { org: Organization }) {
  const query = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const path = "/organizations/" + org.id + "/membership-requests";
  const mine = useQuery({
    queryKey: ["membership-requests", org.id, "mine"],
    queryFn: () => request<JoinRequest | null>(path + "/me"),
    enabled: !org.role,
  });
  const list = useQuery({
    queryKey: ["membership-requests", org.id, "list"],
    queryFn: () => request<JoinRequest[]>(path),
    enabled: org.role === "LEADER",
  });
  async function refresh() {
    await query.invalidateQueries({ queryKey: ["membership-requests"] });
    await query.invalidateQueries({ queryKey: ["members"] });
    await query.invalidateQueries({ queryKey: ["organizations"] });
    await query.invalidateQueries({ queryKey: ["notifications"] });
  }
  if (org.role && org.role !== "LEADER") return null;
  return (
    <Panel title={org.role === "LEADER" ? "가입 신청 관리" : "소속 가입 신청"}>
      {org.role === "LEADER" ? (
        <>
          <ErrorMessage error={list.error} />
          {list.data?.length
            ? list.data.map((r) => (
                <div key={r.id} className="applicant">
                  <strong>{r.user.name}</strong>
                  <p className="meta">
                    {r.user.email} · {r.user.department} · {r.user.studentNumber}
                  </p>
                  <span className="badge">{labels[r.status]}</span>
                  <p className="prewrap">{r.message}</p>
                  <p className="meta">신청 · {date(r.createdAt)}</p>
                  {r.status === "PENDING" && (
                    <div className="toolbar">
                      {[
                        ["ACCEPTED", "가입 승인"],
                        ["REJECTED", "신청 반려"],
                      ].map(([status, label]) => (
                        <Action
                          key={status}
                          label={label}
                          onAction={async () => {
                            await request(path + "/" + r.id + "/status", "PATCH", { status });
                            await refresh();
                          }}
                        />
                      ))}
                    </div>
                  )}
                </div>
              ))
            : !list.isPending && !list.error && <Empty>접수한 가입 신청이 없어요.</Empty>}
        </>
      ) : (
        <>
          <ErrorMessage error={mine.error} />
          {mine.isPending ? (
            <p>가입 상태를 확인하고 있어요…</p>
          ) : (
            <>
              {mine.data && (
                <>
                  <span className="badge">{labels[mine.data.status]}</span>
                  <p className="prewrap">{mine.data.message}</p>
                </>
              )}
              {mine.data?.status === "PENDING" ? (
                <Action
                  label="가입 신청 취소"
                  onAction={async () => {
                    await request(path + "/" + mine.data!.id + "/cancellation", "POST");
                    await refresh();
                  }}
                />
              ) : (
                !mine.error && (
                  <form
                    onSubmit={async (e) => {
                      e.preventDefault();
                      setError(undefined);
                      setBusy(true);
                      const f = new FormData(e.currentTarget);
                      try {
                        await request(path, "POST", { message: f.get("message") });
                        await refresh();
                      } catch (e) {
                        setError(e);
                      } finally {
                        setBusy(false);
                      }
                    }}
                  >
                    <Field label="가입 신청 메시지">
                      <textarea
                        name="message"
                        maxLength={2000}
                        placeholder="소개와 가입 이유를 간단히 남겨 주세요."
                      />
                    </Field>
                    <p className="meta">
                      대표가 승인하면 구성원으로 가입해요. 운영진 권한은 대표가 별도로 지정해요.
                    </p>
                    <ErrorMessage error={error} />
                    <button className="primary" disabled={busy}>
                      {busy ? "접수 중…" : "가입 신청"}
                    </button>
                  </form>
                )
              )}
            </>
          )}
        </>
      )}
    </Panel>
  );
}

import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Organization, type Profile } from "./api";
import { Panel, Field, ErrorMessage, Empty, Action, date, instant } from "./ui";
type Work = {
  id: number;
  kind: string;
  title: string;
  content: string;
  requestedRole: string;
  status: string;
  response: string;
  creatorName: string;
  assigneeId: number | null;
  assigneeName: string;
  dueAt: string | null;
  canManage: boolean;
  canComplete: boolean;
  canCancel: boolean;
};
const kinds: Record<string, string> = {
  SUGGESTION: "건의하기",
  ROLE: "운영진 권한 신청",
  TASK: "운영 업무",
};
const statuses: Record<string, string> = {
  PENDING: "대기 중",
  DONE: "완료",
  ANSWERED: "답변 완료",
  ACCEPTED: "승인",
  REJECTED: "반려",
  CANCELLED: "취소",
};
export default function OrganizationWorkPanel({ org }: { org: Organization }) {
  const client = useQueryClient();
  const [kind, setKind] = useState("SUGGESTION");
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Work>();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const manage = !!org.role && org.role !== "MEMBER";
  const path = "/organizations/" + org.id + "/work";
  const q = useQuery({
    queryKey: ["organization-work", org.id],
    queryFn: () => request<Work[]>(path),
  });
  const members = useQuery({
    queryKey: ["members", org.id],
    queryFn: () => request<{ user: Profile }[]>("/organizations/" + org.id + "/members"),
    enabled: manage,
  });
  const refresh = async () => {
    await client.invalidateQueries({ queryKey: ["organization-work"] });
    await client.invalidateQueries({ queryKey: ["organizations"] });
    await client.invalidateQueries({ queryKey: ["members"] });
    await client.invalidateQueries({ queryKey: ["notifications"] });
    await client.invalidateQueries({ queryKey: ["calendar"] });
  };
  const visible = q.data?.filter((w) => w.kind === kind);
  return (
    <Panel title="건의와 소속 운영">
      <div className="segmented">
        {Object.entries(kinds)
          .filter(([k]) => k === "SUGGESTION" || !!org.role)
          .map(([k, label]) => (
            <button
              key={k}
              className={kind === k ? "primary" : "secondary"}
              onClick={() => {
                setKind(k);
                setOpen(false);
                setEditing(undefined);
                setError(undefined);
              }}
            >
              {label}
            </button>
          ))}
      </div>
      <p className="muted">건의는 작성자와 운영진에게만 공개됩니다. 권한 신청은 대표가 검토해요.</p>
      <ErrorMessage error={q.error} />
      {(kind === "SUGGESTION" ||
        (kind === "ROLE" && org.role !== "LEADER") ||
        (kind === "TASK" && manage)) && (
        <button
          className="secondary"
          onClick={() => {
            setEditing(undefined);
            setOpen(!open);
          }}
        >
          새 {kinds[kind]}
        </button>
      )}
      {open && (
        <form
          key={editing?.id || kind}
          onSubmit={async (e) => {
            e.preventDefault();
            setBusy(true);
            setError(undefined);
            const f = new FormData(e.currentTarget);
            try {
              await request(editing ? path + "/" + editing.id : path, editing ? "PATCH" : "POST", {
                ...Object.fromEntries(f),
                kind,
                assigneeId: f.get("assigneeId") ? Number(f.get("assigneeId")) : null,
                dueAt: f.get("dueAt") ? instant(String(f.get("dueAt"))) : null,
              });
              setOpen(false);
              await refresh();
            } catch (e) {
              setError(e);
            } finally {
              setBusy(false);
            }
          }}
        >
          <Field label="요청 또는 업무 제목">
            <input name="title" required maxLength={200} defaultValue={editing?.title} />
          </Field>
          <Field label="요청 또는 업무 내용">
            <textarea name="content" required maxLength={10000} defaultValue={editing?.content} />
          </Field>
          {kind === "ROLE" && (
            <Field label="신청 역할">
              <select name="requestedRole">
                <option value="STAFF">운영진</option>
                <option value="LEADER">대표</option>
              </select>
            </Field>
          )}
          {kind === "TASK" && (
            <>
              <Field label="업무 담당자">
                <select name="assigneeId" defaultValue={editing?.assigneeId || ""}>
                  <option value="">미지정</option>
                  {members.data?.map((m) => (
                    <option key={m.user.id} value={m.user.id}>
                      {m.user.name}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="업무 마감">
                <input
                  type="datetime-local"
                  name="dueAt"
                  defaultValue={
                    editing?.dueAt
                      ? (() => {
                          const d = new Date(editing.dueAt!);
                          return new Date(d.getTime() - d.getTimezoneOffset() * 60000)
                            .toISOString()
                            .slice(0, 16);
                        })()
                      : undefined
                  }
                />
              </Field>
            </>
          )}
          <ErrorMessage error={error} />
          <button className="primary" disabled={busy}>
            요청·업무 저장
          </button>
        </form>
      )}
      {!q.isPending && !q.error && !visible?.length && <Empty>아직 등록된 항목이 없어요.</Empty>}
      {visible?.map((w) => (
        <details className="applicant" key={w.id}>
          <summary>
            <strong>{w.title}</strong>
            <span className="badge">{statuses[w.status]}</span>
          </summary>
          <p className="meta">
            작성 · {w.creatorName}
            {w.requestedRole &&
              " · 신청 역할: " + (w.requestedRole === "LEADER" ? "대표" : "운영진")}
          </p>
          <p className="prewrap">{w.content}</p>
          {w.assigneeName && <p>담당 · {w.assigneeName}</p>}
          {w.dueAt && (
            <p>
              마감 · {date(w.dueAt)}
              {w.status !== "DONE" && Date.parse(w.dueAt) < Date.now() ? " · 지연" : ""}
            </p>
          )}
          {w.response && <p className="prewrap">답변 · {w.response}</p>}
          {w.canCancel && (
            <Action
              label="요청 취소"
              onAction={async () => {
                await request(path + "/" + w.id + "/status", "PATCH", { status: "CANCELLED" });
                await refresh();
              }}
            />
          )}
          {w.kind === "ROLE" && w.canManage && w.status === "PENDING" && (
            <div className="toolbar">
              {["ACCEPTED", "REJECTED"].map((status) => (
                <Action
                  key={status}
                  label={statuses[status]}
                  onAction={async () => {
                    await request(path + "/" + w.id + "/status", "PATCH", { status });
                    await refresh();
                  }}
                />
              ))}
            </div>
          )}
          {w.kind === "SUGGESTION" && w.canManage && w.status !== "CANCELLED" && (
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setError(undefined);
                try {
                  await request(path + "/" + w.id + "/status", "PATCH", {
                    status: "ANSWERED",
                    response: new FormData(e.currentTarget).get("response"),
                  });
                  await refresh();
                } catch (e) {
                  setError(e);
                }
              }}
            >
              <Field label="건의 답변">
                <textarea name="response" required maxLength={10000} defaultValue={w.response} />
              </Field>
              <button className="primary">답변 저장</button>
              <ErrorMessage error={error} />
            </form>
          )}
          {w.kind === "TASK" && (
            <div className="toolbar">
              {w.canManage && (
                <button
                  className="secondary"
                  onClick={() => {
                    setKind("TASK");
                    setEditing(w);
                    setOpen(true);
                  }}
                >
                  업무 수정
                </button>
              )}
              {w.canComplete && (
                <Action
                  label={w.status === "DONE" ? "업무 다시 열기" : "업무 완료"}
                  onAction={async () => {
                    await request(path + "/" + w.id + "/status", "PATCH", {
                      status: w.status === "DONE" ? "PENDING" : "DONE",
                    });
                    await refresh();
                  }}
                />
              )}
            </div>
          )}
        </details>
      ))}
    </Panel>
  );
}

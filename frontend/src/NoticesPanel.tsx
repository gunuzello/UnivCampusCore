import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Organization } from "./api";
import { Panel, Field, Empty, ErrorMessage, date } from "./ui";
type Notice = {
  id: number;
  title: string;
  content: string;
  visibility: "PUBLIC" | "MEMBERS";
  updatedAt: string;
};
export default function NoticesPanel({ org }: { org: Organization }) {
  const query = useQueryClient();
  const [open, setOpen] = useState(false);
  const [editing, setEditing] = useState<Notice>();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const manage = org.role === "STAFF" || org.role === "LEADER";
  const path = "/organizations/" + org.id + "/notices";
  const q = useQuery({ queryKey: ["notices", org.id], queryFn: () => request<Notice[]>(path) });
  return (
    <Panel title="소속 소식">
      <ErrorMessage error={q.error} />
      {manage && (
        <div className="toolbar">
          <button
            className="secondary"
            onClick={() => {
              setEditing(undefined);
              setOpen(!open);
              setError(undefined);
            }}
          >
            소식 작성
          </button>
        </div>
      )}
      {open && manage && (
        <form
          key={editing?.id || "new"}
          onSubmit={async (e) => {
            e.preventDefault();
            setError(undefined);
            setBusy(true);
            const f = new FormData(e.currentTarget);
            try {
              await request(
                editing ? path + "/" + editing.id : path,
                editing ? "PATCH" : "POST",
                Object.fromEntries(f),
              );
              setOpen(false);
              setEditing(undefined);
              await query.invalidateQueries({ queryKey: ["notices"] });
              await query.invalidateQueries({ queryKey: ["notifications"] });
            } catch (e) {
              setError(e);
            } finally {
              setBusy(false);
            }
          }}
        >
          <Field label="소식 제목">
            <input name="title" required maxLength={200} defaultValue={editing?.title} />
          </Field>
          <Field label="소식 내용">
            <textarea name="content" required maxLength={30000} defaultValue={editing?.content} />
          </Field>
          <Field label="소식 공개 범위">
            <select name="visibility" defaultValue={editing?.visibility || "MEMBERS"}>
              <option value="MEMBERS">소속 구성원</option>
              <option value="PUBLIC">모든 로그인 사용자</option>
            </select>
          </Field>
          <ErrorMessage error={error} />
          <div className="toolbar">
            <button className="primary" disabled={busy}>
              {busy ? "저장 중…" : "소식 저장"}
            </button>
            <button
              type="button"
              className="secondary"
              onClick={() => {
                setOpen(false);
                setEditing(undefined);
              }}
            >
              닫기
            </button>
          </div>
        </form>
      )}
      {q.isPending ? (
        <p>소식을 불러오고 있어요…</p>
      ) : q.data?.length ? (
        q.data.map((n) => (
          <details key={n.id} className="applicant">
            <summary>
              <strong>{n.title}</strong>
              <span className="badge">{n.visibility === "PUBLIC" ? "전체 공개" : "구성원"}</span>
            </summary>
            <p className="meta">최근 수정 · {date(n.updatedAt)}</p>
            <p className="prewrap">{n.content}</p>
            {manage && (
              <button
                className="secondary"
                onClick={() => {
                  setEditing(n);
                  setOpen(true);
                  setError(undefined);
                }}
              >
                소식 수정
              </button>
            )}
          </details>
        ))
      ) : (
        !q.error && <Empty>공개된 소식이 없어요.</Empty>
      )}
    </Panel>
  );
}

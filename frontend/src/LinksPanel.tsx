import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request } from "./api";
import { Panel, Field, ErrorMessage, Action, Empty } from "./ui";
type LinkItem = { id: number; title: string; url: string; description: string };
export default function LinksPanel({
  orgId,
  type,
  targetId,
  manage,
}: {
  orgId: number;
  type: string;
  targetId: number;
  manage: boolean;
}) {
  const query = useQueryClient();
  const [editing, setEditing] = useState<LinkItem>();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const path = "/organizations/" + orgId + "/links?type=" + type + "&targetId=" + targetId;
  const q = useQuery({
    queryKey: ["links", orgId, type, targetId],
    queryFn: () => request<LinkItem[]>(path),
  });
  return (
    <Panel title="관련 자료와 링크">
      <ErrorMessage error={q.error} />
      {!q.data?.length && <Empty>기획서, 디자인, 결과 보고서 링크를 연결해요.</Empty>}
      {q.data?.map((l) => (
        <div key={l.id} className="list-row">
          <div>
            <a href={l.url} target="_blank" rel="noopener noreferrer" className="external-link">
              {l.title} ↗
            </a>
            <p className="muted">{l.description}</p>
          </div>
          {manage && (
            <div className="toolbar">
              <button className="secondary" onClick={() => setEditing(l)}>
                수정
              </button>
              <Action
                label="삭제"
                danger
                onAction={async () => {
                  if (!confirm("자료 링크를 삭제할까요?")) return;
                  await request("/links/" + l.id, "DELETE");
                  await query.invalidateQueries({ queryKey: ["links"] });
                }}
              />
            </div>
          )}
        </div>
      ))}
      {manage && (
        <form
          key={editing?.id || "new"}
          onSubmit={async (e) => {
            e.preventDefault();
            const form = e.currentTarget;
            setError(undefined);
            setBusy(true);
            try {
              await request(
                editing ? "/links/" + editing.id : path,
                editing ? "PATCH" : "POST",
                Object.fromEntries(new FormData(form)),
              );
              setEditing(undefined);
              form.reset();
              await query.invalidateQueries({ queryKey: ["links"] });
            } catch (e) {
              setError(e);
            } finally {
              setBusy(false);
            }
          }}
        >
          <Field label="자료 제목">
            <input name="title" required maxLength={200} defaultValue={editing?.title} />
          </Field>
          <Field label="외부 주소">
            <input
              name="url"
              type="url"
              required
              maxLength={2000}
              placeholder="https://…"
              defaultValue={editing?.url}
            />
          </Field>
          <Field label="간단한 설명">
            <textarea name="description" maxLength={5000} defaultValue={editing?.description} />
          </Field>
          <ErrorMessage error={error} />
          <button className="secondary" disabled={busy}>
            {editing ? "변경 저장" : "링크 추가"}
          </button>
          {editing && (
            <button type="button" className="text-button" onClick={() => setEditing(undefined)}>
              취소
            </button>
          )}
        </form>
      )}
    </Panel>
  );
}

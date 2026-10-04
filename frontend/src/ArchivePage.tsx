import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request, type Organization } from "./api";
import { Panel, Field, Empty, ErrorMessage, Status, date } from "./ui";
import LinksPanel from "./LinksPanel";
type RecordItem = {
  key: string;
  type: string;
  id: number;
  title: string;
  date: string;
  path: string;
  status: string;
};
type Note = { id: number; title: string; period: string; content: string; updatedAt: string };
export default function ArchivePage({ org }: { org?: Organization }) {
  const query = useQueryClient();
  const [year, setYear] = useState("");
  const [editing, setEditing] = useState<Note>();
  const [open, setOpen] = useState(false);
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const records = useQuery({
    queryKey: ["archive", org?.id, year],
    queryFn: () =>
      request<RecordItem[]>(
        "/organizations/" + org!.id + "/archive" + (year ? "?year=" + year : ""),
      ),
    enabled: !!org?.role,
  });
  const notes = useQuery({
    queryKey: ["notes", org?.id],
    queryFn: () => request<Note[]>("/organizations/" + org!.id + "/handover-notes"),
    enabled: !!org?.role,
  });
  if (!org?.role) return <Empty>활동 기록은 학생회 구성원에게 공개됩니다.</Empty>;
  const manage = org.role !== "MEMBER";
  return (
    <>
      <div className="page-heading">
        <h1>지난 활동</h1>
        <p>사람은 바뀌어도, 우리 학생회의 기록은 남아요.</p>
      </div>
      <div className="toolbar">
        <select aria-label="활동 연도" value={year} onChange={(e) => setYear(e.target.value)}>
          <option value="">전체 기간</option>
          {Array.from({ length: 12 }, (_, i) => new Date().getFullYear() + 1 - i).map((y) => (
            <option key={y} value={y}>
              {y}년
            </option>
          ))}
        </select>
        {manage && (
          <button
            className="primary"
            onClick={() => {
              setOpen(!open);
              setEditing(undefined);
            }}
          >
            인수인계 메모 추가
          </button>
        )}
      </div>
      <ErrorMessage error={records.error} />
      <Panel title="활동 기록">
        {!records.data?.length && (
          <Empty>완료한 행사, 종료한 모집, 지난 회의와 내부 일정이 여기에 남아요.</Empty>
        )}
        {records.data?.map((a) => (
          <Link className="list-row" key={a.key} to={a.path}>
            <div>
              <strong>{a.title}</strong>
              <p className="meta">
                {{ EVENT: "행사", RECRUITMENT: "모집", MEETING: "회의", SCHEDULE: "일정" }[
                  a.type
                ] || a.type}{" "}
                · {date(a.date)}
              </p>
            </div>
            <Status value={a.status} />
          </Link>
        ))}
      </Panel>
      {open && (
        <Panel title={editing ? "인수인계 메모 수정" : "인수인계 메모"}>
          <form
            key={editing?.id || "new"}
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError(undefined);
              try {
                await request(
                  editing
                    ? "/handover-notes/" + editing.id
                    : "/organizations/" + org.id + "/handover-notes",
                  editing ? "PATCH" : "POST",
                  Object.fromEntries(new FormData(e.currentTarget)),
                );
                setOpen(false);
                setEditing(undefined);
                await query.invalidateQueries({ queryKey: ["notes"] });
              } catch (e) {
                setError(e);
              } finally {
                setBusy(false);
              }
            }}
          >
            <Field label="제목">
              <input name="title" required maxLength={200} defaultValue={editing?.title} />
            </Field>
            <Field label="기수 또는 기간">
              <input
                name="period"
                required
                maxLength={80}
                placeholder="2026년 · 제16대"
                defaultValue={editing?.period}
              />
            </Field>
            <Field label="다음 운영진에게 남길 내용">
              <textarea name="content" required maxLength={30000} defaultValue={editing?.content} />
            </Field>
            <ErrorMessage error={error} />
            <button className="primary" disabled={busy}>
              메모 저장
            </button>
            <button type="button" className="text-button" onClick={() => setOpen(false)}>
              닫기
            </button>
          </form>
        </Panel>
      )}
      <div className="section-title">
        <h2>인수인계</h2>
      </div>
      <ErrorMessage error={notes.error} />
      {!notes.data?.length && <Empty>아직 남긴 인수인계 메모가 없어요.</Empty>}
      {notes.data?.map((n) => (
        <div className="columns" key={n.id}>
          <Panel title={n.title}>
            <span className="badge">{n.period}</span>
            <p className="prewrap">{n.content}</p>
            <p className="meta">최근 수정 · {date(n.updatedAt)}</p>
            {manage && (
              <button
                className="secondary"
                onClick={() => {
                  setEditing(n);
                  setOpen(true);
                  window.scrollTo({ top: 0, behavior: "smooth" });
                }}
              >
                메모 수정
              </button>
            )}
          </Panel>
          <LinksPanel orgId={org.id} type="NOTE" targetId={n.id} manage={manage} />
        </div>
      ))}
    </>
  );
}

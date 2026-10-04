import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { request, type Organization, type Profile } from "./api";
import { Panel, Field, Empty, ErrorMessage, date, instant } from "./ui";
import LinksPanel from "./LinksPanel";
type Meeting = {
  id: number;
  organizationId: number;
  title: string;
  startsAt: string;
  endsAt: string;
  content: string;
  attendees: number[];
  agendas: string[];
  decisions: string[];
  canManage: boolean;
};
const local = (v: string) => {
  const d = new Date(v);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
function MeetingForm({
  orgId,
  initial,
  onDone,
}: {
  orgId: number;
  initial?: Meeting;
  onDone: (m: Meeting) => void;
}) {
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const members = useQuery({
    queryKey: ["members", orgId],
    queryFn: () =>
      request<{ id: number; user: Profile; role: string }[]>(
        "/organizations/" + orgId + "/members",
      ),
  });
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setError(undefined);
        const f = new FormData(e.currentTarget);
        const lines = (key: string) =>
          String(f.get(key) || "")
            .split("\n")
            .map((s) => s.trim())
            .filter(Boolean);
        try {
          const m = await request<Meeting>(
            initial ? "/meetings/" + initial.id : "/organizations/" + orgId + "/meetings",
            initial ? "PATCH" : "POST",
            {
              title: f.get("title"),
              content: f.get("content"),
              startsAt: instant(String(f.get("startsAt"))),
              endsAt: instant(String(f.get("endsAt"))),
              attendees: f.getAll("attendees").map(Number),
              agendas: lines("agendas"),
              decisions: lines("decisions"),
            },
          );
          onDone(m);
        } catch (e) {
          setError(e);
        } finally {
          setBusy(false);
        }
      }}
    >
      <Field label="회의 제목">
        <input name="title" required maxLength={200} defaultValue={initial?.title} />
      </Field>
      <div className="columns">
        <Field label="시작">
          <input
            type="datetime-local"
            name="startsAt"
            required
            defaultValue={initial ? local(initial.startsAt) : local(new Date().toISOString())}
          />
        </Field>
        <Field label="종료">
          <input
            type="datetime-local"
            name="endsAt"
            required
            defaultValue={
              initial ? local(initial.endsAt) : local(new Date(Date.now() + 3600000).toISOString())
            }
          />
        </Field>
      </div>
      <h3>참석 대상</h3>
      <ErrorMessage error={members.error} />
      <div className="checks">
        {members.data?.map((m) => (
          <label key={m.id}>
            <input
              type="checkbox"
              name="attendees"
              value={m.user.id}
              defaultChecked={initial?.attendees.includes(m.user.id)}
            />
            {m.user.name}
          </label>
        ))}
      </div>
      <Field label="안건 · 한 줄에 하나씩">
        <textarea name="agendas" defaultValue={initial?.agendas.join("\n")} maxLength={30000} />
      </Field>
      <Field label="회의 내용">
        <textarea name="content" defaultValue={initial?.content} maxLength={30000} />
      </Field>
      <Field label="결정사항 · 한 줄에 하나씩">
        <textarea name="decisions" defaultValue={initial?.decisions.join("\n")} maxLength={30000} />
      </Field>
      <ErrorMessage error={error} />
      <button className="primary" disabled={busy}>
        회의 기록 저장
      </button>
    </form>
  );
}
export function MeetingList({ org }: { org?: Organization }) {
  const [create, setCreate] = useState(false);
  const nav = useNavigate();
  const query = useQueryClient();
  const q = useQuery({
    queryKey: ["meetings", org?.id],
    queryFn: () => request<Meeting[]>("/organizations/" + org!.id + "/meetings"),
    enabled: !!org?.role,
  });
  if (!org?.role) return <Empty>회의는 조직 구성원에게 공개됩니다.</Empty>;
  return (
    <>
      <div className="page-heading">
        <h1>정기회의</h1>
        <p>안건을 나누고, 결정한 내용을 함께 남겨요.</p>
      </div>
      {org.role !== "MEMBER" && (
        <div className="toolbar">
          <button className="primary" onClick={() => setCreate(!create)}>
            {create ? "닫기" : "회의 만들기"}
          </button>
        </div>
      )}
      {create && (
        <Panel title="새 회의">
          <MeetingForm
            orgId={org.id}
            onDone={async (m) => {
              await query.invalidateQueries({ queryKey: ["meetings"] });
              await query.invalidateQueries({ queryKey: ["calendar"] });
              await query.invalidateQueries({ queryKey: ["archive"] });
              nav("/meetings/" + m.id);
            }}
          />
        </Panel>
      )}
      <ErrorMessage error={q.error} />
      {!q.data?.length && <Empty>아직 회의 기록이 없어요.</Empty>}
      <div className="grid">
        {q.data?.map((m) => (
          <Link key={m.id} className="card card-link" to={"/meetings/" + m.id}>
            <span className="badge">
              {Date.parse(m.endsAt) < Date.now() ? "지난 회의" : "예정된 회의"}
            </span>
            <h2 style={{ marginTop: 16 }}>{m.title}</h2>
            <p className="meta">{date(m.startsAt)}</p>
            <p>
              {m.agendas.length}개 안건 · {m.decisions.length}개 결정사항
            </p>
          </Link>
        ))}
      </div>
    </>
  );
}
export function MeetingDetail() {
  const { id } = useParams();
  const query = useQueryClient();
  const [editing, setEditing] = useState(false);
  const q = useQuery({
    queryKey: ["meetings", "detail", id],
    queryFn: () => request<Meeting>("/meetings/" + id),
  });
  const members = useQuery({
    queryKey: ["members", q.data?.organizationId],
    queryFn: () =>
      request<{ user: Profile }[]>("/organizations/" + q.data!.organizationId + "/members"),
    enabled: !!q.data,
  });
  if (q.isPending) return <Empty>불러오는 중…</Empty>;
  if (q.error) return <ErrorMessage error={q.error} />;
  const m = q.data!;
  return (
    <>
      <div className="toolbar">
        <Link to="/meetings">← 회의 목록</Link>
        {m.canManage && (
          <button className="secondary" onClick={() => setEditing(!editing)}>
            {editing ? "닫기" : "회의 수정"}
          </button>
        )}
      </div>
      <div className="page-heading">
        <h1>{m.title}</h1>
        <p>
          {date(m.startsAt)} — {date(m.endsAt)}
        </p>
      </div>
      {editing ? (
        <Panel title="회의 기록 수정">
          <MeetingForm
            orgId={m.organizationId}
            initial={m}
            onDone={async () => {
              setEditing(false);
              await query.invalidateQueries({ queryKey: ["meetings"] });
              await query.invalidateQueries({ queryKey: ["calendar"] });
              await query.invalidateQueries({ queryKey: ["archive"] });
              await query.invalidateQueries({ queryKey: ["calendar"] });
            }}
          />
        </Panel>
      ) : (
        <div className="columns">
          <div>
            <Panel title="안건">
              {m.agendas.length ? (
                <ol>
                  {m.agendas.map((s, i) => (
                    <li className="prewrap" key={i}>
                      {s}
                    </li>
                  ))}
                </ol>
              ) : (
                <Empty>안건이 없어요.</Empty>
              )}
            </Panel>
            <Panel title="회의 내용">
              <p className="prewrap">{m.content || "기록한 내용이 없어요."}</p>
            </Panel>
            <Panel title="결정사항">
              {m.decisions.length ? (
                <ol>
                  {m.decisions.map((s, i) => (
                    <li className="prewrap" key={i}>
                      {s}
                    </li>
                  ))}
                </ol>
              ) : (
                <Empty>결정사항이 없어요.</Empty>
              )}
            </Panel>
          </div>
          <div>
            <Panel title="참석 대상">
              <ErrorMessage error={members.error} />
              {m.attendees.map((u) => (
                <p key={u}>
                  {members.data?.find((x) => x.user.id === u)?.user.name || "이전 구성원 #" + u}
                </p>
              ))}
            </Panel>
            <LinksPanel
              orgId={m.organizationId}
              type="MEETING"
              targetId={m.id}
              manage={m.canManage}
            />
          </div>
        </div>
      )}
    </>
  );
}

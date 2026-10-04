import { SaveButton } from "./PersonalPanel";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { request } from "./api";
import { Panel, Field, Empty, ErrorMessage, date, instant, Action } from "./ui";
type Team = {
  id: number;
  ownerId: number;
  title: string;
  content: string;
  roles: string;
  tags: string;
  capacity: number;
  deadline: string;
  status: string;
};
type Application = { userId: number; name?: string; role: string; message: string; status: string };
type View = {
  team: Team;
  owner: boolean;
  member: boolean;
  memberCount: number;
  mine: Application | null;
};
type Entry = {
  id: number;
  kind: string;
  title: string;
  content: string;
  assigneeId: number | null;
  startsAt: string | null;
  endsAt: string | null;
  url: string;
  done: boolean;
};
type Member = { id: number; name: string; role: string };
const labels: Record<string, string> = {
  OPEN: "모집 중",
  CLOSED: "모집 종료",
  COMPLETED: "활동 완료",
  PENDING: "검토 중",
  ACCEPTED: "수락",
  REJECTED: "반려",
  CANCELLED: "취소",
  MEETING: "모임",
  DEADLINE: "마감",
  TASK: "할 일",
  STAGE: "진행 단계",
  LINK: "자료",
};
const local = (v: string) => {
  const d = new Date(v);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
function TeamForm({ initial, onDone }: { initial?: Team; onDone: (v: View) => void }) {
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setError(undefined);
        const f = new FormData(e.currentTarget);
        try {
          onDone(
            await request<View>(
              initial ? "/teams/" + initial.id : "/teams",
              initial ? "PATCH" : "POST",
              {
                ...Object.fromEntries(f),
                capacity: Number(f.get("capacity")),
                deadline: instant(String(f.get("deadline"))),
              },
            ),
          );
        } catch (e) {
          setError(e);
        } finally {
          setBusy(false);
        }
      }}
    >
      <Field label="팀 모집 제목">
        <input name="title" required maxLength={200} defaultValue={initial?.title} />
      </Field>
      <Field label="팀 소개와 지원 안내">
        <textarea name="content" required maxLength={30000} defaultValue={initial?.content} />
      </Field>
      <Field label="필요 역할">
        <input name="roles" required maxLength={1000} defaultValue={initial?.roles} />
      </Field>
      <Field label="관심 태그">
        <input name="tags" maxLength={500} defaultValue={initial?.tags} />
      </Field>
      <Field label="팀장 포함 정원">
        <input
          type="number"
          name="capacity"
          min={2}
          max={100}
          required
          defaultValue={initial?.capacity || 4}
        />
      </Field>
      <Field label="팀 지원 마감">
        <input
          type="datetime-local"
          name="deadline"
          required
          defaultValue={initial ? local(initial.deadline) : undefined}
        />
      </Field>
      <ErrorMessage error={error} />
      <button className="primary" disabled={busy}>
        팀 모집 저장
      </button>
    </form>
  );
}
export function TeamList() {
  const [search, setSearch] = useState("");
  const [mine, setMine] = useState(false);
  const [create, setCreate] = useState(false);
  const nav = useNavigate();
  const q = useQuery({
    queryKey: ["teams", search, mine],
    queryFn: () => request<View[]>("/teams?" + new URLSearchParams({ search, mine: String(mine) })),
  });
  return (
    <>
      <div className="toolbar">
        <Field label="팀 검색">
          <input value={search} onChange={(e) => setSearch(e.target.value)} />
        </Field>
        <label>
          <input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} />내
          팀만
        </label>
        <button className="primary" onClick={() => setCreate(!create)}>
          {create ? "닫기" : "팀 모으기"}
        </button>
      </div>
      {create && (
        <Panel title="팀 모으기">
          <TeamForm onDone={(v) => nav("/teams/" + v.team.id)} />
        </Panel>
      )}
      <ErrorMessage error={q.error} />
      {q.isPending ? (
        <Empty>불러오는 중…</Empty>
      ) : (
        !q.data?.length && !q.error && <Empty>등록된 팀이 없어요.</Empty>
      )}
      <div className="grid">
        {q.data?.map((v) => (
          <Link key={v.team.id} className="card card-link" to={"/teams/" + v.team.id}>
            <span className="badge">
              {v.team.status === "OPEN" && Date.parse(v.team.deadline) <= Date.now()
                ? "기간 마감"
                : labels[v.team.status]}
            </span>
            <h2>{v.team.title}</h2>
            <p>{v.team.roles}</p>
            <p className="meta">
              {v.memberCount}/{v.team.capacity}명 · {date(v.team.deadline)}
            </p>
          </Link>
        ))}
      </div>
    </>
  );
}
function EntryForm({
  id,
  members,
  initial,
  onDone,
}: {
  id: number;
  members: Member[];
  initial?: Entry;
  onDone: () => void;
}) {
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        setBusy(true);
        setError(undefined);
        const f = new FormData(e.currentTarget);
        try {
          await request(
            "/teams/" + id + "/entries" + (initial ? "/" + initial.id : ""),
            initial ? "PATCH" : "POST",
            {
              ...Object.fromEntries(f),
              assigneeId: f.get("assigneeId") ? Number(f.get("assigneeId")) : null,
              startsAt: f.get("startsAt") ? instant(String(f.get("startsAt"))) : null,
              endsAt: f.get("endsAt") ? instant(String(f.get("endsAt"))) : null,
              done: f.get("done") === "on",
            },
          );
          onDone();
        } catch (e) {
          setError(e);
        } finally {
          setBusy(false);
        }
      }}
    >
      <Field label="팀 항목 유형">
        <select name="kind" defaultValue={initial?.kind || "TASK"}>
          {["TASK", "STAGE", "MEETING", "DEADLINE", "LINK"].map((k) => (
            <option key={k} value={k}>
              {labels[k]}
            </option>
          ))}
        </select>
      </Field>
      <Field label="항목 제목">
        <input name="title" required maxLength={200} defaultValue={initial?.title} />
      </Field>
      <Field label="항목 내용">
        <textarea name="content" maxLength={10000} defaultValue={initial?.content} />
      </Field>
      <Field label="담당 구성원">
        <select name="assigneeId" defaultValue={initial?.assigneeId || ""}>
          <option value="">미지정</option>
          {members.map((m) => (
            <option key={m.id} value={m.id}>
              {m.name}
            </option>
          ))}
        </select>
      </Field>
      <Field label="모임 시작">
        <input
          name="startsAt"
          type="datetime-local"
          defaultValue={initial?.startsAt ? local(initial.startsAt) : undefined}
        />
      </Field>
      <Field label="모임 종료 또는 마감">
        <input
          name="endsAt"
          type="datetime-local"
          defaultValue={initial?.endsAt ? local(initial.endsAt) : undefined}
        />
      </Field>
      <Field label="팀 자료 주소">
        <input name="url" maxLength={2000} defaultValue={initial?.url} />
      </Field>
      <label>
        <input type="checkbox" name="done" defaultChecked={initial?.done} />
        완료
      </label>
      <ErrorMessage error={error} />
      <button className="primary" disabled={busy}>
        팀 항목 저장
      </button>
    </form>
  );
}
export function TeamDetail() {
  const { id } = useParams();
  const client = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [edit, setEdit] = useState(false);
  const [entryOpen, setEntryOpen] = useState(false);
  const [editingEntry, setEditingEntry] = useState<Entry>();
  const q = useQuery({
    queryKey: ["teams", "detail", id],
    queryFn: () => request<View>("/teams/" + id),
  });
  const apps = useQuery({
    queryKey: ["team-applicants", id],
    queryFn: () => request<Application[]>("/teams/" + id + "/applications"),
    enabled: !!q.data?.owner,
  });
  const members = useQuery({
    queryKey: ["team-members", id],
    queryFn: () => request<Member[]>("/teams/" + id + "/members"),
    enabled: !!q.data?.member,
  });
  const entries = useQuery({
    queryKey: ["team-entries", id],
    queryFn: () => request<Entry[]>("/teams/" + id + "/entries"),
    enabled: !!q.data?.member,
  });
  const refresh = async () => {
    await client.invalidateQueries({ predicate: (q) => String(q.queryKey[0]).startsWith("team") });
    await client.invalidateQueries({ queryKey: ["notifications"] });
    await client.invalidateQueries({ queryKey: ["calendar"] });
  };
  if (q.isPending) return <Empty>불러오는 중…</Empty>;
  if (q.error) return <ErrorMessage error={q.error} />;
  const v = q.data!,
    t = v.team;
  return (
    <>
      <Link to="/discover">← 찾기</Link>
      <div className="page-heading">
        <h1>{t.title}</h1>
        <SaveButton type="TEAM" id={t.id} />
        <p>
          {labels[t.status]} · {v.memberCount}/{t.capacity}명 · 마감 {date(t.deadline)}
        </p>
      </div>
      <ErrorMessage error={error} />
      <Panel title="팀 소개">
        <p className="prewrap">{t.content}</p>
        <p>필요 역할 · {t.roles}</p>
        <p>관심 태그 · {t.tags}</p>
        {v.owner && t.status !== "COMPLETED" && (
          <div className="toolbar">
            <button className="secondary" onClick={() => setEdit(!edit)}>
              모집 수정
            </button>
            {[t.status === "OPEN" ? "CLOSED" : "OPEN", "COMPLETED"].map((s) => (
              <Action
                key={s}
                label={
                  s === "CLOSED"
                    ? "모집 종료"
                    : s === "OPEN"
                      ? "모집 다시 열기"
                      : "활동 완료 및 기록 보존"
                }
                onAction={async () => {
                  await request("/teams/" + id + "/status", "PATCH", { status: s });
                  await refresh();
                }}
              />
            ))}
          </div>
        )}
      </Panel>
      {edit && (
        <Panel title="모집 수정">
          <TeamForm
            initial={t}
            onDone={() => {
              setEdit(false);
              refresh();
            }}
          />
        </Panel>
      )}
      {!v.member && (
        <Panel title="팀 지원">
          {v.mine && <p>내 지원 · {labels[v.mine.status]}</p>}
          {v.mine?.status === "PENDING" ? (
            <Action
              label="지원 취소"
              onAction={async () => {
                await request("/teams/" + id + "/leave", "POST");
                await refresh();
              }}
            />
          ) : t.status === "OPEN" &&
            Date.parse(t.deadline) > Date.now() &&
            v.memberCount < t.capacity ? (
            <form
              onSubmit={async (e) => {
                e.preventDefault();
                setError(undefined);
                try {
                  await request(
                    "/teams/" + id + "/applications",
                    "POST",
                    Object.fromEntries(new FormData(e.currentTarget)),
                  );
                  await refresh();
                } catch (e) {
                  setError(e);
                }
              }}
            >
              <Field label="지원 역할">
                <input name="role" required maxLength={100} />
              </Field>
              <Field label="팀 지원 메시지">
                <textarea name="message" required maxLength={5000} />
              </Field>
              <button className="primary">팀 지원하기</button>
            </form>
          ) : (
            <p>현재 지원을 받고 있지 않아요.</p>
          )}
        </Panel>
      )}
      {v.owner && (
        <Panel title="팀 지원자 관리">
          <ErrorMessage error={apps.error} />
          {apps.data?.map((a) => (
            <div className="applicant" key={a.userId}>
              <strong>
                {a.name} · {a.role}
              </strong>
              <p className="prewrap">{a.message}</p>
              <span className="badge">{labels[a.status]}</span>
              {a.status === "PENDING" && t.status !== "COMPLETED" && (
                <div className="toolbar">
                  {["ACCEPTED", "REJECTED"].map((s) => (
                    <Action
                      key={s}
                      label={s === "ACCEPTED" ? "팀원으로 수락" : "지원 반려"}
                      onAction={async () => {
                        await request(
                          "/teams/" + id + "/applications/" + a.userId + "/status",
                          "PATCH",
                          { status: s },
                        );
                        await refresh();
                      }}
                    />
                  ))}
                </div>
              )}
            </div>
          ))}
        </Panel>
      )}
      {v.member && (
        <>
          <Panel title="팀 구성원">
            <ErrorMessage error={members.error} />
            {members.data?.map((m) => (
              <p key={m.id}>
                {m.name} · {m.role}
              </p>
            ))}
            {!v.owner && t.status !== "COMPLETED" && (
              <Action
                label="팀 탈퇴"
                onAction={async () => {
                  await request("/teams/" + id + "/leave", "POST");
                  await refresh();
                }}
              />
            )}
          </Panel>
          <Panel title="팀 공간">
            <p className="muted">모임, 마감, 진행 단계, 할 일과 자료를 함께 남겨요.</p>
            {t.status !== "COMPLETED" && (
              <button
                className="primary"
                onClick={() => {
                  setEditingEntry(undefined);
                  setEntryOpen(!entryOpen);
                }}
              >
                항목 추가
              </button>
            )}
            {entryOpen && (
              <EntryForm
                key={editingEntry?.id || "new"}
                id={t.id}
                members={members.data || []}
                initial={editingEntry}
                onDone={() => {
                  setEntryOpen(false);
                  refresh();
                }}
              />
            )}
            <ErrorMessage error={entries.error} />
            {entries.data?.map((e) => (
              <div className="applicant" key={e.id}>
                <span className="badge">
                  {labels[e.kind]} · {e.done ? "완료" : "진행 중"}
                </span>
                <h3>{e.title}</h3>
                <p className="prewrap">{e.content}</p>
                {e.assigneeId && (
                  <p>담당 · {members.data?.find((m) => m.id === e.assigneeId)?.name}</p>
                )}
                {e.startsAt && <p>시작 · {date(e.startsAt)}</p>}
                {e.endsAt && <p>종료/마감 · {date(e.endsAt)}</p>}
                {e.url && (
                  <a href={e.url} target="_blank" rel="noopener noreferrer">
                    자료 열기
                  </a>
                )}
                {t.status !== "COMPLETED" && (
                  <button
                    className="secondary"
                    onClick={() => {
                      setEditingEntry(e);
                      setEntryOpen(true);
                    }}
                  >
                    항목 수정
                  </button>
                )}
              </div>
            ))}
          </Panel>
        </>
      )}
    </>
  );
}

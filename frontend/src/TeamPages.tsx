import { SaveButton } from "./PersonalPanel";
import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { request } from "./api";
import { Panel, Field, Empty, ErrorMessage, date, instant, Action } from "./ui";
import { Users } from "./Icons";
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
        if (busy) return;
        setBusy(true);
        setError(undefined);
        const f = new FormData(e.currentTarget);
        try {
          if (
            !String(f.get("title")).trim() ||
            !String(f.get("content")).trim() ||
            !String(f.get("roles")).trim()
          )
            throw new Error("팀 제목, 소개, 필요한 역할을 입력해 주세요.");
          if (!initial && Date.parse(String(f.get("deadline"))) <= Date.now())
            throw new Error("지원 마감은 현재 이후로 지정해 주세요.");
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
        <input
          name="roles"
          required
          maxLength={1000}
          defaultValue={initial?.roles}
          placeholder="예: 기획 1명, 프론트엔드 1명, 디자인 1명"
        />
      </Field>
      <Field label="관심 태그">
        <input
          name="tags"
          maxLength={500}
          defaultValue={initial?.tags}
          placeholder="개발, 공모전, 디자인 · 쉼표로 구분"
        />
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
          defaultValue={
            initial
              ? local(initial.deadline)
              : local(new Date(Date.now() + 7 * 86400000).toISOString())
          }
        />
      </Field>
      <ErrorMessage error={error} />
      <button className="primary" disabled={busy}>
        {busy ? "저장 중…" : "팀 모집 저장"}
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
          <input
            type="search"
            maxLength={200}
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="제목 · 필요한 역할 · 관심 태그"
          />
        </Field>
        <label className="checks">
          <input type="checkbox" checked={mine} onChange={(e) => setMine(e.target.checked)} />내
          팀만
        </label>
        {(search || mine) && (
          <button
            className="text-button"
            onClick={() => {
              setSearch("");
              setMine(false);
            }}
          >
            필터 초기화
          </button>
        )}
        <button className="primary" onClick={() => setCreate(!create)}>
          {create ? "닫기" : "팀 모으기"}
        </button>
      </div>
      {create && (
        <Panel title="팀 모으기">
          <TeamForm onDone={(v) => nav("/teams/" + v.team.id)} />
        </Panel>
      )}
      {q.data && (
        <p className="count-label" role="status">
          팀 {q.data.length}개{q.isFetching && " · 업데이트 중…"}
        </p>
      )}
      <ErrorMessage error={q.error} />
      {q.isPending ? (
        <Empty>불러오는 중…</Empty>
      ) : (
        !q.data?.length &&
        !q.error && (
          <Empty>
            {mine
              ? "아직 참여 중인 팀이 없어요. 팀을 만들거나 모집에 지원해 보세요."
              : search
                ? "검색 조건에 맞는 팀이 없어요. 역할이나 관심 분야로 다시 찾아보세요."
                : "등록된 팀이 없어요. 함께할 동료를 모아 보세요."}
          </Empty>
        )
      )}
      <div className="grid">
        {q.data?.map((v) => (
          <Link
            key={v.team.id}
            className="card card-link opportunity-card"
            to={"/teams/" + v.team.id}
          >
            <div className="card-topline">
              <span className="card-accent team-accent">
                <Users size={23} />
              </span>
              <span className="badge">
                {v.team.status === "OPEN" && Date.parse(v.team.deadline) <= Date.now()
                  ? "기간 마감"
                  : v.team.status === "OPEN" && v.memberCount >= v.team.capacity
                    ? "정원 마감"
                    : labels[v.team.status]}
              </span>
            </div>
            <h2>{v.team.title}</h2>
            <p className="muted card-excerpt">{v.team.content}</p>
            <div className="chip-list">
              {v.team.tags
                .split(/[,，]/)
                .map((tag) => tag.trim())
                .filter(Boolean)
                .slice(0, 4)
                .map((tag, i) => (
                  <span className="chip" key={tag + i}>
                    {tag}
                  </span>
                ))}
            </div>
            <p className="meta card-excerpt">모집 역할 · {v.team.roles}</p>
            <div className="card-footer">
              <strong>
                {v.memberCount}/{v.team.capacity}명
              </strong>
              <span className="meta">마감 {date(v.team.deadline)}</span>
            </div>
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
  const [kind, setKind] = useState(initial?.kind || "TASK");
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        if (busy) return;
        setBusy(true);
        setError(undefined);
        const f = new FormData(e.currentTarget);
        try {
          if (!String(f.get("title")).trim()) throw new Error("항목 제목을 입력해 주세요.");
          if (
            kind === "MEETING" &&
            Date.parse(String(f.get("startsAt"))) >= Date.parse(String(f.get("endsAt")))
          )
            throw new Error("모임 종료는 시작 이후로 지정해 주세요.");
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
        <select name="kind" value={kind} onChange={(e) => setKind(e.target.value)}>
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
      {kind === "MEETING" && (
        <Field label="모임 시작">
          <input
            name="startsAt"
            type="datetime-local"
            required
            defaultValue={initial?.startsAt ? local(initial.startsAt) : undefined}
          />
        </Field>
      )}
      {["MEETING", "DEADLINE", "TASK"].includes(kind) && (
        <Field label={kind === "MEETING" ? "모임 종료" : "마감 일시"}>
          <input
            name="endsAt"
            type="datetime-local"
            required={kind !== "TASK"}
            defaultValue={initial?.endsAt ? local(initial.endsAt) : undefined}
          />
        </Field>
      )}
      <Field label="관련 자료 주소 · 선택 사항">
        <input
          name="url"
          type="url"
          placeholder="https://"
          maxLength={2000}
          defaultValue={initial?.url}
        />
      </Field>
      <label>
        <input type="checkbox" name="done" defaultChecked={initial?.done} />
        완료
      </label>
      <ErrorMessage error={error} />
      <button className="primary" disabled={busy}>
        {busy ? "저장 중…" : "팀 항목 저장"}
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
  const [applying, setApplying] = useState(false);
  useEffect(() => {
    setEdit(false);
    setEntryOpen(false);
    setEditingEntry(undefined);
    setError(undefined);
  }, [id]);
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
      <Link to="/discover?tab=teams">← 팀 모집 목록</Link>
      <div className="page-heading">
        <h1>{t.title}</h1>
        <SaveButton type="TEAM" id={t.id} />
        <p>
          {t.status === "OPEN" && Date.parse(t.deadline) <= Date.now()
            ? "기간 마감"
            : t.status === "OPEN" && v.memberCount >= t.capacity
              ? "정원 마감"
              : labels[t.status]}{" "}
          · {v.memberCount}/{t.capacity}명 · 마감 {date(t.deadline)}
        </p>
      </div>
      <ErrorMessage error={error} />
      <Panel title="팀 소개">
        <p className="prewrap">{t.content}</p>
        <p>필요 역할 · {t.roles}</p>
        {t.tags && (
          <div className="chip-list">
            {t.tags
              .split(/[,，]/)
              .map((tag) => tag.trim())
              .filter(Boolean)
              .map((tag, i) => (
                <span className="chip" key={tag + i}>
                  {tag}
                </span>
              ))}
          </div>
        )}
        {v.owner && t.status !== "COMPLETED" && (
          <div className="toolbar">
            <button className="secondary" onClick={() => setEdit(!edit)}>
              모집 수정
            </button>
            {[
              ...(t.status === "OPEN"
                ? ["CLOSED"]
                : Date.parse(t.deadline) > Date.now()
                  ? ["OPEN"]
                  : []),
              "COMPLETED",
            ].map((s) => (
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
                  if (
                    s === "COMPLETED" &&
                    !confirm(
                      "활동을 완료하면 팀 공간이 기록으로 보존되고 수정할 수 없어요. 완료할까요?",
                    )
                  )
                    return;
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
          <p className="muted">
            함께 맡고 싶은 역할과 경험을 알려 주세요. 팀장이 검토 후 결과를 안내해요.
          </p>
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
                if (applying) return;
                setApplying(true);
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
                } finally {
                  setApplying(false);
                }
              }}
            >
              <Field label="지원 역할">
                <input name="role" required maxLength={100} placeholder="예: 프론트엔드 개발" />
              </Field>
              <Field label="팀 지원 메시지">
                <textarea
                  name="message"
                  required
                  maxLength={5000}
                  rows={5}
                  placeholder="관심 있는 이유, 관련 경험, 함께할 수 있는 시간을 알려 주세요."
                />
              </Field>
              <button className="primary" disabled={applying}>
                {applying ? "지원서 제출 중…" : "팀 지원하기"}
              </button>
            </form>
          ) : (
            <p className="inline-notice">
              {t.status === "COMPLETED"
                ? "활동을 마친 팀이에요. 새로운 팀을 찾아보세요."
                : v.memberCount >= t.capacity
                  ? "현재 팀 정원이 모두 찼어요."
                  : Date.parse(t.deadline) <= Date.now()
                    ? "지원 기간이 마감됐어요."
                    : "현재 모집을 종료한 팀이에요."}
            </p>
          )}
        </Panel>
      )}
      {v.owner && (
        <Panel title="팀 지원자 관리">
          <ErrorMessage error={apps.error} />
          {apps.isPending && <Empty>지원서를 불러오고 있어요…</Empty>}
          {!apps.isPending && !apps.error && !apps.data?.length && (
            <Empty>아직 접수된 지원서가 없어요.</Empty>
          )}
          {apps.data?.map((a) => (
            <div className="applicant" key={a.userId}>
              <strong>
                {a.name} · {a.role}
              </strong>
              <p className="prewrap">{a.message}</p>
              <span className="badge">{labels[a.status]}</span>
              {a.status === "PENDING" && v.memberCount >= t.capacity && (
                <p className="meta">정원이 찼어요. 정원을 늘리면 지원자를 수락할 수 있어요.</p>
              )}
              {a.status === "PENDING" && t.status !== "COMPLETED" && (
                <div className="toolbar">
                  {(v.memberCount < t.capacity ? ["ACCEPTED", "REJECTED"] : ["REJECTED"]).map(
                    (s) => (
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
                    ),
                  )}
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
            {members.isPending && <Empty>구성원을 불러오고 있어요…</Empty>}
            <div className="member-list">
              {members.data?.map((m) => (
                <span className="member-pill" key={m.id}>
                  <strong>{m.name}</strong>
                  <span className="meta">{m.role}</span>
                </span>
              ))}
            </div>
            {!v.owner && t.status !== "COMPLETED" && (
              <Action
                label="팀 탈퇴"
                onAction={async () => {
                  if (!confirm("이 팀에서 탈퇴할까요? 팀 공간에 더 이상 접근할 수 없어요.")) return;
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
                {entryOpen ? "입력 닫기" : "항목 추가"}
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
            {entries.isPending && <Empty>팀 활동을 불러오고 있어요…</Empty>}
            {!entries.isPending && !entries.error && !entries.data?.length && (
              <Empty>
                팀의 첫 활동을 남겨 보세요. 모임 일정, 할 일, 자료를 함께 관리할 수 있어요.
              </Empty>
            )}
            {t.status === "COMPLETED" && (
              <p className="inline-notice">
                활동이 완료되어 팀 기록을 읽기 전용으로 보관하고 있어요.
              </p>
            )}
            {entries.data?.map((e) => (
              <div className="applicant" key={e.id}>
                <span className="badge">
                  {labels[e.kind]} · {e.done ? "완료" : "진행 중"}
                </span>
                <h3>{e.title}</h3>
                <p className="prewrap">{e.content}</p>
                {e.assigneeId && (
                  <p>
                    담당 ·{" "}
                    {members.data?.find((m) => m.id === e.assigneeId)?.name || "구성원 확인 중"}
                  </p>
                )}
                {e.startsAt && <p>시작 · {date(e.startsAt)}</p>}
                {e.endsAt && (
                  <p>
                    {e.kind === "MEETING" ? "종료" : "마감"} · {date(e.endsAt)}
                  </p>
                )}
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

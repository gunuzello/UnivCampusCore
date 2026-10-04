import { useState, type FormEvent } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useNavigate, useParams } from "react-router-dom";
import { request, download, type Organization, type Profile } from "./api";
import LinksPanel from "./LinksPanel";
import { Panel, Field, ErrorMessage, Empty, Action, Status, date, instant } from "./ui";
export type Kind = "events" | "recruitments";
export type Activity = {
  id: number;
  organizationId: number;
  title: string;
  description: string;
  opensAt: string;
  closesAt: string;
  startsAt?: string;
  endsAt?: string;
  location?: string;
  capacity?: number;
  status: string;
  questions: string[];
  applicationCount: number;
  canManage: boolean;
};
type Application = {
  id: number;
  userId: number;
  name: string;
  email: string;
  department: string;
  studentNumber: string;
  status: string;
  answers: string[];
  submittedAt: string;
};
const label = (kind: Kind) => (kind === "events" ? "행사" : "모집");
const local = (value: string) => {
  const d = new Date(value);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
export function ActivityForm({
  kind,
  orgId,
  initial,
  copy = false,
  onDone,
}: {
  kind: Kind;
  orgId: number;
  initial?: Activity;
  copy?: boolean;
  onDone: (item: Activity) => void;
}) {
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const [questions, setQuestions] = useState(initial?.questions || []);
  const now = Date.now();
  async function submit(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    setBusy(true);
    setError(undefined);
    const f = Object.fromEntries(new FormData(e.currentTarget));
    try {
      const body = {
        ...f,
        opensAt: instant(f.opensAt as string),
        closesAt: instant(f.closesAt as string),
        questions: questions.filter((q) => q.trim()).map((q) => q.trim()),
        ...(kind === "events"
          ? {
              startsAt: instant(f.startsAt as string),
              endsAt: instant(f.endsAt as string),
              capacity: Number(f.capacity),
            }
          : {}),
      };
      const path = initial
        ? copy
          ? "/events/" + initial.id + "/copies"
          : "/" + kind + "/" + initial.id
        : "/organizations/" + orgId + "/" + kind;
      const item = await request<Activity>(path, initial && !copy ? "PATCH" : "POST", body);
      onDone(item);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <form onSubmit={submit}>
      <Field label="제목">
        <input
          name="title"
          required
          maxLength={200}
          defaultValue={initial ? (copy ? initial.title + " (복제)" : initial.title) : ""}
        />
      </Field>
      <Field label="설명">
        <textarea name="description" maxLength={20000} defaultValue={initial?.description || ""} />
      </Field>
      <div className="columns">
        <Field label="신청 시작">
          <input
            type="datetime-local"
            name="opensAt"
            required
            defaultValue={
              initial && !copy ? local(initial.opensAt) : local(new Date(now - 60000).toISOString())
            }
          />
        </Field>
        <Field label="신청 마감">
          <input
            type="datetime-local"
            name="closesAt"
            required
            defaultValue={
              initial && !copy
                ? local(initial.closesAt)
                : local(new Date(now + 86400000 * 7).toISOString())
            }
          />
        </Field>
      </div>
      {kind === "events" && (
        <>
          <div className="columns">
            <Field label="행사 시작">
              <input
                type="datetime-local"
                name="startsAt"
                required
                defaultValue={
                  initial?.startsAt && !copy
                    ? local(initial.startsAt)
                    : local(new Date(now + 86400000 * 8).toISOString())
                }
              />
            </Field>
            <Field label="행사 종료">
              <input
                type="datetime-local"
                name="endsAt"
                required
                defaultValue={
                  initial?.endsAt && !copy
                    ? local(initial.endsAt)
                    : local(new Date(now + 86400000 * 8 + 7200000).toISOString())
                }
              />
            </Field>
          </div>
          <Field label="장소">
            <input
              name="location"
              defaultValue={initial?.location || ""}
              required
              maxLength={200}
            />
          </Field>
          <Field label="정원">
            <input
              type="number"
              name="capacity"
              defaultValue={initial?.capacity || 60}
              min={1}
              max={100000}
              required
            />
          </Field>
        </>
      )}
      <div className="section-title">
        <h3>{kind === "events" ? "추가 신청 질문" : "지원 질문"}</h3>
        <button
          type="button"
          className="secondary"
          disabled={questions.length >= 20 || (!!initial && !copy && initial.status !== "DRAFT")}
          onClick={() => setQuestions([...questions, ""])}
        >
          질문 추가
        </button>
      </div>
      <p className="meta">모든 질문은 필수 답변이에요. 공개한 뒤에는 변경할 수 없어요.</p>
      {questions.map((q, i) => (
        <div className="toolbar" key={i}>
          <input
            aria-label={"질문 " + (i + 1)}
            value={q}
            required
            maxLength={500}
            disabled={!!initial && !copy && initial.status !== "DRAFT"}
            onChange={(e) => setQuestions(questions.map((v, j) => (j === i ? e.target.value : v)))}
          />
          <button
            type="button"
            className="text-button"
            disabled={!!initial && !copy && initial.status !== "DRAFT"}
            onClick={() => setQuestions(questions.filter((_, j) => j !== i))}
          >
            삭제
          </button>
        </div>
      ))}
      <ErrorMessage error={error} />
      <button className="primary" disabled={busy}>
        {busy ? "저장 중…" : initial && !copy ? "변경 저장" : label(kind) + " 초안 만들기"}
      </button>
    </form>
  );
}
export function ActivityList({ kind, org }: { kind: Kind; org?: Organization }) {
  const q = useQuery({
    queryKey: [kind, org?.id],
    queryFn: () => request<Activity[]>("/organizations/" + org!.id + "/" + kind),
    enabled: !!org,
  });
  if (!org) return <Empty>소속에서 학생회를 만들거나 선택해 주세요.</Empty>;
  return (
    <>
      <div className="page-heading">
        <h1>{label(kind)}</h1>
        <p>
          {kind === "events"
            ? "학생회의 행사에 함께해요."
            : "우리 학생회의 다음 이야기를 함께 만들어요."}
        </p>
      </div>
      <div className="toolbar">
        {org.role && org.role !== "MEMBER" && (
          <Link className="primary link-button" to={"/" + kind + "/new"}>
            {label(kind)} 만들기
          </Link>
        )}
      </div>
      <ErrorMessage error={q.error} />
      {q.isPending ? (
        <Empty>불러오는 중…</Empty>
      ) : !q.data?.length ? (
        <Empty>아직 등록한 {label(kind)}가 없어요.</Empty>
      ) : (
        <div className="grid">
          {q.data.map((item) => (
            <Link className="card card-link" to={"/" + kind + "/" + item.id} key={item.id}>
              <div className="toolbar">
                <Status value={item.status} />
                <span className="meta">
                  {kind === "events" ? item.location : "신입 구성원 모집"}
                </span>
              </div>
              <h2>{item.title}</h2>
              <p className="muted">
                {item.description?.slice(0, 100) || "상세 내용을 확인해 보세요."}
              </p>
              <p className="meta">신청 마감 · {date(item.closesAt)}</p>
              <strong>
                {item.applicationCount}
                {kind === "events" ? " / " + item.capacity + "명 신청" : "명 지원"}
              </strong>
            </Link>
          ))}
        </div>
      )}
    </>
  );
}
export function ActivityCreate({ kind, org }: { kind: Kind; org?: Organization }) {
  const nav = useNavigate();
  const query = useQueryClient();
  if (!org || !org.role || org.role === "MEMBER") return <Empty>운영진 권한이 필요해요.</Empty>;
  return (
    <Panel title={label(kind) + " 만들기"}>
      <ActivityForm
        kind={kind}
        orgId={org.id}
        onDone={async (item) => {
          await query.invalidateQueries({ queryKey: [kind] });
          nav("/" + kind + "/" + item.id);
        }}
      />
    </Panel>
  );
}
export function ActivityDetail({ kind, user }: { kind: Kind; user: Profile }) {
  const { id } = useParams();
  const query = useQueryClient();
  const nav = useNavigate();
  const [editing, setEditing] = useState(false);
  const [copying, setCopying] = useState(false);
  const [applicantSearch, setApplicantSearch] = useState("");
  const [applicantStatus, setApplicantStatus] = useState("");
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const orgs = useQuery({
    queryKey: ["organizations"],
    queryFn: () => request<Organization[]>("/organizations"),
  });
  const item = useQuery({
    queryKey: [kind, "detail", id],
    queryFn: () => request<Activity>("/" + kind + "/" + id),
  });
  const mine = useQuery({
    queryKey: [kind, "mine", id],
    queryFn: () => request<Application | null>("/" + kind + "/" + id + "/applications/me"),
  });
  const applicants = useQuery({
    queryKey: [kind, "applicants", id],
    queryFn: () => request<Application[]>("/" + kind + "/" + id + "/applications"),
    enabled: !!item.data?.canManage,
  });
  async function refresh() {
    await query.invalidateQueries({ queryKey: [kind] });
    await query.invalidateQueries({ queryKey: ["notifications"] });
    await query.invalidateQueries({ queryKey: ["my-applications"] });
    await query.invalidateQueries({ queryKey: ["calendar"] });
    await query.invalidateQueries({ queryKey: ["archive"] });
  }
  if (item.isPending) return <Empty>불러오는 중…</Empty>;
  if (item.error) return <ErrorMessage error={item.error} />;
  const a = item.data!;
  const applicantLabels: Record<string, string> =
    kind === "events"
      ? { REGISTERED: "신청 완료", ATTENDED: "참가 완료", ABSENT: "불참", CANCELLED: "취소" }
      : {
          SUBMITTED: "접수",
          REVIEWING: "검토 중",
          ACCEPTED: "합격",
          REJECTED: "불합격",
          CANCELLED: "취소",
        };
  const search = applicantSearch.trim().toLowerCase();
  const filteredApplicants = applicants.data?.filter(
    (p) =>
      (!applicantStatus || p.status === applicantStatus) &&
      [p.name, p.email, p.department, p.studentNumber].some((value) =>
        (value || "").toLowerCase().includes(search),
      ),
  );

  const closed = !(
    a.status === "PUBLISHED" &&
    Date.now() >= Date.parse(a.opensAt) &&
    Date.now() < Date.parse(a.closesAt)
  );
  const status = async (next: string) => {
    await request("/" + kind + "/" + id + "/status", "PATCH", { status: next });
    await refresh();
  };
  return (
    <>
      <div className="toolbar">
        <Link to={"/" + kind}>← {label(kind)} 목록</Link>
        <Status value={a.status} />
      </div>
      <div className="page-heading">
        <h1>{a.title}</h1>
        <p>
          {kind === "events" && a.startsAt
            ? date(a.startsAt) + " · " + a.location
            : "학생회 신규 구성원 모집"}
        </p>
      </div>
      {(editing || copying) && (
        <Panel title={copying ? "지난 행사 재사용" : "내용 수정"}>
          <p className="muted">
            {copying ? "참가자 정보는 복제하지 않아요. 새로운 날짜를 지정해 주세요." : ""}
          </p>
          <ActivityForm
            key={copying ? "copy" : "edit"}
            kind={kind}
            orgId={a.organizationId}
            initial={a}
            copy={copying}
            onDone={async (newItem) => {
              setEditing(false);
              setCopying(false);
              await refresh();
              nav("/" + kind + "/" + newItem.id);
            }}
          />
          <button
            className="text-button"
            onClick={() => {
              setEditing(false);
              setCopying(false);
            }}
          >
            닫기
          </button>
        </Panel>
      )}
      <div className="columns">
        <div>
          <Panel title="활동 안내">
            <p className="prewrap">{a.description || "등록된 설명이 없어요."}</p>
            <div className="divider" />
            <p className="meta">
              신청 시작 · {date(a.opensAt)}
              <br />
              신청 마감 · {date(a.closesAt)}
            </p>
            {kind === "events" && (
              <p className="meta">
                행사 종료 · {date(a.endsAt!)}
                <br />
                정원 · {a.applicationCount} / {a.capacity}명
              </p>
            )}
          </Panel>
          {orgs.data?.find((o) => o.id === a.organizationId)?.role && (
            <LinksPanel
              orgId={a.organizationId}
              type={kind === "events" ? "EVENT" : "RECRUITMENT"}
              targetId={a.id}
              manage={a.canManage}
            />
          )}{" "}
          {a.canManage && (
            <Panel title="운영 관리">
              <div className="toolbar">
                {a.status === "DRAFT" && (
                  <Action label="공개하기" onAction={() => status("PUBLISHED")} />
                )}{" "}
                {a.status === "PUBLISHED" && (
                  <Action label="신청 마감" onAction={() => status("CLOSED")} />
                )}{" "}
                {kind === "events" && a.status === "CLOSED" && (
                  <Action label="행사 완료" onAction={() => status("COMPLETED")} />
                )}{" "}
                {!["COMPLETED", "CANCELLED"].includes(a.status) && (
                  <Action
                    label="활동 취소"
                    danger
                    onAction={async () => {
                      if (confirm("활동을 취소할까요? 신청자에게 알림이 전달됩니다."))
                        await status("CANCELLED");
                    }}
                  />
                )}
                {!["COMPLETED", "CANCELLED"].includes(a.status) && (
                  <button
                    className="secondary"
                    onClick={() => {
                      setEditing(true);
                      setCopying(false);
                    }}
                  >
                    내용 수정
                  </button>
                )}
                {kind === "events" && (
                  <button
                    className="secondary"
                    onClick={() => {
                      setCopying(true);
                      setEditing(false);
                    }}
                  >
                    새 행사로 복제
                  </button>
                )}
              </div>
            </Panel>
          )}
          {a.canManage && (
            <Panel title={kind === "events" ? "신청자 관리" : "지원자 관리"}>
              <ErrorMessage error={applicants.error} />
              <div className="applicant-filters">
                <Field label="신청자 검색">
                  <input
                    type="search"
                    maxLength={200}
                    value={applicantSearch}
                    placeholder="이름 · 이메일 · 학과 · 학번"
                    onChange={(e) => setApplicantSearch(e.target.value)}
                  />
                </Field>
                <Field label="신청 상태">
                  <select
                    value={applicantStatus}
                    onChange={(e) => setApplicantStatus(e.target.value)}
                  >
                    <option value="">전체 상태</option>
                    {Object.entries(applicantLabels).map(([value, name]) => (
                      <option key={value} value={value}>
                        {name}
                      </option>
                    ))}
                  </select>
                </Field>
              </div>
              {applicants.data && (
                <>
                  <div className="toolbar">
                    <span className="meta" role="status">
                      전체 {applicants.data.length}명 · 검색 결과 {filteredApplicants?.length || 0}
                      명
                    </span>
                    <Action
                      label="명단 CSV 다운로드"
                      onAction={() =>
                        download(
                          "/" +
                            kind +
                            "/" +
                            id +
                            "/applications/export?" +
                            new URLSearchParams({
                              search: applicantSearch,
                              status: applicantStatus,
                            }),
                          kind + "-" + id + "-applicants.csv",
                        )
                      }
                    />
                    {(applicantSearch || applicantStatus) && (
                      <button
                        className="text-button"
                        onClick={() => {
                          setApplicantSearch("");
                          setApplicantStatus("");
                        }}
                      >
                        필터 초기화
                      </button>
                    )}
                  </div>
                  <p className="meta">
                    현재 검색·상태 필터에 맞는 명단과 질문 답변을 내려받아요. 취소한 신청은 취소
                    필터에서 확인할 수 있어요.
                  </p>
                </>
              )}
              {applicants.isPending && <p className="muted">명단을 불러오고 있어요…</p>}
              {applicants.data && !filteredApplicants?.length && (
                <Empty>
                  {applicants.data.length
                    ? "검색 조건에 맞는 신청자가 없어요."
                    : "아직 신청자가 없어요."}
                </Empty>
              )}
              {filteredApplicants?.map((p) => (
                <details key={p.id} className="applicant">
                  <summary>
                    <span>
                      <strong>{p.name}</strong>
                      <small>
                        {p.department} · {p.studentNumber} · {p.email}
                      </small>
                    </span>
                    <Status value={p.status} />
                  </summary>
                  <p className="meta">제출 · {date(p.submittedAt)}</p>
                  {a.questions.map((q, i) => (
                    <div key={i} className="answer">
                      <strong>{q}</strong>
                      <p className="prewrap">{p.answers[i]}</p>
                    </div>
                  ))}
                  {p.status !== "CANCELLED" && (
                    <div className="toolbar">
                      {(kind === "events"
                        ? a.status === "CLOSED"
                          ? ["REGISTERED", "ATTENDED", "ABSENT"]
                          : []
                        : a.status !== "CANCELLED"
                          ? ["SUBMITTED", "REVIEWING", "ACCEPTED", "REJECTED"]
                          : []
                      ).map((s) => (
                        <Action
                          key={s}
                          label={
                            {
                              REGISTERED: "신청",
                              ATTENDED: "참가",
                              ABSENT: "불참",
                              SUBMITTED: "접수",
                              REVIEWING: "검토",
                              ACCEPTED: "합격",
                              REJECTED: "불합격",
                            }[s] || s
                          }
                          onAction={async () => {
                            await request(
                              "/" + kind + "/" + id + "/applications/" + p.id,
                              "PATCH",
                              { status: s },
                            );
                            await refresh();
                          }}
                        />
                      ))}
                    </div>
                  )}
                </details>
              ))}
            </Panel>
          )}
        </div>
        <Panel title={kind === "events" ? "행사 신청" : "지원서 작성"}>
          <div className="profile-summary">
            <strong>{user.name}</strong>
            <p className="meta">
              {user.email}
              <br />
              {user.department || "학과 미등록"} · {user.studentNumber || "학번 미등록"}
            </p>
            <Link to="/profile" className="meta">
              기본 정보 수정 →
            </Link>
          </div>
          <ErrorMessage error={mine.error} />
          {mine.data && mine.data.status !== "CANCELLED" ? (
            <>
              <div className="divider" />
              <Status value={mine.data.status} />
              {a.questions.map((q, i) => (
                <div className="answer" key={i}>
                  <strong>{q}</strong>
                  <p className="prewrap">{mine.data!.answers[i]}</p>
                </div>
              ))}
              {!closed && (
                <Action
                  label="신청 취소"
                  danger
                  onAction={async () => {
                    if (!confirm("신청을 취소할까요?")) return;
                    await request("/" + kind + "/" + id + "/applications/me", "DELETE");
                    await refresh();
                  }}
                />
              )}
            </>
          ) : (
            <form
              onSubmit={async (e: FormEvent<HTMLFormElement>) => {
                e.preventDefault();
                setBusy(true);
                setError(undefined);
                const f = new FormData(e.currentTarget);
                try {
                  await request("/" + kind + "/" + id + "/applications", "POST", {
                    answers: a.questions.map((_, i) => f.get("answer-" + i)),
                  });
                  await refresh();
                } catch (e) {
                  setError(e);
                } finally {
                  setBusy(false);
                }
              }}
            >
              {a.questions.map((q, i) => (
                <Field key={i} label={q}>
                  <textarea name={"answer-" + i} required maxLength={5000} disabled={closed} />
                </Field>
              ))}
              {closed && <p className="muted">현재 신청 기간이 아니거나 활동이 마감되었어요.</p>}
              <ErrorMessage error={error} />
              <button className="primary wide" disabled={closed || busy || mine.isPending}>
                {busy ? "제출 중…" : kind === "events" ? "행사 신청하기" : "지원서 제출하기"}
              </button>
            </form>
          )}
        </Panel>
      </div>
    </>
  );
}

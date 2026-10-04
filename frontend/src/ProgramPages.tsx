import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { request, type Organization } from "./api";
import { Panel, Field, Empty, ErrorMessage, date, instant } from "./ui";
export type Program = {
  id: number;
  organizationId: number;
  title: string;
  content: string;
  category: string;
  tags: string;
  applicationUrl: string;
  deadline: string;
  startsAt: string;
  endsAt: string;
  published: boolean;
  canManage: boolean;
};
const local = (v: string) => {
  const d = new Date(v);
  return new Date(d.getTime() - d.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
};
function ProgramForm({
  org,
  initial,
  onDone,
}: {
  org: number;
  initial?: Program;
  onDone: () => void;
}) {
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  return (
    <form
      onSubmit={async (e) => {
        e.preventDefault();
        setError(undefined);
        setBusy(true);
        const f = new FormData(e.currentTarget);
        try {
          await request(
            initial ? "/programs/" + initial.id : "/organizations/" + org + "/programs",
            initial ? "PATCH" : "POST",
            {
              ...Object.fromEntries(f),
              deadline: instant(String(f.get("deadline"))),
              startsAt: instant(String(f.get("startsAt"))),
              endsAt: instant(String(f.get("endsAt"))),
              published: f.get("published") === "on",
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
      {[
        ["title", "프로그램 제목", 200],
        ["category", "분류", 80],
        ["tags", "관심 태그 · 쉼표로 구분", 500],
        ["applicationUrl", "외부 신청 주소", 2000],
      ].map(([name, label, max]) => (
        <Field key={String(name)} label={String(label)}>
          <input
            name={String(name)}
            required={name === "title" || name === "category"}
            maxLength={Number(max)}
            defaultValue={(initial?.[name as keyof Program] as string) || ""}
          />
        </Field>
      ))}
      <Field label="프로그램 안내">
        <textarea name="content" required maxLength={30000} defaultValue={initial?.content} />
      </Field>
      {[
        ["deadline", "신청 마감"],
        ["startsAt", "진행 시작"],
        ["endsAt", "진행 종료"],
      ].map(([name, label]) => (
        <Field key={name} label={label}>
          <input
            type="datetime-local"
            name={name}
            required
            defaultValue={
              initial ? local(initial[name as "deadline" | "startsAt" | "endsAt"]) : undefined
            }
          />
        </Field>
      ))}
      <label className="checks">
        <input type="checkbox" name="published" defaultChecked={initial?.published} />
        공개하기
      </label>
      <ErrorMessage error={error} />
      <button className="primary" disabled={busy}>
        프로그램 저장
      </button>
    </form>
  );
}
export function ProgramList({ org }: { org?: Organization }) {
  const [search, setSearch] = useState("");
  const [category, setCategory] = useState("");
  const [openOnly, setOpenOnly] = useState(false);
  const [create, setCreate] = useState(false);
  const client = useQueryClient();
  const q = useQuery({
    queryKey: ["programs", search, category, openOnly],
    queryFn: () =>
      request<Program[]>(
        "/programs?" + new URLSearchParams({ search, category, openOnly: String(openOnly) }),
      ),
  });
  return (
    <>
      <div className="toolbar">
        <Field label="프로그램 검색">
          <input value={search} onChange={(e) => setSearch(e.target.value)} />
        </Field>
        <Field label="분류 필터">
          <input
            value={category}
            onChange={(e) => setCategory(e.target.value)}
            placeholder="분류 이름"
          />
        </Field>
        <label>
          <input
            type="checkbox"
            checked={openOnly}
            onChange={(e) => setOpenOnly(e.target.checked)}
          />
          신청 가능한 프로그램
        </label>
      </div>
      {org?.role && org.role !== "MEMBER" && (
        <button className="primary" onClick={() => setCreate(!create)}>
          {create ? "닫기" : "프로그램 등록"}
        </button>
      )}
      {create && org && (
        <Panel title="프로그램 등록">
          <ProgramForm
            org={org.id}
            onDone={() => {
              setCreate(false);
              client.invalidateQueries({ queryKey: ["programs"] });
            }}
          />
        </Panel>
      )}
      <ErrorMessage error={q.error} />
      {q.isPending ? (
        <Empty>불러오는 중…</Empty>
      ) : (
        !q.data?.length && !q.error && <Empty>등록된 프로그램이 없어요.</Empty>
      )}
      <div className="grid">
        {q.data?.map((p) => (
          <Link className="card card-link" key={p.id} to={"/programs/" + p.id}>
            <span className="badge">
              {p.category} ·{" "}
              {p.published
                ? Date.parse(p.deadline) > Date.now()
                  ? "신청 가능"
                  : "마감"
                : "비공개"}
            </span>
            <h2>{p.title}</h2>
            <p className="meta">마감 {date(p.deadline)}</p>
            <p>{p.tags}</p>
          </Link>
        ))}
      </div>
    </>
  );
}
export function ProgramDetail() {
  const { id } = useParams();
  const client = useQueryClient();
  const [edit, setEdit] = useState(false);
  const q = useQuery({
    queryKey: ["programs", "detail", id],
    queryFn: () => request<Program>("/programs/" + id),
  });
  if (q.isPending) return <Empty>불러오는 중…</Empty>;
  if (q.error) return <ErrorMessage error={q.error} />;
  const p = q.data!;
  return (
    <>
      <Link to="/discover">← 찾기</Link>
      <div className="page-heading">
        <h1>{p.title}</h1>
        <p>
          {p.category} · {p.tags}
        </p>
      </div>
      {p.canManage && (
        <button className="secondary" onClick={() => setEdit(!edit)}>
          {edit ? "닫기" : "프로그램 수정"}
        </button>
      )}
      {edit ? (
        <Panel title="프로그램 수정">
          <ProgramForm
            org={p.organizationId}
            initial={p}
            onDone={() => {
              setEdit(false);
              client.invalidateQueries({ queryKey: ["programs"] });
            }}
          />
        </Panel>
      ) : (
        <Panel title="상세 안내">
          <p className="prewrap">{p.content}</p>
          <p>신청 마감 · {date(p.deadline)}</p>
          <p>
            진행 · {date(p.startsAt)} — {date(p.endsAt)}
          </p>
          {p.applicationUrl && p.published && Date.parse(p.deadline) > Date.now() ? (
            <a
              className="primary"
              href={p.applicationUrl}
              target="_blank"
              rel="noopener noreferrer"
            >
              외부 신청 페이지 열기
            </a>
          ) : (
            <p className="muted">
              {Date.parse(p.deadline) <= Date.now()
                ? "신청 기간이 마감됐어요."
                : "외부 신청 주소가 등록되지 않았어요."}
            </p>
          )}
        </Panel>
      )}
    </>
  );
}

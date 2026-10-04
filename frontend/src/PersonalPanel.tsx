import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request } from "./api";
import { Panel, Field, ErrorMessage, Empty, Action } from "./ui";
type Interests = {
  interests: string;
  activities: string;
  courses: string;
  skills: string;
  portfolio: string;
};
type Saved = { type: string; targetId: number; title: string; path: string };
export function SaveButton({ type, id }: { type: string; id: number }) {
  const client = useQueryClient();
  const q = useQuery({ queryKey: ["saved"], queryFn: () => request<Saved[]>("/me/saved") });
  const exists = q.data?.some((x) => x.type === type && x.targetId === id);
  return (
    <Action
      label={exists ? "저장 해제" : "활동 저장"}
      onAction={async () => {
        await request("/me/saved/" + type + "/" + id, exists ? "DELETE" : "PUT");
        await client.invalidateQueries({ queryKey: ["saved"] });
        await client.invalidateQueries({ queryKey: ["calendar"] });
      }}
    />
  );
}
export function Recommendations() {
  const q = useQuery({
    queryKey: ["recommendations"],
    queryFn: () =>
      request<{ id: number; type: string; title: string; path: string; reason: string }[]>(
        "/me/recommendations",
      ),
  });
  return (
    <Panel title="관심 키워드로 찾는 기회">
      <p className="muted">프로필에 등록한 관심 분야·활동과 공고 내용을 비교해요.</p>
      <ErrorMessage error={q.error} />
      {!q.isPending && !q.error && !q.data?.length && (
        <Empty>관심 정보를 입력하거나 새로운 공고를 기다려 주세요.</Empty>
      )}
      {q.data?.map((r) => (
        <Link className="list-row" key={r.type + r.id} to={r.path}>
          <div>
            <strong>{r.title}</strong>
            <p className="meta">{r.reason}</p>
          </div>
        </Link>
      ))}
    </Panel>
  );
}
export default function PersonalPanel() {
  const client = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const [saved, setSaved] = useState(false);
  const q = useQuery({
    queryKey: ["interests"],
    queryFn: () => request<Interests>("/me/interests"),
  });
  const bookmarks = useQuery({ queryKey: ["saved"], queryFn: () => request<Saved[]>("/me/saved") });
  const teams = useQuery({
    queryKey: ["teams", "mine"],
    queryFn: () =>
      request<{ team: { id: number; title: string; status: string } }[]>("/teams?mine=true"),
  });
  return (
    <>
      <Panel title="관심과 경험">
        <ErrorMessage error={q.error} />
        {q.data && (
          <form
            key={JSON.stringify(q.data)}
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setSaved(false);
              setError(undefined);
              try {
                await request(
                  "/me/interests",
                  "PUT",
                  Object.fromEntries(new FormData(e.currentTarget)),
                );
                await client.invalidateQueries({ queryKey: ["interests"] });
                await client.invalidateQueries({ queryKey: ["recommendations"] });
                setSaved(true);
              } catch (e) {
                setError(e);
              } finally {
                setBusy(false);
              }
            }}
          >
            {[
              ["interests", "관심 분야 · 쉼표로 구분", 2000],
              ["activities", "해보고 싶은 활동 · 쉼표로 구분", 2000],
              ["courses", "수강 과목", 4000],
              ["skills", "할 줄 아는 것", 4000],
              ["portfolio", "포트폴리오와 경험", 10000],
            ].map(([name, label, max]) => (
              <Field key={String(name)} label={String(label)}>
                <textarea
                  name={String(name)}
                  maxLength={Number(max)}
                  defaultValue={q.data?.[name as keyof Interests] || ""}
                />
              </Field>
            ))}
            <ErrorMessage error={error} />
            {saved && <p role="status">관심 정보를 저장했어요.</p>}
            <button className="primary" disabled={busy}>
              관심 정보 저장
            </button>
          </form>
        )}
      </Panel>
      <Recommendations />
      <Panel title="저장한 활동">
        <ErrorMessage error={bookmarks.error} />
        {!bookmarks.isPending && !bookmarks.error && !bookmarks.data?.length && (
          <Empty>아직 저장한 활동이 없어요.</Empty>
        )}
        {bookmarks.data?.map((s) => (
          <div className="list-row" key={s.type + s.targetId}>
            <Link to={s.path}>{s.title}</Link>
            <SaveButton type={s.type} id={s.targetId} />
          </div>
        ))}
      </Panel>
      <Panel title="내 팀과 활동 기록">
        <ErrorMessage error={teams.error} />
        {teams.data?.map((v) => (
          <Link className="list-row" key={v.team.id} to={"/teams/" + v.team.id}>
            <strong>{v.team.title}</strong>
            <span className="badge">{v.team.status === "COMPLETED" ? "완료 기록" : "참여 중"}</span>
          </Link>
        ))}
        <Link to="/rentals">내 대여 이력 보기</Link>
      </Panel>
    </>
  );
}

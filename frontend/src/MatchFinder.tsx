import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request } from "./api";
import { Empty, ErrorMessage } from "./ui";
import { SaveButton } from "./PersonalPanel";
type Match = {
  type: string;
  id: number;
  title: string;
  path: string;
  reason: string;
  score: number;
};
export default function MatchFinder() {
  const [search, setSearch] = useState("");
  const [type, setType] = useState("ALL");
  const [matchedOnly, setMatchedOnly] = useState(false);
  const q = useQuery({
    queryKey: ["recommendations"],
    queryFn: () => request<Match[]>("/me/recommendations"),
  });
  const matches =
    q.data?.filter(
      (r) =>
        (type === "ALL" || r.type === type) &&
        (!matchedOnly || r.score > 0) &&
        (r.title + " " + r.reason).toLocaleLowerCase().includes(search.trim().toLocaleLowerCase()),
    ) || [];
  return (
    <section>
      <div className="card match-intro">
        <span className="eyebrow">AI FIND · 관심 기반 탐색</span>
        <h2>내 관심에서 시작하는 다음 활동</h2>
        <p>
          관심 분야, 해보고 싶은 활동, 수강 경험과 보유 기술을 공고와 비교해요. 일치하는 키워드가
          많은 활동부터 보여드려요.
        </p>
        <p className="muted">
          AI나 유료 API를 사용하지 않아요. 추천 이유를 직접 확인하고 선택하세요.
        </p>
        <Link className="text-button" to="/profile">
          관심과 경험 수정 →
        </Link>
      </div>
      <div className="toolbar">
        <input
          aria-label="맞춤 활동 검색"
          placeholder="활동 이름이나 관심 키워드 검색"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
        />
        <select aria-label="맞춤 활동 유형" value={type} onChange={(e) => setType(e.target.value)}>
          <option value="ALL">전체 유형</option>
          <option value="PROGRAM">프로그램</option>
          <option value="TEAM">팀 모집</option>
        </select>
        <label>
          <input
            type="checkbox"
            checked={matchedOnly}
            onChange={(e) => setMatchedOnly(e.target.checked)}
          />{" "}
          관심 일치만
        </label>
        {(search || type !== "ALL" || matchedOnly) && (
          <button
            className="secondary"
            onClick={() => {
              setSearch("");
              setType("ALL");
              setMatchedOnly(false);
            }}
          >
            필터 초기화
          </button>
        )}
      </div>
      <ErrorMessage error={q.error} />
      {q.error && (
        <button className="secondary" onClick={() => q.refetch()}>
          다시 불러오기
        </button>
      )}
      {q.isPending ? (
        <Empty>모집 중인 활동을 살펴보고 있어요…</Empty>
      ) : (
        !q.error && (
          <p className="meta" role="status">
            {matches.length}개의 활동 · 관심 키워드 일치 순
          </p>
        )
      )}
      {!q.isPending && !q.error && !matches.length && (
        <Empty>
          조건에 맞는 활동이 없어요. 필터를 바꾸거나 프로필에 관심 키워드를 추가해 보세요.
        </Empty>
      )}
      <div className="grid">
        {matches.map((r) => (
          <article className="card opportunity-card" key={r.type + r.id}>
            <div className="badges">
              <span className="chip">{r.type === "TEAM" ? "팀 모집" : "프로그램"}</span>
              <span className="badge">{r.score ? `키워드 ${r.score}개 일치` : "둘러보기"}</span>
            </div>
            <h3>
              <Link to={r.path}>{r.title}</Link>
            </h3>
            <p className="muted">{r.reason}</p>
            <div className="toolbar">
              <Link className="text-button" to={r.path}>
                자세히 보기 →
              </Link>
              <SaveButton type={r.type} id={r.id} />
            </div>
          </article>
        ))}
      </div>
    </section>
  );
}

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request, type Organization } from "./api";
import { Empty, ErrorMessage, Field } from "./ui";
import { Users } from "./Icons";
export default function ClubList() {
  const [search, setSearch] = useState("");
  const q = useQuery({
    queryKey: ["organizations"],
    queryFn: () => request<Organization[]>("/organizations"),
  });
  const clubs = q.data?.filter(
    (o) =>
      o.type === "CLUB" &&
      [o.name, o.department, o.description].some((v) =>
        (v || "").toLowerCase().includes(search.trim().toLowerCase()),
      ),
  );
  return (
    <>
      <div className="section-title">
        <h2>캠퍼스 동아리</h2>
        <Link className="secondary link-button" to="/organization">
          동아리 만들기
        </Link>
      </div>
      <Field label="동아리 검색">
        <input
          type="search"
          maxLength={200}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="동아리 이름 · 소개"
        />
      </Field>
      {search && (
        <button className="text-button" onClick={() => setSearch("")}>
          검색 초기화
        </button>
      )}
      {clubs && (
        <p className="count-label" role="status">
          동아리 {clubs.length}개
        </p>
      )}
      <ErrorMessage error={q.error} />
      {q.isPending ? (
        <p>동아리를 불러오고 있어요…</p>
      ) : clubs?.length ? (
        <div className="grid">
          {clubs.map((o) => (
            <Link
              key={o.id}
              className="card card-link opportunity-card"
              to={"/organization?org=" + o.id + "&from=clubs"}
            >
              <div className="card-topline">
                <span className="card-accent club-accent">
                  <Users size={23} />
                </span>
                <span className="badge">{o.role ? "내 동아리" : "동아리"}</span>
              </div>
              <h3>{o.name}</h3>
              <p className="muted card-excerpt">{o.description || "함께할 구성원을 기다려요."}</p>
              <div className="card-footer">
                <span className="meta">{o.department || "캠퍼스 구성원 누구나"}</span>
                <span className="meta">자세히 →</span>
              </div>
            </Link>
          ))}
        </div>
      ) : (
        !q.error && (
          <Empty>
            {search
              ? "검색 결과가 없어요. 동아리 이름이나 관심 분야로 다시 찾아보세요."
              : "아직 등록된 동아리가 없어요. 새로운 동아리를 만들어 보세요."}
          </Empty>
        )
      )}
    </>
  );
}

import { useState } from "react";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request, type Organization } from "./api";
import { Empty, ErrorMessage, Field } from "./ui";
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
      <ErrorMessage error={q.error} />
      {q.isPending ? (
        <p>동아리를 불러오고 있어요…</p>
      ) : clubs?.length ? (
        <div className="grid">
          {clubs.map((o) => (
            <Link key={o.id} className="card card-link" to={"/organization?org=" + o.id}>
              <span className="badge">{o.role ? "내 동아리" : "동아리"}</span>
              <h3>{o.name}</h3>
              <p className="muted">{o.description || "함께할 구성원을 기다려요."}</p>
              <span className="meta">소개·가입 신청 보기 →</span>
            </Link>
          ))}
        </div>
      ) : (
        !q.error && (
          <Empty>조건에 맞는 동아리가 없어요. 소속에서 새로운 동아리를 만들 수 있어요.</Empty>
        )
      )}
    </>
  );
}

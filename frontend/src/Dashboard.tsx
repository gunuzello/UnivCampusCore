import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request, type Organization, type Profile } from "./api";
import type { Activity } from "./ActivityPages";
import { Empty, ErrorMessage, Status, date, Panel } from "./ui";
export default function Dashboard({ user, org }: { user: Profile; org?: Organization }) {
  const events = useQuery({
    queryKey: ["events", org?.id],
    queryFn: () => request<Activity[]>("/organizations/" + org!.id + "/events"),
    enabled: !!org,
  });
  const recruits = useQuery({
    queryKey: ["recruitments", org?.id],
    queryFn: () => request<Activity[]>("/organizations/" + org!.id + "/recruitments"),
    enabled: !!org,
  });
  const today = new Date();
  const from = new Date(today.getFullYear(), today.getMonth(), today.getDate()).toISOString();
  const to = new Date(today.getFullYear(), today.getMonth(), today.getDate() + 7).toISOString();
  const calendar = useQuery({
    queryKey: ["calendar", org?.id, "home", from],
    queryFn: () =>
      request<{ key: string; title: string; startsAt: string; path: string }[]>(
        "/organizations/" +
          org!.id +
          "/calendar?from=" +
          encodeURIComponent(from) +
          "&to=" +
          encodeURIComponent(to),
      ),
    enabled: !!org,
  });
  return (
    <>
      <div className="page-heading">
        <span className="eyebrow">{user.department || "나의 캠퍼스"}</span>
        <h1>{user.name}님, 반가워요.</h1>
        <p>우리 학생회의 오늘을 확인해요.</p>
      </div>
      {!org ? (
        <Panel title="우리 학생회를 시작해요">
          <Empty>첫 학생회를 만들고 캠퍼스의 이야기를 이어가세요.</Empty>
          <Link className="primary link-button" to="/organization">
            학생회 만들기
          </Link>
        </Panel>
      ) : (
        <>
          <div className="grid">
            <Panel title="우리 학생회">
              <h2>{org.name}</h2>
              <Link className="meta" to="/organization">
                소속에서 보기 →
              </Link>
            </Panel>
            <Panel title="진행 중인 행사">
              <span className="stat">
                {events.data?.filter((e) => e.status === "PUBLISHED").length || 0}
              </span>
              <p className="meta">함께할 수 있는 활동</p>
            </Panel>
            <Panel title="열린 모집">
              <span className="stat">
                {recruits.data?.filter((e) => e.status === "PUBLISHED").length || 0}
              </span>
              <p className="meta">다음 구성원을 기다려요</p>
            </Panel>
          </div>
          <Panel title="이번 주 일정">
            <ErrorMessage error={calendar.error} />
            {!calendar.data?.length && <Empty>이번 주에 예정된 일정이 없어요.</Empty>}
            {calendar.data?.slice(0, 5).map((item) => (
              <Link key={item.key} to={item.path} className="list-row">
                <strong>{item.title}</strong>
                <span className="meta">{date(item.startsAt)}</span>
              </Link>
            ))}
            <Link to="/calendar" className="meta">
              캘린더 전체 보기 →
            </Link>
          </Panel>
          {[
            ["events", "학생회 행사", events],
            ["recruitments", "신입부원 모집", recruits],
          ].map(([kind, title, q]) => {
            const query = q as typeof events;
            return (
              <section key={kind as string}>
                <div className="section-title">
                  <h2>{title as string}</h2>
                  <Link className="meta" to={"/" + kind}>
                    전체 보기 →
                  </Link>
                </div>
                <ErrorMessage error={query.error} />
                {!query.data?.length ? (
                  <Empty>아직 등록한 활동이 없어요.</Empty>
                ) : (
                  <div className="grid">
                    {query.data.slice(0, 3).map((a) => (
                      <Link className="card card-link" to={"/" + kind + "/" + a.id} key={a.id}>
                        <Status value={a.status} />
                        <h3 style={{ marginTop: 16 }}>{a.title}</h3>
                        <p className="meta">신청 마감 · {date(a.closesAt)}</p>
                        <strong>{a.applicationCount}명 참여</strong>
                      </Link>
                    ))}
                  </div>
                )}
              </section>
            );
          })}
        </>
      )}
      <div className="section-title">
        <h2>나한테 맞는 기회</h2>
        <span className="badge">AI FIND · 준비 중</span>
      </div>
      <Panel title="캠퍼스의 더 많은 가능성">
        <p className="muted">프로그램 추천, 팀 구하기, 동아리 탐색은 다음 버전에서 만나요.</p>
        <Link className="meta" to="/discover">
          찾기에서 보기 →
        </Link>
      </Panel>
    </>
  );
}

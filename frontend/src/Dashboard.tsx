import { Recommendations } from "./PersonalPanel";
import { useQuery } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request, type Organization, type Profile } from "./api";
import { CalendarDays, Compass, Users } from "./Icons";
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
        "/me/calendar?" +
          (org ? "org=" + org.id + "&" : "") +
          "from=" +
          encodeURIComponent(from) +
          "&to=" +
          encodeURIComponent(to),
      ),
  });
  return (
    <>
      <div className="page-heading home-heading">
        <span className="eyebrow">
          {new Date().toLocaleDateString("ko-KR", {
            month: "long",
            day: "numeric",
            weekday: "long",
          })}{" "}
          · {user.department || "나의 캠퍼스"}
        </span>
        <h1>{user.name}님, 반가워요.</h1>
        <p>캠퍼스의 오늘과 함께할 기회를 확인해요.</p>
      </div>
      <section className="campus-hero">
        <div>
          <span className="eyebrow">MAKE YOUR CAMPUS</span>
          <h2>
            나의 다음 활동,
            <br />
            여기서 시작해요.
          </h2>
          <p>혼자 떠올린 아이디어가 함께하는 경험이 되도록.</p>
          <Link className="primary link-button" to="/discover?tab=programs">
            새로운 기회 둘러보기 <span aria-hidden="true">↗</span>
          </Link>
        </div>
        <div className="hero-art" aria-hidden="true">
          <div className="hero-orbit" />
          <div className="hero-note">
            <CalendarDays size={30} />
            <span>
              이번 학기도,
              <br />
              함께.
            </span>
          </div>
          <div className="hero-dot" />
          <div className="hero-spark">✳</div>
        </div>
      </section>
      <nav className="quick-links" aria-label="캠퍼스 바로가기">
        <Link to="/discover?tab=teams">
          <Users size={20} />
          <span>함께할 팀 찾기</span>
          <span aria-hidden="true">↗</span>
        </Link>
        <Link to="/discover?tab=clubs">
          <Compass size={20} />
          <span>내 취향의 동아리</span>
          <span aria-hidden="true">↗</span>
        </Link>
        <Link to="/calendar">
          <CalendarDays size={20} />
          <span>이번 주 일정</span>
          <span aria-hidden="true">↗</span>
        </Link>
      </nav>
      {!org ? (
        <Panel title="우리 소속을 시작해요">
          <Empty>첫 소속을 만들고 캠퍼스의 이야기를 이어가세요.</Empty>
          <Link className="primary link-button" to="/organization">
            소속 만들기
          </Link>
        </Panel>
      ) : (
        <>
          <div className="grid overview-grid">
            <Panel title="우리 소속">
              <h2>{org.name}</h2>
              <Link className="meta" to="/organization">
                소속에서 보기 →
              </Link>
            </Panel>
            <Panel title="진행 중인 행사">
              <span className="stat">
                {events.data?.filter(
                  (e) => e.status === "PUBLISHED" && Date.parse(e.closesAt) > Date.now(),
                ).length ?? "—"}
              </span>
              <p className="meta">함께할 수 있는 활동</p>
            </Panel>
            <Panel title="열린 모집">
              <span className="stat">
                {recruits.data?.filter(
                  (e) => e.status === "PUBLISHED" && Date.parse(e.closesAt) > Date.now(),
                ).length ?? "—"}
              </span>
              <p className="meta">다음 구성원을 기다려요</p>
            </Panel>
          </div>
          <div className="dashboard-spacer" />
          <Panel title="이번 주 일정">
            <ErrorMessage error={calendar.error} />
            {!calendar.isPending && !calendar.error && !calendar.data?.length && (
              <Empty>이번 주에 예정된 일정이 없어요.</Empty>
            )}
            {calendar.isPending && <Empty>이번 주 일정을 불러오고 있어요…</Empty>}
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
            ["events", "소속 행사", events],
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
                {query.isPending ? (
                  <Empty>활동을 불러오고 있어요…</Empty>
                ) : !query.error && !query.data?.length ? (
                  <Empty>아직 등록한 활동이 없어요.</Empty>
                ) : (
                  <div className="grid">
                    {query.data
                      ?.filter((a) => !["CANCELLED", "COMPLETED"].includes(a.status))
                      .slice(0, 3)
                      .map((a) => (
                        <Link
                          className="card card-link opportunity-card"
                          to={"/" + kind + "/" + a.id}
                          key={a.id}
                        >
                          <div
                            className={
                              "card-accent " + (kind === "events" ? "event-accent" : "team-accent")
                            }
                            aria-hidden="true"
                          >
                            {kind === "events" ? <CalendarDays size={27} /> : <Users size={27} />}
                          </div>
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
      <Recommendations />
      <Panel title="캠퍼스의 더 많은 가능성">
        <p className="muted">프로그램을 찾고, 팀을 모으고, 동아리에 가입해 보세요.</p>
        <Link className="meta" to="/discover">
          찾기에서 보기 →
        </Link>
      </Panel>
    </>
  );
}

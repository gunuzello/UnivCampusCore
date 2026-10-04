import { useEffect, useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { NavLink, Routes, Route, Navigate, useLocation, useNavigate } from "react-router-dom";
import { Home, Compass, CalendarDays, Users, UserRound } from "./Icons";
import { request, ApiError, type Profile, type Organization } from "./api";
import AuthPage from "./AuthPage";
import OrganizationPage from "./OrganizationPage";
import ProfilePage from "./ProfilePage";
import { Panel, Empty, ErrorMessage, Action } from "./ui";
import { ActivityList, ActivityCreate, ActivityDetail } from "./ActivityPages";
import Dashboard from "./Dashboard";
import CalendarPage from "./CalendarPage";
import ScheduleDetail from "./ScheduleDetail";
import ArchivePage from "./ArchivePage";
import { MeetingList, MeetingDetail } from "./MeetingPages";
import NotificationsPage from "./NotificationsPage";
import RentalPage from "./RentalPage";
import DiscoverPage from "./DiscoverPage";
export default function App() {
  const query = useQueryClient();
  const [selected, setSelected] = useState<number>();
  const location = useLocation();
  const navigate = useNavigate();
  useEffect(() => {
    const value = new URLSearchParams(location.search).get("org");
    if (value && /^\d+$/.test(value) && Number(value) > 0) setSelected(Number(value));
  }, [location.search]);
  const me = useQuery({ queryKey: ["me"], queryFn: () => request<Profile>("/me") });
  const organizations = useQuery({
    queryKey: ["organizations"],
    queryFn: () => request<Organization[]>("/organizations"),
    enabled: !!me.data,
  });
  if (me.isPending) return <main className="loading">UCC를 불러오고 있어요…</main>;
  if (me.error) {
    if (me.error instanceof ApiError && me.error.status === 401) return <AuthPage />;
    return (
      <main>
        <ErrorMessage error={me.error} />
        <Action label="다시 시도" onAction={() => me.refetch()} />
      </main>
    );
  }
  const user = me.data!;
  const org = organizations.data?.find((o) => o.id === selected) || organizations.data?.[0];
  return (
    <div className="shell">
      <aside>
        <NavLink to="/" className="brand">
          <img src="/ucc-logo.png" alt="" />
          <div>
            <strong>UCC</strong>
            <small>유크크</small>
          </div>
        </NavLink>
        <p className="eyebrow">캠퍼스의 이야기를 잇다</p>
        <nav>
          {[
            ["/", "홈", Home],
            ["/discover", "찾기", Compass],
            ["/calendar", "캘린더", CalendarDays],
            ["/organization", "소속", Users],
            ["/events", "행사", Compass],
            ["/recruitments", "모집", Users],
            ["/rentals", "대여사업", Users],
            ["/meetings", "회의", Users],
            ["/archive", "지난 활동", CalendarDays],
          ].map(([to, label, Icon]) => {
            if (["/meetings", "/archive"].includes(to as string) && !org?.role) return null;
            const I = Icon as typeof Home;
            return (
              <NavLink
                key={to as string}
                to={to as string}
                end
                className={
                  ["/", "/discover", "/calendar", "/organization"].includes(to as string)
                    ? undefined
                    : "secondary-nav"
                }
              >
                <I size={19} />
                {label as string}
              </NavLink>
            );
          })}
        </nav>
        <div className="aside-bottom">
          <div className="list-row">
            <span className="avatar small">{user.name[0]}</span>
            <div>
              <strong>{user.name}</strong>
              <small>{user.department || "학과 미등록"}</small>
            </div>
          </div>
          <Action
            label="로그아웃"
            onAction={async () => {
              await request("/auth/logout", "POST");
              query.clear();
              await me.refetch();
            }}
          />
        </div>
      </aside>
      <div className="workspace">
        <header>
          <div>
            <span className="eyebrow">UNIVERSITY CAMPUS CORE</span>
            <p>함께 만들고, 다음으로 이어가요.</p>
          </div>
          <select
            aria-label="소속 선택"
            value={org?.id || ""}
            onChange={(e) => {
              const id = Number(e.target.value);
              setSelected(id);
              if (location.pathname === "/organization") navigate("/organization?org=" + id);
            }}
          >
            <option value="" disabled>
              소속 선택
            </option>
            {organizations.data?.map((o) => (
              <option key={o.id} value={o.id}>
                {o.name}
              </option>
            ))}
          </select>
          <NavLink to="/notifications" className="header-action" aria-label="알림 보기">
            알림
          </NavLink>
          <NavLink to="/profile" className="avatar small" aria-label="내 프로필">
            {user.name[0]}
          </NavLink>
        </header>
        <main>
          <ErrorMessage error={organizations.error} />
          <Routes>
            <Route path="/" element={<Dashboard user={user} org={org} />} />
            {(["events", "recruitments"] as const).map((kind) => (
              <Route
                key={kind}
                path={"/" + kind}
                element={<ActivityList kind={kind} org={org} />}
              />
            ))}
            {(["events", "recruitments"] as const).map((kind) => (
              <Route
                key={kind}
                path={"/" + kind + "/new"}
                element={<ActivityCreate kind={kind} org={org} />}
              />
            ))}
            {(["events", "recruitments"] as const).map((kind) => (
              <Route
                key={kind}
                path={"/" + kind + "/:id"}
                element={<ActivityDetail kind={kind} user={user} />}
              />
            ))}
            <Route
              path="/organization"
              element={<OrganizationPage key={org?.id} org={org} onSelect={setSelected} />}
            />
            <Route path="/rentals" element={<RentalPage key={org?.id} org={org} />} />
            <Route path="/profile" element={<ProfilePage user={user} />} />
            <Route path="/discover" element={<DiscoverPage />} />
            <Route path="/calendar" element={<CalendarPage key={org?.id} org={org} />} />
            <Route path="/meetings" element={<MeetingList key={org?.id} org={org} />} />
            <Route path="/schedules/:id" element={<ScheduleDetail />} />
            <Route path="/meetings/:id" element={<MeetingDetail />} />
            <Route path="/archive" element={<ArchivePage key={org?.id} org={org} />} />
            <Route path="/notifications" element={<NotificationsPage />} />
            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>
      </div>
    </div>
  );
}

import PersonalPanel from "./PersonalPanel";
import { ThemeSettings } from "./Theme";
import { Link } from "react-router-dom";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Profile } from "./api";
import { Panel, Field, ErrorMessage, Empty, Status, date, Action } from "./ui";
export default function ProfilePage({ user }: { user: Profile }) {
  const query = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const applications = useQuery({
    queryKey: ["my-applications"],
    queryFn: () =>
      request<
        {
          key: string;
          type: string;
          title: string;
          status: string;
          path: string;
          submittedAt: string;
        }[]
      >("/me/applications"),
  });
  return (
    <>
      <div className="page-heading">
        <h1>내 프로필</h1>
        <p>기본 정보와 관심 분야, 캠퍼스 활동을 한곳에서 관리해요.</p>
      </div>
      <Panel title="내 기본 정보">
        <p className="muted">{user.email}</p>
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            if (busy) return;
            setBusy(true);
            setError(undefined);
            setSaved(false);
            try {
              const values = Object.fromEntries(new FormData(e.currentTarget));
              if (!String(values.name).trim()) throw new Error("이름을 입력해 주세요.");
              await request("/me", "PATCH", values);
              await query.invalidateQueries({ queryKey: ["me"] });
              setSaved(true);
            } catch (e) {
              setError(e);
            } finally {
              setBusy(false);
            }
          }}
        >
          <Field label="이름">
            <input name="name" defaultValue={user.name} required maxLength={80} />
          </Field>
          <Field label="학과">
            <input name="department" defaultValue={user.department || ""} maxLength={120} />
          </Field>
          <Field label="학번">
            <input name="studentNumber" defaultValue={user.studentNumber || ""} maxLength={40} />
          </Field>
          <ErrorMessage error={error} />
          {saved && <p role="status">저장했어요.</p>}
          <button className="primary" disabled={busy}>
            {busy ? "저장 중…" : "프로필 저장"}
          </button>
        </form>
      </Panel>
      <Panel title="계정">
        <Action
          label="로그아웃"
          onAction={async () => {
            await request("/auth/logout", "POST");
            query.removeQueries({ predicate: (q) => q.queryKey[0] !== "me" });
            await query.resetQueries({ queryKey: ["me"] });
          }}
        />
      </Panel>
      <ThemeSettings />
      <PersonalPanel />
      <Panel title="내 신청과 지원">
        <ErrorMessage error={applications.error} />
        {applications.isPending && <Empty>신청 기록을 불러오고 있어요…</Empty>}
        {!applications.isPending && !applications.error && !applications.data?.length && (
          <Empty>아직 신청한 행사나 모집이 없어요. 관심 있는 활동을 찾아 참여해 보세요.</Empty>
        )}
        {applications.data?.map((a) => (
          <Link to={a.path} className="list-row" key={a.key}>
            <div>
              <strong>{a.title}</strong>
              <p className="meta">
                {a.type === "EVENT" ? "행사 신청" : "모집 지원"} · {date(a.submittedAt)}
              </p>
            </div>
            <Status value={a.status} />
          </Link>
        ))}
      </Panel>
    </>
  );
}

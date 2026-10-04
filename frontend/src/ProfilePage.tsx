import { Link } from "react-router-dom";
import { useState } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Profile } from "./api";
import { Panel, Field, ErrorMessage, Empty, Status, date } from "./ui";
export default function ProfilePage({ user }: { user: Profile }) {
  const query = useQueryClient();
  const [error, setError] = useState<unknown>();
  const [saved, setSaved] = useState(false);
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
      <Panel title="내 기본 정보">
        <p className="muted">{user.email}</p>
        <form
          onSubmit={async (e) => {
            e.preventDefault();
            setError(undefined);
            setSaved(false);
            try {
              await request("/me", "PATCH", Object.fromEntries(new FormData(e.currentTarget)));
              await query.invalidateQueries({ queryKey: ["me"] });
              setSaved(true);
            } catch (e) {
              setError(e);
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
          <button className="primary">프로필 저장</button>
        </form>
      </Panel>
      <Panel title="내 신청과 지원">
        <ErrorMessage error={applications.error} />
        {!applications.data?.length && <Empty>아직 신청한 행사나 모집이 없어요.</Empty>}
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

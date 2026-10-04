import MembershipRequestsPanel from "./MembershipRequestsPanel";
import { Link, useNavigate } from "react-router-dom";
import { useState, type FormEvent } from "react";
import { useQuery, useQueryClient } from "@tanstack/react-query";
import { request, type Organization, type Profile } from "./api";
import { Panel, Field, ErrorMessage, Empty, Action, Status } from "./ui";
type Member = { id: number; user: Profile; role: string };
export default function OrganizationPage({
  org,
  onSelect,
}: {
  org?: Organization;
  onSelect: (id: number) => void;
}) {
  const query = useQueryClient();
  const navigate = useNavigate();
  const [error, setError] = useState<unknown>();
  const [busy, setBusy] = useState(false);
  const members = useQuery({
    queryKey: ["members", org?.id],
    queryFn: () => request<Member[]>("/organizations/" + org!.id + "/members"),
    enabled: !!org?.role,
  });
  async function create(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const d = Object.fromEntries(new FormData(e.currentTarget));
    setBusy(true);
    setError(undefined);
    try {
      const o = await request<Organization>("/organizations", "POST", d);
      await query.invalidateQueries({ queryKey: ["organizations"] });
      onSelect(o.id);
      navigate("/organization?org=" + o.id);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  async function add(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    const form = e.currentTarget;
    setBusy(true);
    setError(undefined);
    try {
      await request(
        "/organizations/" + org!.id + "/members",
        "POST",
        Object.fromEntries(new FormData(form)),
      );
      await query.invalidateQueries({ queryKey: ["members"] });
      form.reset();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <div className="columns">
      <div>
        <Panel title={org?.type === "CLUB" ? "내 동아리" : "우리 학생회"}>
          {org ? (
            <>
              <div className="toolbar">
                <Link className="secondary link-button" to="/events">
                  행사
                </Link>
                <Link className="secondary link-button" to="/recruitments">
                  모집
                </Link>
                <Link className="secondary link-button" to="/rentals">
                  대여사업
                </Link>
                {org.role && (
                  <>
                    <Link className="secondary link-button" to="/meetings">
                      회의
                    </Link>
                    <Link className="secondary link-button" to="/archive">
                      지난 활동
                    </Link>
                  </>
                )}
              </div>
              <div className="org-banner">
                <div className="avatar">{org.name[0]}</div>
                <div>
                  <h3>{org.name}</h3>
                  <p className="muted">{org.department}</p>
                  {org.role ? (
                    <Status value={org.role} />
                  ) : (
                    <span className="badge">일반 학생</span>
                  )}
                </div>
              </div>
              <p className="prewrap">{org.description || "학생회의 이야기를 시작해 보세요."}</p>
              {["STAFF", "LEADER"].includes(org.role || "") && (
                <form
                  onSubmit={async (e) => {
                    e.preventDefault();
                    setError(undefined);
                    try {
                      await request(
                        "/organizations/" + org.id,
                        "PATCH",
                        Object.fromEntries(new FormData(e.currentTarget)),
                      );
                      await query.invalidateQueries({ queryKey: ["organizations"] });
                    } catch (e) {
                      setError(e);
                    }
                  }}
                >
                  <Field label="조직명">
                    <input name="name" defaultValue={org.name} required />
                  </Field>
                  <Field label="학과">
                    <input name="department" defaultValue={org.department} />
                  </Field>
                  <Field label="소개">
                    <textarea name="description" defaultValue={org.description} />
                  </Field>
                  <button className="secondary">조직 정보 저장</button>
                </form>
              )}
            </>
          ) : (
            <Empty>학생회를 만들거나 상단에서 선택해 주세요.</Empty>
          )}
        </Panel>
        {org?.role && (
          <Panel title="함께하는 구성원">
            <ErrorMessage error={members.error} />
            {members.data?.map((m) => (
              <div className="list-row" key={m.id}>
                <div>
                  <strong>{m.user.name}</strong>
                  <p className="muted">{m.user.email}</p>
                </div>
                <Status value={m.role} />
                {org.role === "LEADER" && (
                  <>
                    <select
                      aria-label={m.user.name + " 역할"}
                      value={m.role}
                      onChange={async (e) => {
                        try {
                          await request("/organizations/" + org.id + "/members/" + m.id, "PATCH", {
                            role: e.target.value,
                          });
                          await query.invalidateQueries({ queryKey: ["members"] });
                          await query.invalidateQueries({ queryKey: ["organizations"] });
                        } catch (e) {
                          setError(e);
                        }
                      }}
                    >
                      <option value="MEMBER">구성원</option>
                      <option value="STAFF">운영진</option>
                      <option value="LEADER">대표</option>
                    </select>
                    <Action
                      label="제거"
                      danger
                      onAction={async () => {
                        if (!confirm(m.user.name + "님을 조직에서 제거할까요?")) return;
                        await request("/organizations/" + org.id + "/members/" + m.id, "DELETE");
                        await query.invalidateQueries({ queryKey: ["members"] });
                        await query.invalidateQueries({ queryKey: ["organizations"] });
                      }}
                    />
                  </>
                )}
              </div>
            ))}
            {org.role === "LEADER" && (
              <form onSubmit={add}>
                <h3>구성원 추가</h3>
                <Field label="가입한 사용자의 이메일">
                  <input type="email" name="email" required />
                </Field>
                <Field label="조직 역할">
                  <select name="role">
                    <option value="MEMBER">구성원</option>
                    <option value="STAFF">운영진</option>
                    <option value="LEADER">대표</option>
                  </select>
                </Field>
                <button className="primary" disabled={busy}>
                  구성원 추가
                </button>
              </form>
            )}
          </Panel>
        )}
      </div>
      <Panel title="새 소속 시작하기">
        <p className="muted">학생회나 동아리를 만들면 첫 대표가 됩니다.</p>
        <form onSubmit={create}>
          <Field label="소속 유형">
            <select name="type" defaultValue="STUDENT_COUNCIL">
              <option value="STUDENT_COUNCIL">학생회</option>
              <option value="CLUB">동아리</option>
            </select>
          </Field>
          <Field label="소속 이름">
            <input name="name" required maxLength={120} placeholder="제16대 ITM 학생회" />
          </Field>
          <Field label="학과">
            <input name="department" maxLength={120} />
          </Field>
          <Field label="소개">
            <textarea name="description" maxLength={10000} />
          </Field>
          <button className="primary" disabled={busy}>
            소속 만들기
          </button>
        </form>
      </Panel>
      {org && <MembershipRequestsPanel org={org} />}
      <ErrorMessage error={error} />
    </div>
  );
}

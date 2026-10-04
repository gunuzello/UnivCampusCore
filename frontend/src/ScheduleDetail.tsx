import { useQuery } from "@tanstack/react-query";
import { Link, useParams } from "react-router-dom";
import { request } from "./api";
import { Panel, ErrorMessage, date } from "./ui";
import LinksPanel from "./LinksPanel";
type Detail = {
  organizationId: number;
  schedule: {
    id: number;
    title: string;
    startsAt: string;
    endsAt: string;
    description: string;
    canManage: boolean;
  };
};
export default function ScheduleDetail() {
  const { id } = useParams();
  const q = useQuery({
    queryKey: ["schedule", id],
    queryFn: () => request<Detail>("/schedules/" + id),
  });
  if (q.error) return <ErrorMessage error={q.error} />;
  if (!q.data) return <p>일정을 불러오고 있어요…</p>;
  const { schedule: s, organizationId } = q.data;
  return (
    <>
      <div className="page-heading">
        <h1>{s.title}</h1>
        <p>학생회 내부 일정</p>
      </div>
      <Panel title="일정 정보">
        <p>
          {date(s.startsAt)} ~ {date(s.endsAt)}
        </p>
        <p style={{ whiteSpace: "pre-wrap" }}>{s.description || "등록된 설명이 없어요."}</p>
        <Link className="secondary" to="/calendar">
          캘린더로 돌아가기
        </Link>
        {s.canManage && (
          <p className="muted">캘린더의 일정 수정 버튼에서 내용을 변경할 수 있어요.</p>
        )}
      </Panel>
      <LinksPanel orgId={organizationId} type="SCHEDULE" targetId={s.id} manage={s.canManage} />
    </>
  );
}

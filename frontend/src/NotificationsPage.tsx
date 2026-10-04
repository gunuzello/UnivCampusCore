import { useQuery, useQueryClient } from "@tanstack/react-query";
import { Link } from "react-router-dom";
import { request } from "./api";
import { Panel, Empty, ErrorMessage, Action, date } from "./ui";
export type Notification = {
  id: number;
  message: string;
  path: string;
  read: boolean;
  createdAt: string;
};
export default function NotificationsPage() {
  const query = useQueryClient();
  const q = useQuery({
    queryKey: ["notifications"],
    queryFn: () => request<Notification[]>("/notifications"),
  });
  return (
    <Panel title="알림">
      <ErrorMessage error={q.error} />
      {!q.data?.length && <Empty>새 알림이 없어요.</Empty>}
      {q.data?.map((n) => (
        <div className={"list-row notification " + (!n.read ? "unread" : "")} key={n.id}>
          <div>
            <Link to={n.path || "/"}>{n.message}</Link>
            <p className="meta">{date(n.createdAt)}</p>
          </div>
          {!n.read && (
            <Action
              label="읽음"
              onAction={async () => {
                await request("/notifications/" + n.id, "PATCH");
                await query.invalidateQueries({ queryKey: ["notifications"] });
              }}
            />
          )}
        </div>
      ))}
    </Panel>
  );
}

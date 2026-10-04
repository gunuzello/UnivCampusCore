import { useQuery } from "@tanstack/react-query";
import { request } from "./api";
export function useDemoMode() {
  return (
    useQuery({
      queryKey: ["environment"],
      queryFn: () => request<{ demo: boolean }>("/health"),
      staleTime: Infinity,
    }).data?.demo === true
  );
}
export function DemoNotice() {
  const demo = useDemoMode();
  return demo ? (
    <div className="demo-notice">
      <span className="demo-dot" />
      캠퍼스 미리보기 <span>· 가상 계정과 활동으로 구성한 데모예요.</span>
    </div>
  ) : null;
}

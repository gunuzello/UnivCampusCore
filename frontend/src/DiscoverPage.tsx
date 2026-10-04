import { useState } from "react";
import type { Organization } from "./api";
import { ProgramList } from "./ProgramPages";
import { TeamList } from "./TeamPages";
import ClubList from "./ClubList";
export default function DiscoverPage({ org }: { org?: Organization }) {
  const [tab, setTab] = useState("프로그램");
  return (
    <>
      <div className="page-heading">
        <h1>찾기</h1>
        <p>캠퍼스의 기회를 찾고, 함께할 사람을 만나요.</p>
      </div>
      <div className="segmented" role="tablist" aria-label="찾기 유형">
        {["프로그램", "팀 구하기", "동아리"].map((t) => (
          <button
            key={t}
            role="tab"
            aria-selected={t === tab}
            className={t === tab ? "primary" : "secondary"}
            onClick={() => setTab(t)}
          >
            {t}
          </button>
        ))}
      </div>
      {tab === "프로그램" ? (
        <ProgramList key={org?.id} org={org} />
      ) : tab === "팀 구하기" ? (
        <TeamList />
      ) : (
        <ClubList />
      )}
    </>
  );
}

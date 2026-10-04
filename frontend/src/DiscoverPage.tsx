import { ProgramList } from "./ProgramPages";
import type { Organization } from "./api";
import ClubList from "./ClubList";
import { useState } from "react";
import { Panel } from "./ui";
export default function DiscoverPage({ org }: { org?: Organization }) {
  const [tab, setTab] = useState("프로그램");
  const examples: Record<string, { title: string; description: string; tag: string }[]> = {
    프로그램: [
      {
        title: "인터랙션 연구실 학부인턴 모집",
        description: "연구실 프로젝트에 학부생으로 직접 참여해볼 수 있어요.",
        tag: "연구 · AI FIND",
      },
      {
        title: "교내 공모전과 대외활동",
        description: "캠퍼스의 다양한 기회를 모아보는 공간이에요.",
        tag: "프로그램 탐색",
      },
    ],
    "팀 구하기": [
      {
        title: "노원구 공공데이터로 만드는 자취생 생활 지도",
        description: "디자인과 개발을 함께할 팀원을 찾는 화면이에요.",
        tag: "AI CONNECT",
      },
      {
        title: "처음 나가보는 사람끼리 24시간 버텨보기",
        description: "교내 해커톤에 함께 참여할 팀을 구성하는 화면이에요.",
        tag: "팀 탐색",
      },
    ],
    동아리: [
      { title: "SNUTO", description: "합주와 공연을 함께하는 캠퍼스 동아리.", tag: "음악 · 공연" },
      {
        title: "내 동아리",
        description: "동아리 소식과 구성원을 만나는 공간이에요.",
        tag: "동아리 탐색",
      },
    ],
  };
  return (
    <>
      <div className="page-heading">
        <h1>찾기</h1>
        <p>나한테 맞는 캠퍼스의 기회를 찾아요.</p>
      </div>
      <div className="segmented" role="tablist" aria-label="찾기 유형">
        {Object.keys(examples).map((t) => (
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
      ) : tab === "동아리" ? (
        <ClubList />
      ) : (
        <>
          <div className="notice">
            <strong>준비 중인 화면이에요.</strong>
            <p>
              아래 카드는 기존 프로토타입의 예시이며 실제 모집 공고가 아니에요. 학생회 행사와 모집은
              홈 또는 소속에서 이용할 수 있어요.
            </p>
          </div>
          <div className="section-title">
            <h2>
              {tab === "프로그램"
                ? "나한테 맞는 기회"
                : tab === "팀 구하기"
                  ? "나한테 맞는 팀"
                  : "함께할 동아리"}
            </h2>
            <span className="badge">Demo UI</span>
          </div>
          <div className="grid">
            {examples[tab].map((e) => (
              <Panel key={e.title} title={e.title}>
                <span className="badge">{e.tag}</span>
                <p className="muted">{e.description}</p>
                <button disabled className="secondary">
                  준비 중
                </button>
              </Panel>
            ))}
          </div>
        </>
      )}
    </>
  );
}

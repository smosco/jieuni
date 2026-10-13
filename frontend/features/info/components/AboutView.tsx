import Link from "next/link";
import { CONTACT_EMAIL } from "../contact";
import { InfoPage, InfoSection, List, P } from "./InfoPage";
import { NewsletterLink } from "./NewsletterLink";
import styles from "./InfoPage.module.css";

export function AboutView() {
  const mail = <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>;

  return (
    <InfoPage label="ABOUT" title="공간의 지은이">
      <InfoSection title="다녀온 뒤에도 오래 생각나는 공간">
        <P>
          공간의 지은이는 공간에서 감동받는 사람들을 위한 곳이에요. 다녀온 뒤에도 오래 생각나는 공간과, 그 공간을 지은
          사람들을 소개해요. 평일 아침 8시마다 한 곳씩 열려요.
        </P>
        <P>
          공간마다 설계한 건축가, 인테리어와 가구를 만든 디자이너, 주인을 함께 적어요. 마음에 드는 사람을 팔로우하면 그
          사람의 새 공간을 내 공간에서 볼 수 있어요.
        </P>
      </InfoSection>

      <InfoSection title="하지 않는 것">
        <List
          items={[
            "좋아요, 별점, 순위, 인기순은 없어요. 저장 수나 팔로워 수도 보여 주지 않아요.",
            "공간의 감상과 한 줄은 직접 다녀온 사람이 써요. AI가 쓰지 않아요.",
          ]}
        />
      </InfoSection>

      <InfoSection title="사진과 크레딧">
        <P>
          모든 사진 아래에 찍은 사람과 출처를 적어요. 사진을 내려 주기를 원하거나 크레딧이 틀렸다면 {mail}로 알려 주세요.
          확인한 뒤 48시간 안에 내리거나 고칠게요.
        </P>
        <P>
          알고 있는 공간이나 만든 사람을 소개하고 싶다면 로그인한 뒤 내 공간의 &ldquo;공간 소개하기&rdquo;를 눌러 주세요.
        </P>
      </InfoSection>

      <InfoSection title="문의">
        <P>그 밖의 문의도 {mail}로 받아요.</P>
      </InfoSection>

      <div className={styles.end}>
        <hr className={styles.rule} />
        <div className={styles.links}>
          <NewsletterLink className={`label-md ${styles.linkRow}`} />
          <Link href="/terms" className={`label-md ${styles.linkRow}`}>
            이용약관 <span aria-hidden="true">→</span>
          </Link>
          <Link href="/privacy" className={`label-md ${styles.linkRow}`}>
            개인정보처리방침 <span aria-hidden="true">→</span>
          </Link>
        </div>
      </div>
    </InfoPage>
  );
}

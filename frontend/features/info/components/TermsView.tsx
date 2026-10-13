import Link from "next/link";
import { CONTACT_EMAIL, EFFECTIVE_DATE } from "../contact";
import { InfoPage, InfoSection, List, P } from "./InfoPage";

export function TermsView() {
  const mail = <a href={`mailto:${CONTACT_EMAIL}`}>{CONTACT_EMAIL}</a>;

  return (
    <InfoPage label="TERMS" title="이용약관" effectiveDate={EFFECTIVE_DATE}>
      <InfoSection title="1. 목적">
        <P>이 약관은 공간의 지은이(이하 &ldquo;서비스&rdquo;)를 쓰는 데 필요한 서비스와 이용자 사이의 약속을 정해요.</P>
      </InfoSection>

      <InfoSection title="2. 서비스">
        <P>
          서비스는 공간과 그 공간을 만든 사람을 소개하는 무료 웹 서비스예요. 개인이 운영하며, 로그인하지 않아도 모든
          소개를 볼 수 있어요.
        </P>
      </InfoSection>

      <InfoSection title="3. 계정">
        <List
          items={[
            "구글 계정으로 가입하고 로그인해요. 만 14세 이상만 가입할 수 있어요.",
            "계정은 본인만 써야 해요.",
            "내 공간의 “설정”에서 언제든 탈퇴할 수 있어요. 탈퇴하면 저장과 팔로우가 바로 지워져요.",
          ]}
        />
      </InfoSection>

      <InfoSection title="4. 제보">
        <P>
          이용자가 보낸 공간 소개와 크레딧 제보는 운영자가 확인한 뒤 서비스의 소개에 쓸 수 있어요. 쓸지 말지, 어떻게 쓸지는
          운영자가 정해요. 다른 사람의 권리를 침해하거나 사실이 아닌 내용은 보내지 말아 주세요.
        </P>
      </InfoSection>

      <InfoSection title="5. 콘텐츠의 권리">
        <P>
          소개 글의 권리는 서비스에 있어요. 사진의 권리는 사진 아래 적은 찍은 사람과 출처에 있어요. 사진이나 크레딧에 문제가
          있다면 <Link href="/about">소개</Link>에 적은 창구로 알려 주세요.
        </P>
      </InfoSection>

      <InfoSection title="6. 하지 말아야 할 것">
        <List
          items={[
            "다른 사람의 계정이나 정보를 쓰는 것",
            "서비스를 자동으로 대량 수집하거나 정상적인 운영을 방해하는 것",
            "법령이나 다른 사람의 권리를 침해하는 내용을 보내는 것",
          ]}
        />
        <P>이런 일이 있으면 해당 계정의 이용을 제한할 수 있어요.</P>
      </InfoSection>

      <InfoSection title="7. 서비스의 변경과 중단">
        <P>
          서비스의 내용은 바뀌거나 중단될 수 있어요. 미리 알릴 수 있는 경우에는 서비스 안에서 알려요.
        </P>
      </InfoSection>

      <InfoSection title="8. 책임">
        <P>
          영업시간, 방문 방식 같은 공간 정보는 바뀔 수 있어요. 방문하기 전에 공간의 공식 안내를 확인해 주세요. 서비스는 고의나
          중대한 과실이 없는 한 소개된 정보로 생긴 손해에 책임지지 않아요.
        </P>
      </InfoSection>

      <InfoSection title="9. 약관의 변경">
        <P>
          약관을 바꾸면 시행 7일 전에 이 페이지에 알려요. 이 약관은 {EFFECTIVE_DATE}부터 시행해요. 문의는 {mail}로 받아요.
        </P>
      </InfoSection>

      <InfoSection title="10. 준거법">
        <P>이 약관은 대한민국 법에 따라 해석해요.</P>
      </InfoSection>
    </InfoPage>
  );
}

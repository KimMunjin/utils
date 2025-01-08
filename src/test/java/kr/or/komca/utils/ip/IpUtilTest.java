package kr.or.komca.utils.ip;

// Servlet 요청, JUnit 테스트, Mockito 관련 클래스 임포트
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

// JUnit의 Assertions와 Mockito의 when 메서드를 정적 임포트
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

// IpUtil 클래스의 테스트를 위한 테스트 클래스
class IpUtilTest {

    // X-Forwarded-For 헤더를 통한 IP 추출 테스트
    @Test
    void testGetClientIp_withXForwardedFor() {
        // HTTP 요청 객체 모의(Mock) 생성
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // X-Forwarded-For 헤더가 192.168.0.1을 반환하도록 설정
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.0.1");
        // getRemoteAddr이 127.0.0.1을 반환하도록 설정
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // IpUtil.getClientIp 메서드 호출하여 결과 저장
        String clientIp = IpUtil.getClientIp(request);

        // 반환된 IP가 X-Forwarded-For 헤더의 IP와 일치하는지 검증
        assertEquals("192.168.0.1", clientIp);
    }

    // RemoteAddr을 통한 IP 추출 테스트
    @Test
    void testGetClientIp_withRemoteAddr() {
        // HTTP 요청 객체 모의 생성
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // X-Forwarded-For 헤더가 없는 상황 설정
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        // getRemoteAddr이 127.0.0.1을 반환하도록 설정
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // IP 추출 메서드 호출
        String clientIp = IpUtil.getClientIp(request);

        // 반환된 IP가 RemoteAddr의 IP와 일치하는지 검증
        assertEquals("127.0.0.1", clientIp);
    }

    // 여러 IP가 포함된 경우 첫 번째 IP 추출 테스트
    @Test
    void testGetClientIp_withMultipleIps() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        // mock(HttpServletRequest.class)는 기본적으로 모든 메서드 호출에 대해 null을 반환하므로, 헤더 값을 null로 설정할 필요가 없음

        // X-Forwarded-For 헤더 설정
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.0.1, 10.0.0.1");

        // RemoteAddr 설정
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        // getClientIp 호출 및 결과 확인
        String clientIp = IpUtil.getClientIp(request);

        // 검증
        assertEquals("192.168.0.1", clientIp);

        // Mock이 호출되었는지 검증
        Mockito.verify(request, Mockito.atLeastOnce()).getHeader("X-Forwarded-For");
    }

    // null 요청에 대한 처리 테스트
    @Test
    void testGetClientIp_withNullRequest() {
        // null 요청으로 메서드 호출
        String clientIp = IpUtil.getClientIp(null);
        // 기본 IP(0.0.0.0)가 반환되는지 검증
        assertEquals("0.0.0.0", clientIp);
    }

    // IPv6 주소 처리 테스트
    @Test
    void testGetClientIp_withIPv6() {
        // HTTP 요청 객체 모의 생성
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // IPv6 로컬호스트 주소 반환하도록 설정
        when(request.getRemoteAddr()).thenReturn("0:0:0:0:0:0:0:1");

        // IP 추출 메서드 호출
        String clientIp = IpUtil.getClientIp(request);
        // IPv6가 IPv4로 변환되는지 검증
        assertEquals("127.0.0.1", clientIp);
    }

    @Test
    void testGetClientIp_withIPv6Short() {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("::1");

        String clientIp = IpUtil.getClientIp(request);
        assertEquals("127.0.0.1", clientIp);
    }

    // unknown 값 처리 테스트
    @Test
    void testGetClientIp_withUnknownValue() {
        // HTTP 요청 객체 모의 생성
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // X-Forwarded-For 헤더가 unknown 반환하도록 설정
        when(request.getHeader("X-Forwarded-For")).thenReturn("unknown");
        // getRemoteAddr 값 설정
        when(request.getRemoteAddr()).thenReturn("192.168.0.1");

        // IP 추출 메서드 호출
        String clientIp = IpUtil.getClientIp(request);
        // RemoteAddr의 IP가 반환되는지 검증
        assertEquals("192.168.0.1", clientIp);
    }

    // 로컬 IP 판별 테스트
    @Test
    void testIsLocalIp() {
        // 로컬호스트 IP 검증
        assertTrue(IpUtil.isLocalIp("127.0.0.1"));
        // 192.168 대역 사설 IP 검증
        assertTrue(IpUtil.isLocalIp("192.168.1.1"));
        // 10 대역 사설 IP 검증
        assertTrue(IpUtil.isLocalIp("10.0.0.1"));
        // 172.16 대역 사설 IP 검증
        assertTrue(IpUtil.isLocalIp("172.16.0.1"));
        // 공인 IP 검증
        assertFalse(IpUtil.isLocalIp("8.8.8.8"));

        // IPv6 관련 테스트
        assertTrue(IpUtil.isLocalIp("::1")); // IPv6 short form
        assertTrue(IpUtil.isLocalIp("0:0:0:0:0:0:0:1")); // IPv6 full form
    }

    // IPv6 변환 가능 여부 테스트
    @Test
    void testIsConvertibleIPv6() {
        // IPv6 로컬호스트 주소 검증
        assertTrue(IpUtil.isConvertibleIPv6("0:0:0:0:0:0:0:1"));
        // IPv4 매핑된 IPv6 주소 검증
        assertTrue(IpUtil.isConvertibleIPv6("::ffff:192.168.1.1"));
        // 일반 IPv4 주소 검증
        assertFalse(IpUtil.isConvertibleIPv6("192.168.1.1"));

        // 6to4 주소 테스트 추가
        assertTrue(IpUtil.isConvertibleIPv6("2002:c0a8:0101::"));
    }
}
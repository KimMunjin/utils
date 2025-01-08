package kr.or.komca.utils.ip;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import java.util.Arrays;

@Slf4j
public class IpUtil {

    // 클라이언트 IP를 추출하기 위해 확인할 헤더 목록
    private static final String[] IP_HEADER_CANDIDATES = {
            "X-Forwarded-For",           // 프록시 서버가 추가하는 클라이언트 IP
            "X-Real-IP",                 // Nginx 프록시를 통해 전달된 실제 클라이언트 IP
            "Proxy-Client-IP",           // 프록시 클라이언트의 IP
            "WL-Proxy-Client-IP",        // WebLogic 서버에서 사용하는 헤더
            "HTTP_X_FORWARDED_FOR",      // HTTP 포워드 헤더
            "HTTP_X_FORWARDED",          // HTTP 포워드
            "HTTP_X_CLUSTER_CLIENT_IP",  // 클러스터 환경에서 클라이언트 IP
            "HTTP_CLIENT_IP",            // 클라이언트 IP를 직접 지정
            "HTTP_FORWARDED_FOR",        // HTTP 포워드
            "HTTP_FORWARDED",            // HTTP 포워드
            "HTTP_VIA",                  // HTTP 경유 헤더
            "REMOTE_ADDR"                // 기본적으로 HttpServletRequest에서 제공하는 클라이언트 IP
    };

    // "unknown" 문자열을 비교하기 위한 상수
    private static final String UNKNOWN = "unknown";

    // IPv4와 IPv6에서 로컬 호스트를 나타내는 값
    private static final String LOCALHOST_IPV4 = "127.0.0.1";
    private static final String LOCALHOST_IPV6 = "0:0:0:0:0:0:0:1";
    private static final String LOCALHOST_IPV6_SHORT = "::1";

    // IP를 확인할 수 없을 때 반환할 기본값
    private static final String DEFAULT_IP = "0.0.0.0";

    private static final String IPV4_PATTERN = "([0-9]{1,3}\\.){3}[0-9]{1,3}";
    private static final String IPV6_PREFIX = "::ffff:";
    private static final String IPV6_6TO4_PREFIX = "2002:";
    /**
     * HttpServletRequest에서 클라이언트 IP를 추출합니다.
     * 웹사이트에 접속한 사용자의 실제 IP 주소 찾아내는 메서드
     *
     * @param request HttpServletRequest 객체
     * @return 클라이언트의 IP 주소 (없을 경우 기본값 반환)
     */
    public static String getClientIp(HttpServletRequest request) {
        // 요청 객체가 null일 경우 기본 IP를 반환
        if (request == null) {
            return DEFAULT_IP;
        }
        
        // 사용자가 직접 접속할 때도 있지만, 프록시나 로드밸런서를 통해 접속할 때도 있어서 여러 곳 확인 필요
        String ip;

        // 클라이언트 IP를 헤더에서 추출
        for (String header : IP_HEADER_CANDIDATES) {
            ip = request.getHeader(header); // 해당 헤더의 값을 가져옴
            if (StringUtils.hasText(ip)) {
                ip = extractFirstIp(ip);   // 여러 개의 IP가 있을 경우 첫 번째 IP 추출
                if (isValidIp(ip)) {       // 추출된 IP가 유효한지 다시 확인
                    return normalizeIp(ip); // IP를 정규화 후 반환
                }
            }
        }

        // 헤더에서 찾지 못한 경우 getRemoteAddr()을 사용
        ip = request.getRemoteAddr();
        if (isValidIp(ip)) {
            return normalizeIp(ip); // IP를 정규화 후 반환
        }

        // 모든 방법 실패 시 기본값 반환
        return DEFAULT_IP;
    }

    /**
     * IP 주소가 유효한지 확인합니다.
     *
     * @param ip 확인할 IP 주소
     * @return 유효하면 true, 그렇지 않으면 false
     */
    private static boolean isValidIp(String ip) {
        // IP가 null이거나 빈 문자열이거나 "unknown"인 경우 false 반환
        if (!StringUtils.hasText(ip) || UNKNOWN.equalsIgnoreCase(ip)) {
            return false;
        }

        // IP 길이가 너무 긴 경우 false 반환 (IPv6 최대 길이 고려)
        if (ip.length() > 45) {
            return false;
        }

        return ip.matches(IPV4_PATTERN) || // IPv4
                ip.contains(":"); // IPv6
    }

    /**
     * 여러 개의 IP가 포함된 문자열에서 첫 번째 유효한 IP를 추출합니다.
     *
     * @param ip 쉼표로 구분된 IP 문자열
     * @return 첫 번째 유효한 IP (없을 경우 null 반환)
     */
    private static String extractFirstIp(String ip) {
        if (ip == null) {
            return null;
        }


        // 쉼표(,)로 구분된 IP 목록을 스트림으로 처리
        return Arrays.stream(ip.split(","))
                .map(String::trim)       // 각 IP 문자열을 트림
                .filter(IpUtil::isValidIp) // 유효한 IP만 필터링
                .findFirst()            // 첫 번째 유효한 IP를 반환
                .orElse(null);          // 없으면 null 반환
        //보통 맨 앞 IP가 실제 클라이언트 IP
    }

    /**
     * IP 주소를 정규화합니다.
     * IPv6에서 로컬호스트 또는 IPv4-mapped 주소를 처리.
     *
     * @param ip 정규화할 IP
     * @return 정규화된 IP
     */
    private static String normalizeIp(String ip) {
        // IPv6 로컬호스트를 IPv4 로컬호스트로 변환
        if (LOCALHOST_IPV6.equals(ip) || LOCALHOST_IPV6_SHORT.equals(ip)) {
            return LOCALHOST_IPV4;
        }
        // IPv4-mapped IPv6 주소를 IPv4로 변환
        if (ip.startsWith(IPV6_PREFIX)) {
            return ip.substring(IPV6_PREFIX.length());
        }
        return ip; // 변환이 필요하지 않으면 그대로 반환
    }

    /**
     * IP가 로컬 IP인지 확인합니다.
     *
     * @param ip 확인할 IP 주소
     * @return 로컬 IP이면 true, 그렇지 않으면 false
     */
    public static boolean isLocalIp(String ip) {
        // IP가 null이면 로컬이 아니라고 판단
        if (ip == null) {
            return false;
        }
        // 로컬 네트워크 범위에 해당하는지 확인
        return ip.equals(LOCALHOST_IPV4) ||
                ip.equals(LOCALHOST_IPV6) ||
                ip.equals(LOCALHOST_IPV6_SHORT) ||
                ip.startsWith("127.") ||
                ip.startsWith("169.254.") ||
                ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                isInRange172(ip); // 172.16.0.0/12 범위 확인
    }

    /**
     * IP가 172.16.0.0/12 범위에 속하는지 확인합니다.
     *
     * @param ip 확인할 IP 주소
     * @return 범위에 속하면 true, 그렇지 않으면 false
     */
    private static boolean isInRange172(String ip) {
        // 172.16-31로 시작하는 IP는 내부 네트워크용으로 예약된 특별한 범위

        // IP가 172.로 시작하지 않으면 false
        if (!ip.startsWith("172.")) {
            return false;
        }

        try {
            // 두 번째 옥텟을 확인
            String[] parts = ip.split("\\.",4);
            if (parts.length != 4) { // IP가 네 부분으로 나뉘어야 함
                return false;
            }
            int secondOctet = Integer.parseInt(parts[1]);
            // 두 번째 옥텟이 16~31 범위인지 확인
            return secondOctet >= 16 && secondOctet <= 31;
        } catch (NumberFormatException e) {
            return false; // 숫자가 아닌 경우 false 반환
        }
    }

    /**
     * IPv6 주소가 IPv4로 변환 가능한지 확인합니다.
     *
     * @param ip 확인할 IPv6 주소
     * @return 변환 가능하면 true, 그렇지 않으면 false
     */
    public static boolean isConvertibleIPv6(String ip) {
        // IPv6 주소가 로컬호스트이거나 IPv4-mapped 또는 6to4 주소인지 확인
        return ip != null && (
                ip.equals(LOCALHOST_IPV6) ||
                        ip.startsWith(IPV6_PREFIX) || // IPv4-mapped IPv6 주소
                        ip.startsWith(IPV6_6TO4_PREFIX)     // 6to4 주소
        );
    }
}


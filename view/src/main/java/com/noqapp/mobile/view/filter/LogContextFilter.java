package com.noqapp.mobile.view.filter;

import com.noqapp.mobile.view.controller.open.IsWorkingController;
import com.noqapp.search.elastic.config.IPGeoConfiguration;

import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.CityResponse;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.context.ApplicationContext;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.support.WebApplicationContextUtils;

import org.elasticsearch.common.geo.GeoPoint;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

/**
 * User: hitender
 * Date: 11/17/16 9:00 AM
 */
@SuppressWarnings({
    "PMD.BeanMembersShouldSerialize",
    "PMD.LocalVariableCouldBeFinal",
    "PMD.MethodArgumentCouldBeFinal",
    "PMD.LongVariable"
})
public class LogContextFilter implements Filter {
    private static final Logger LOG = LoggerFactory.getLogger(LogContextFilter.class);

    /* https://stackoverflow.com/questions/24894093/ruby-regular-expression-extracting-part-of-url */
    private static final Pattern EXTRACT_ENDPOINT_PATTERN = Pattern.compile("^(([^:/?#]+):)?(//([^/?#]*))?([^?#]*)(\\?([^#]*))?(#(.*))?");
    private static final String REQUEST_ID_MDC_KEY = "X-REQUEST-ID";
    private IPGeoConfiguration ipGeoConfiguration;

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        String uuid = UUID.randomUUID().toString();
        MDC.put(REQUEST_ID_MDC_KEY, uuid);

        HttpServletRequest httpServletRequest = (HttpServletRequest) req;
        Map<String, String> headerMap = getHeadersInfo(httpServletRequest);
        String url = httpServletRequest.getRequestURL().toString();
        String query = httpServletRequest.getQueryString();
        String ip = getHeader(headerMap, "x-forwarded-for");
        String appVersion = getHeader(headerMap, "x-r-ver");
        String flavor = getHeader(headerMap, "x-r-fla");
        String model = getHeader(headerMap, "x-r-mod");
        String lat = getHeader(headerMap, "x-r-lat");
        String lng = getHeader(headerMap, "x-r-lng");
        String did = getHeader(headerMap, "x-r-did");
        String mail = getHeader(headerMap, "x-r-mail");
        String qid = getHeader(headerMap, "x-r-qid");
        String countryCode = "";
        String city = "";
        String geoHash = "";
        try {
            if (ip != null) {
                InetAddress ipAddress = InetAddress.getByName(ip);
                CityResponse response = ipGeoConfiguration.getDatabaseReader().city(ipAddress);
                countryCode = response.getCountry().getIsoCode();
            } else {
                countryCode = "ZZ";
            }
        } catch (AddressNotFoundException e) {
            LOG.warn("Failed finding ip={} reason={}", ip, e.getLocalizedMessage());
        } catch (GeoIp2Exception e) {
            LOG.error("Failed reason={}", e.getLocalizedMessage(), e);
        } catch (IOException e) {
            LOG.error("Failed databaseReader reason={}", e.getLocalizedMessage(), e);
        }

        if (StringUtils.isNotBlank(lat) && StringUtils.isNotBlank(lng)) {
            geoHash = new GeoPoint(Double.parseDouble(lat), Double.parseDouble(lng)).getGeohash();

            LOG.info("Request received:"
                + " host=\"" + getHeader(headerMap, "host") + "\""
                + " userAgent=\"" + getHeader(headerMap, "user-agent") + "\""
                + " accept=\"" + getHeader(headerMap, "accept") + "\""
                + " content-type=\"" + getHeader(headerMap, "content-type") + "\""
                + " ip=\"" + ip + "\""
                + " country=\"" + countryCode + "\""
//                + " city=\"" + city + "\""
                + " geoHash=\"" + geoHash + "\""
                + " appVersion=\"" + appVersion + "\""
                + " flavor=\"" + flavor + "\""
                + " model=\"" + model + "\""
                + " did=\"" + did + "\""
                + " mail=\"" + mail + "\""
                + " qid=\"" + qid + "\""
                + " endpoint=\"" + extractDataFromURL(url, "$5") + "\""
                + " query=\"" + (query == null ? "none" : query) + "\""
                + " url=\"" + url + "\""
            );
        }

        if (StringUtils.isNotBlank(countryCode)) {
            switch (countryCode) {
                case "IN":
                case "US":
                case "NP":
                    //Allowed country
                    break;
//                case "AE": //UAE
//                case "OM": //Oman & Muscat
//                case "SA": //Saudi
//                case "CN": //China
//                case "HK": //Hong Kong
//                case "CZ": //Czechia
//                case "SG": //Singapore
//                    LOG.warn("Request from country {} failed response", countryCode);
//                    HttpServletResponse httpServletResponse = (HttpServletResponse) res;
//                    httpServletResponse.setStatus(SC_NOT_FOUND);
//                    break;
                default:
                    LOG.warn("Request from country {} allowed response", countryCode);
//                    httpServletResponse = (HttpServletResponse) res;
//                    httpServletResponse.setStatus(SC_NOT_FOUND);
            }
        }

        if (isHttpHead(httpServletRequest)) {
            HttpServletResponse httpServletResponse = (HttpServletResponse) res;
            NoBodyResponseWrapper noBodyResponseWrapper = new NoBodyResponseWrapper(httpServletResponse);

            chain.doFilter(new ForceGetRequestWrapper(httpServletRequest), noBodyResponseWrapper);
            noBodyResponseWrapper.setContentLength();
        } else {
            chain.doFilter(req, res);
        }
    }

    private String getHeader(Map<String, String> headers, String header) {
        return CollectionUtils.isEmpty(headers) && !headers.containsKey(header) ? "" : headers.get(header);
    }

    private String extractDataFromURL(String uri, String group) {
        return EXTRACT_ENDPOINT_PATTERN.matcher(uri).replaceFirst(group);
    }

    private Map<String, String> getHeadersInfo(HttpServletRequest request) {
        Map<String, String> map = new HashMap<>();

        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String key = headerNames.nextElement();
            String value = request.getHeader(key);
            map.put(key, value);
        }

        return map;
    }

    @Override
    public void init(FilterConfig filterConfig) {
        LOG.info("Initialized mobile logContextFilter");

        ApplicationContext ctx = WebApplicationContextUtils.getRequiredWebApplicationContext(filterConfig.getServletContext());
        this.ipGeoConfiguration = ctx.getBean(IPGeoConfiguration.class);
    }

    @Override
    public void destroy() {
        LOG.info("Destroyed mobile logContextFilter");
    }

    /**
     * Deals with HTTP HEAD requests and response for all controllers. Even if these controllers are secured its better
     * to treat them nicely and not fail on HEAD request.
     * <p>
     * Added support for HEAD method in filter to prevent failing on HEAD request. As of now there is no valid
     * reason why filter contains this HEAD request as everything is secure after login and there are no bots or
     * crawlers when a valid user has logged in. We plan to use this until a decision would be made in near future.
     * <p>
     * The reason for this addition has already been fixed in code at location below.
     *
     * @see IsWorkingController#isWorking()
     */
    private boolean isHttpHead(HttpServletRequest request) {
        return "HEAD".equals(request.getMethod());
    }

    private static class ForceGetRequestWrapper extends HttpServletRequestWrapper {
        public ForceGetRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public String getMethod() {
            return "GET";
        }
    }

    private static class NoBodyResponseWrapper extends HttpServletResponseWrapper {
        private final NoBodyOutputStream noBodyOutputStream = new NoBodyOutputStream();
        private PrintWriter writer;

        public NoBodyResponseWrapper(HttpServletResponse response) {
            super(response);
        }

        public ServletOutputStream getOutputStream() {
            return noBodyOutputStream;
        }

        public PrintWriter getWriter() throws UnsupportedEncodingException {
            if (writer == null) {
                writer = new PrintWriter(new OutputStreamWriter(noBodyOutputStream, getCharacterEncoding()));
            }

            return writer;
        }

        void setContentLength() {
            super.setContentLength(noBodyOutputStream.getContentLength());
        }
    }

    private static class NoBodyOutputStream extends ServletOutputStream {
        private int contentLength = 0;

        int getContentLength() {
            return contentLength;
        }

        public void write(int b) {
            contentLength++;
        }

        public void write(byte[] buf, int offset, int len) {
            contentLength += len;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setWriteListener(WriteListener writeListener) {

        }
    }
}

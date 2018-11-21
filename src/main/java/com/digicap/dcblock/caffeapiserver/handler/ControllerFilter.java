package com.digicap.dcblock.caffeapiserver.handler;

import com.digicap.dcblock.caffeapiserver.exception.ForbiddenException;
import com.digicap.dcblock.caffeapiserver.exception.UnknownException;
import com.digicap.dcblock.caffeapiserver.util.ApplicationProperties;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.TeeOutputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ControllerFilter { // implements Filter {

    ApplicationProperties properties;

    @Autowired
    public ControllerFilter(ApplicationProperties properties) {
        this.properties = properties;
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        try {
//            HttpServletRequest httpRequest = (HttpServletRequest)request;
//            HttpServletResponse httpResponse = (HttpServletResponse)response;
//
//            HttpRequestWrapper httpRequestWrapper = new HttpRequestWrapper(httpRequest);

            // before Controller
//            preFilter(httpRequestWrapper);

            chain.doFilter(request, response);

            // after Controller
//            postFilter(httpRequestWrapper, httpResponse);
        } catch (ServletException | IOException e) {
            throw new UnknownException(e.getMessage());
        }
    }

    private boolean preFilter(HttpRequestWrapper request) throws ForbiddenException, IOException {
        long threadId = Thread.currentThread().getId();

        String body = IOUtils.toString(request.getInputStream(), StandardCharsets.UTF_8).replaceAll("\r", "").replaceAll("\n", "");
        log.info(threadId + " " + request.getMethod() + " " + request.getRequestURI() + " " + body);

        String remoteIp = getRemoteIP(request);

        // remote client ip filtering.
        List<String> allow_remotes = properties.getAllow_remotes();
        if (!allow_remotes.contains(remoteIp)) {
            log.error(remoteIp + " is not allow remote ip.");
            throw new ForbiddenException("not allow remote client");
        }

        return true;
    }

    private void postFilter(HttpServletRequest request, HttpServletResponse response) throws IOException {
        long threadId = Thread.currentThread().getId();
        log.info(threadId + " " + request.getMethod() + " " + request.getRequestURI() + " " + response.getStatus());
    }

    private String getRemoteIP(HttpServletRequest request){
        String ip = request.getHeader("X-FORWARDED-FOR");

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }

        return ip;
    }

    private class HttpRequestWrapper extends HttpServletRequestWrapper {

        @Getter
        private byte[] bodyData;

        public HttpRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);

            InputStream is = super.getInputStream();
            bodyData = IOUtils.toByteArray(is);
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            final ByteArrayInputStream bis = new ByteArrayInputStream(bodyData);
            return new ServletImpl(bis);
        }

        private class ServletImpl extends ServletInputStream {

            private InputStream is;

            public ServletImpl(InputStream bis) {
                is = bis;
            }

            @Override
            public int read() throws IOException {
                return is.read();
            }

            @Override
            public int read(byte[] b) throws IOException {
                return is.read(b);
            }

            @Override
            public boolean isFinished() {
                return false;
            }

            @Override
            public boolean isReady() {
                return false;
            }

            @Override
            public void setReadListener(ReadListener listener) {
            }
        }
    }

//    private class HttpResponseWrapper extends HttpServletResponseWrapper {
//
//        @Getter
//        private byte[] bodyData;
//
//        public HttpResponseWrapper(HttpServletResponse response) throws IOException {
//            super(response);
//
//            OutputStream out = super.getOutputStream();
//            bodyData = IOUtils.toByteArray(out);
//        }
//
//        @Override
//        public ServletOutputStream getOutputStream() throws IOException {
//            final ByteArrayOutputStream bis = new ByteArrayOutputStream(bodyData);
//            return new ServletImpl(bis);
//        }
//
//        private class ServletImpl extends ServletOutputStream {
//
//            private final TeeOutputStream targetStream;
//
//            public ServletImpl(OutputStream one, OutputStream two) {
//                targetStream = new TeeOutputStream(one, two);
//            }
//
//            @Override
//            public void write(int arg0) throws IOException {
//                this.targetStream.write(arg0);
//            }
//
//            public void flush() throws IOException {
//                super.flush();
//                this.targetStream.flush();
//            }
//
//            public void close() throws IOException {
//                super.close();
//                this.targetStream.close();
//            }
//
//            @Override
//            public boolean isReady() {
//                // TODO Auto-generated method stub
//                return false;
//            }
//
//            @Override
//            public void setWriteListener(WriteListener listener) {
//                // TODO Auto-generated method stub
//            }
//        }
//    }
}
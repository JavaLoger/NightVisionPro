package org.loger.signalr;

import java.io.IOException;
import java.util.logging.Logger;
import okhttp3.HttpUrl;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class SignalRNegotiateInterceptor implements Interceptor {
    private static final String NEGOTIATE_PATH = "/negotiate";
    private final boolean debug;
    private final Logger logger;

    public SignalRNegotiateInterceptor(Logger logger, boolean debug) {
        this.logger = logger;
        this.debug = debug;
    }

    @Override // net.mlsac.libs.okhttp3.Interceptor
    public Response intercept(Interceptor.Chain chain) throws IOException {
        Request originalRequest = chain.request();
        HttpUrl url = originalRequest.url();
        if (url.encodedPath().endsWith(NEGOTIATE_PATH) && "GET".equals(originalRequest.method())) {
            if (this.debug) {
                this.logger.info("[SignalR] Converting GET negotiate request to POST for ASP.NET Core compatibility");
            }
            Request newRequest = originalRequest.newBuilder().method("POST", RequestBody.create(new byte[0], (MediaType) null)).build();
            return chain.proceed(newRequest);
        }
        return chain.proceed(originalRequest);
    }
}

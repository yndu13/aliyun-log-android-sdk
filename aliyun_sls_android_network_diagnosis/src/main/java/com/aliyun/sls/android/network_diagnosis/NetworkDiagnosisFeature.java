package com.aliyun.sls.android.network_diagnosis;

import com.alibaba.netspeed.network.DetectCallback;
import com.alibaba.netspeed.network.Diagnosis;
import com.alibaba.netspeed.network.DnsConfig;
import com.alibaba.netspeed.network.HttpConfig;
import com.alibaba.netspeed.network.Logger;
import com.alibaba.netspeed.network.MtrConfig;
import com.alibaba.netspeed.network.PingConfig;
import com.alibaba.netspeed.network.TcpPingConfig;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.TextUtils;
import android.util.Pair;
import com.aliyun.sls.android.core.SLSLog;
import com.aliyun.sls.android.core.configuration.Configuration;
import com.aliyun.sls.android.core.configuration.Credentials;
import com.aliyun.sls.android.core.configuration.Credentials.NetworkDiagnosisCredentials;
import com.aliyun.sls.android.core.feature.SdkFeature;
import com.aliyun.sls.android.core.sender.SdkSender;
import com.aliyun.sls.android.core.utdid.Utdid;
import com.aliyun.sls.android.ot.Attribute;
import com.aliyun.sls.android.ot.ISpanProcessor;
import com.aliyun.sls.android.ot.SpanBuilder;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author gordon
 * @date 2022/7/22
 */
public class NetworkDiagnosisFeature extends SdkFeature implements INetworkDiagnosis {
    private static final String TAG = "NetworkDiagnosisFeature";

    public static final int DEFAULT_PING_SIZE = 64;
    public static final int DEFAULT_TIMEOUT = 2 * 1000;
    public static final int DEFAULT_MAX_TIMES = 10;

    public static final int DEFAULT_MTR_MAX_TTL = 30;
    public static final int DEFAULT_MTR_MAX_PATH = 1;

    public static final String DNS_TYPE_IPv4 = "A";
    public static final String DNS_TYPE_IPv6 = "AAAA";

    private static final TaskIdGenerator TASK_ID_GENERATOR = new TaskIdGenerator();
    private NetworkDiagnosisSender networkDiagnosisSender;

    @Override
    public SpanBuilder newSpanBuilder(String spanName) {
        return new SpanBuilder(spanName, networkDiagnosisSender, configuration.spanProvider);
    }

    @Override
    protected void onInitSender(Context context, Credentials credentials, Configuration configuration) {
        super.onInitSender(context, credentials, configuration);
    }

    @Override
    protected void onInitialize(Context context, Credentials credentials, Configuration configuration) {
        final NetworkDiagnosisCredentials networkDiagnosisCredentials = credentials.networkDiagnosisCredentials;
        if (null == networkDiagnosisCredentials) {
            SLSLog.w(TAG, "NetworkDiagnosisCredentials must not be null.");
            return;
        }

        Diagnosis.init(
            networkDiagnosisCredentials.secretKey,
            Utdid.getInstance().getUtdid(context),
            networkDiagnosisCredentials.siteId,
            networkDiagnosisCredentials.extension
        );

        networkDiagnosisSender = new NetworkDiagnosisSender(context, this);
        networkDiagnosisSender.initialize(credentials);

        Diagnosis.registerLogger(this, networkDiagnosisSender);

        NetworkDiagnosis.getInstance().setNetworkDiagnosis(this);
    }

    @Override
    protected void onPostInitialize(Context context) {

    }

    @Override
    protected void onStop(Context context) {

    }

    @Override
    protected void onPostStop(Context context) {

    }

    @Override
    public void setCredentials(Credentials credentials) {
        super.setCredentials(credentials);
        if (null == networkDiagnosisSender) {
            return;
        }

        networkDiagnosisSender.setCredentials(credentials);
    }

    @Override
    public void setPolicyDomain(String domain) {
        if (TextUtils.isEmpty(domain)) {
            return;
        }

        Diagnosis.setPolicyDomain(domain);
    }

    @Override
    public void disableExNetworkInfo() {
        Diagnosis.disableExNetworkInfo();
    }

    public void http(String url) {
        Diagnosis.startHttpPing(
            new HttpConfig(
                TASK_ID_GENERATOR.generate(),
                url,
                (DetectCallback)null,
                this
            )
        );
    }

    @Override
    public void ping(String domain) {
        this.ping(domain, DEFAULT_PING_SIZE);
    }

    @Override
    public void ping(String domain, int size) {
        this.ping(domain, size, DEFAULT_MAX_TIMES, DEFAULT_TIMEOUT);
    }

    @Override
    public void ping(String domain, int maxTimes, int timeout) {
        this.ping(domain, DEFAULT_PING_SIZE, maxTimes, timeout);
    }

    @Override
    public void ping(String domain, int size, int maxTimes, int timeout) {
        Diagnosis.startPing(
            new PingConfig(
                TASK_ID_GENERATOR.generate(),
                domain,
                size,
                maxTimes,
                timeout,
                null,
                this
            )
        );
    }

    @Override
    public void tcpPing(String domain, int port) {
        this.tcpPing(domain, port, DEFAULT_MAX_TIMES);
    }

    @Override
    public void tcpPing(String domain, int port, int maxTimes) {
        this.tcpPing(domain, port, maxTimes, DEFAULT_TIMEOUT);
    }

    @Override
    public void tcpPing(String domain, int port, int maxTimes, int timeout) {
        Diagnosis.startTcpPing(
            new TcpPingConfig(
                TASK_ID_GENERATOR.generate(),
                domain,
                port,
                maxTimes,
                timeout,
                null,
                this
            )
        );
    }

    @Override
    public void mtr(String domain) {
        this.mtr(domain, DEFAULT_MTR_MAX_TTL);
    }

    @Override
    public void mtr(String domain, int maxTTL) {
        this.mtr(domain, maxTTL, DEFAULT_MTR_MAX_PATH);
    }

    @Override
    public void mtr(String domain, int maxTTL, int maxPaths) {
        this.mtr(domain, maxTTL, maxPaths, DEFAULT_MAX_TIMES);
    }

    @Override
    public void mtr(String domain, int maxTTL, int maxPaths, int maxTimes) {
        this.mtr(domain, maxTTL, maxPaths, maxTimes, DEFAULT_TIMEOUT);
    }

    @Override
    public void mtr(String domain, int maxTTL, int maxPaths, int maxTimes, int timeout) {
        Diagnosis.startMtr(
            new MtrConfig(
                TASK_ID_GENERATOR.generate(),
                domain,
                maxTTL,
                maxPaths,
                maxTimes,
                timeout,
                null,
                this
            )
        );
    }

    @Override
    public void dns(String nameServer, String domain) {
        this.dns(nameServer, domain, DNS_TYPE_IPv4);
    }

    @Override
    public void dns(String nameServer, String domain, String type) {
        this.dns(nameServer, domain, type, DEFAULT_TIMEOUT);
    }

    @Override
    public void dns(String nameServer, String domain, String type, int timeout) {
        Diagnosis.startDns(
            new DnsConfig(
                TASK_ID_GENERATOR.generate(),
                nameServer,
                domain,
                type,
                timeout,
                null,
                this
            )
        );
    }

    private static class NetworkDiagnosisSender extends SdkSender implements Logger, ISpanProcessor {
        static {
            TAG = "NetworkDiagnosisSender";
        }

        private final SdkFeature feature;

        public NetworkDiagnosisSender(Context context, SdkFeature feature) {
            super(context);
            this.feature = feature;
        }

        @Override
        protected String provideLogFileName() {
            return "net_d";
        }

        @Override
        protected String provideEndpoint(Credentials credentials) {
            return super.provideEndpoint(credentials.networkDiagnosisCredentials);
        }

        @Override
        protected String provideProjectName(Credentials credentials) {
            return credentials.networkDiagnosisCredentials.project;
        }

        @Override
        protected String provideLogstoreName(Credentials credentials) {
            return credentials.networkDiagnosisCredentials.logstore;
        }

        @Override
        protected String provideAccessKeyId(Credentials credentials) {
            return credentials.networkDiagnosisCredentials.accessKeyId;
        }

        @Override
        protected String provideAccessKeySecret(Credentials credentials) {
            return credentials.networkDiagnosisCredentials.accessKeySecret;
        }

        @Override
        protected String provideSecurityToken(Credentials credentials) {
            return credentials.networkDiagnosisCredentials.securityToken;
        }

        @Override
        protected void initLogProducer(Credentials credentials, String fileName) {
            super.initLogProducer(credentials, fileName);
        }

        @Override
        public void report(Object context, String msg) {
            if (TextUtils.isEmpty(msg)) {
                return;
            }

            JSONObject object;
            try {
                object = new JSONObject(msg);
            } catch (JSONException e) {
                SLSLog.w(TAG, "msg to json error. e: " + e.getMessage());
                return;
            }

            final String method = object.optString("method");
            if (TextUtils.isEmpty(method)) {
                return;
            }

            SLSLog.v(TAG, "network diagnosis result: method=" + method + ", result: " + msg);

            SpanBuilder builder = feature.newSpanBuilder("network_diagnosis");
            builder.addAttribute(
                Attribute.of(
                    Pair.create("t", "net_d"),
                    Pair.create("net.type", method),
                    Pair.create("net.origin", msg)
                )
            );
            builder.build().end();
        }

        @Override
        public void setCredentials(Credentials credentials) {
            super.setCredentials(credentials.networkDiagnosisCredentials);
        }

        @Override
        public void debug(String tag, String msg) {
            SLSLog.d(tag, msg);
        }

        @Override
        public void info(String tag, String msg) {
            SLSLog.i(tag, msg);
        }

        @Override
        public void warm(String tag, String msg) {
            SLSLog.w(tag, msg);
        }

        @Override
        public void error(String tag, String msg) {
            SLSLog.e(tag, msg);
        }
    }

    private static class TaskIdGenerator {

        private final String prefix = String.valueOf(System.nanoTime());
        private long index = 0;

        @SuppressLint("DefaultLocale")
        synchronized String generate() {
            index += 1;
            return String.format("%s_%d", prefix, index);
        }
    }
}
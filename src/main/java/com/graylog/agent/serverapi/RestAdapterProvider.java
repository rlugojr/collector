package com.graylog.agent.serverapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.graylog.agent.AgentVersion;
import com.graylog.agent.annotations.GraylogServerURL;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;

import javax.inject.Inject;
import javax.inject.Provider;

public class RestAdapterProvider implements Provider<RestAdapter> {
    private final String graylogServerURL;

    @Inject
    public RestAdapterProvider(@GraylogServerURL String graylogServerURL) {
        this.graylogServerURL = graylogServerURL;
    }

    @Override
    public RestAdapter get() {
        return new RestAdapter.Builder()
                .setEndpoint(graylogServerURL)
                .setConverter(new JacksonConverter(new ObjectMapper()))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("User-Agent", "Graylog Agent " + AgentVersion.CURRENT);
                        request.addHeader("X-Graylog-Agent-Version", AgentVersion.CURRENT.version());
                    }
                })
                .build();
    }
}
package com.doopp.gutty.db;

import org.apache.ibatis.logging.Log;
import org.apache.ibatis.session.Configuration;
import org.mybatis.guice.configuration.settings.ConfigurationSetting;

public class LogImplConfigurationSetting implements ConfigurationSetting {

    private final Class<? extends  Log> logImpl;

    public LogImplConfigurationSetting(Class<? extends  Log> logImpl) {
        this.logImpl = logImpl;
    }

    public void applyConfigurationSetting(Configuration configuration) {
        configuration.setLogImpl(this.logImpl);
    }
}

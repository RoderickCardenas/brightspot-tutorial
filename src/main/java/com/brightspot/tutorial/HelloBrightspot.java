package com.brightspot.tutorial;

import java.util.Optional;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Site;
import com.psddev.dari.util.Utils;
import org.apache.commons.lang3.StringUtils;


public class HelloBrightspot extends Content implements Directory.Item {

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String createPermalink(Site site) {
        return Optional.ofNullable(getName())
            .map(Utils::toNormalized)
            .filter(StringUtils::isNotEmpty)
            .map("/hello-"::concat)
            .orElse(null);
    }
}

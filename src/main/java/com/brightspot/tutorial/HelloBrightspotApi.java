package com.brightspot.tutorial;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import com.psddev.dari.db.Singleton;
import com.psddev.graphql.GraphQLCorsConfiguration;
import com.psddev.graphql.cda.ContentDeliveryApiAccessOption;
import com.psddev.graphql.cda.ContentDeliveryApiAccessOptionImplicit;
import com.psddev.graphql.cda.ContentDeliveryApiEndpoint;
import com.psddev.graphql.cda.ContentDeliveryEntryPointField;

public class HelloBrightspotApi extends ContentDeliveryApiEndpoint implements Singleton {

    @Override
    protected String getPathSuffix() {
        return "/hello-brightspot";
    }

    @Override
    public List <ContentDeliveryEntryPointField> getQueryEntryFields() {
        return Stream.of(
            new ContentDeliveryEntryPointField(
                HelloBrightspotViewModel.class,
                "HelloBrightspot",
                "Say Hello to Brightspot.")
        ).collect(Collectors.toList());
    }

    @Override
    public ContentDeliveryApiAccessOption getAccessOption() {
        return new ContentDeliveryApiAccessOptionImplicit();
    }

    @Override
    protected void updateCorsConfiguration(GraphQLCorsConfiguration corsConfiguration) {
        super.updateCorsConfiguration(corsConfiguration);
        corsConfiguration.addWhitelistedDomain("localhost");
    }
}
package com.hwans.apiserver.service.authentication.oauth2;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Map;

@Getter
public class ProviderAttributes {
    private final ProviderType providerType;
    private final Map<String, Object> attributes;
    private final String name;
    private final String email;
    private final String profileImagelUrl;

    public ProviderAttributes(ProviderType providerType, Map<String, Object> attributes,
                              String nameAttributeKey, String emailAttributeKey, String profileImagelUrlAttributeKey)
    {
        this.providerType = providerType;
        this.attributes = attributes;
        this.name = attributes.getOrDefault(nameAttributeKey, "").toString();
        this.email = attributes.getOrDefault(emailAttributeKey, "").toString();
        this.profileImagelUrl = attributes.getOrDefault(profileImagelUrlAttributeKey, "").toString();
    }

    public static ProviderAttributes of(ProviderType providerType, Map<String, Object> attributes) {
        switch (providerType) {
            case GOOGLE:
                return ofGoogle(providerType, attributes);
        }
        return null;
    }

    private static ProviderAttributes ofGoogle(ProviderType providerType, Map<String, Object> attributes) {
        return new ProviderAttributes(providerType, attributes,
                "name", "email", "picture");
    }
}

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
                              String nameAttributeKey, String emailAttributeKey, String profileImagelUrlAttributeKey) {
        this.providerType = providerType;
        this.attributes = attributes;
        this.name = getAttributeValue(attributes, nameAttributeKey);
        this.email = getAttributeValue(attributes, emailAttributeKey);
        this.profileImagelUrl = getAttributeValue(attributes, profileImagelUrlAttributeKey);
    }

    private static String getAttributeValue(Map<String, Object> attributes, String key) {
        if (attributes == null || key == null)
            return null;

        int index = key.indexOf('/');
        if (index < 0) {
            return attributes.getOrDefault(key, "").toString();
        } else {
            Map<String, Object> children = (Map<String, Object>) attributes.get(key.substring(0, index));
            return getAttributeValue(children, key.substring(index + 1));
        }
    }

    public static ProviderAttributes of(ProviderType providerType, Map<String, Object> attributes) {
        switch (providerType) {
            case GOOGLE:
                return ofGoogle(providerType, attributes);
            case GITHUB:
                return ofGithub(providerType, attributes);
            case FACEBOOK:
                return ofFacebook(providerType, attributes);
            case NAVER:
                return ofNaver(providerType, attributes);
            case KAKAO:
                return ofKakao(providerType, attributes);
        }
        return null;
    }

    private static ProviderAttributes ofGoogle(ProviderType providerType, Map<String, Object> attributes) {
        return new ProviderAttributes(providerType, attributes,
                "name", "email", "picture");
    }

    private static ProviderAttributes ofGithub(ProviderType providerType, Map<String, Object> attributes) {
        return new ProviderAttributes(providerType, attributes,
                "name", "email", "avatar_url");
    }

    private static ProviderAttributes ofFacebook(ProviderType providerType, Map<String, Object> attributes) {
        return new ProviderAttributes(providerType, attributes,
                "name", "email", "imageUrl");
    }

    private static ProviderAttributes ofNaver(ProviderType providerType, Map<String, Object> attributes) {
        return new ProviderAttributes(providerType, attributes,
                "response/nickname", "response/email", "response/profile_image");
    }

    private static ProviderAttributes ofKakao(ProviderType providerType, Map<String, Object> attributes) {
        return new ProviderAttributes(providerType, attributes,
                "properties/nickname", "account_email", "properties/thumbnail_image");
    }
}

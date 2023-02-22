package com.hwans.apiserver.service.authentication.oauth2;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.Map;

@Getter
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class  ProviderAttributes {
    private final ProviderType providerType;
    private final Map<String, Object> attributes;
    private final String name;
    private final String email;
    private final String profileImagelUrl;

    private static String getAttributeValue(Map<String, Object> attributes, String key, String defaultValue) {
        if (attributes == null || key == null)
            return null;

        int index = key.indexOf('/');
        if (index < 0) {
            var value = attributes.getOrDefault(key, defaultValue);
            if(value == null) {
                return defaultValue;
            } else {
                return value.toString();
            }
        } else {
            Map<String, Object> children = (Map<String, Object>) attributes.get(key.substring(0, index));
            return getAttributeValue(children, key.substring(index + 1));
        }
    }

    private static String getAttributeValue(Map<String, Object> attributes, String key) {
        return getAttributeValue(attributes, key, "");
    }

    private static ProviderAttributes ofAttributeKey(ProviderType providerType, Map<String, Object> attributes,
                              String nameAttributeKey, String emailAttributeKey, String profileImagelUrlAttributeKey) {
        return new ProviderAttributes(providerType, attributes,
                getAttributeValue(attributes, nameAttributeKey),
                getAttributeValue(attributes, emailAttributeKey, "Unknown"),
                getAttributeValue(attributes, profileImagelUrlAttributeKey));
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
            case DISCORD:
                return ofDiscord(providerType, attributes);
            case MICROSOFT:
                return ofMicrosoft(providerType, attributes);
        }
        return null;
    }

    private static ProviderAttributes ofGoogle(ProviderType providerType, Map<String, Object> attributes) {
        return ProviderAttributes.ofAttributeKey(providerType, attributes,
                "name", "email", "picture");
    }

    private static ProviderAttributes ofGithub(ProviderType providerType, Map<String, Object> attributes) {
        return ProviderAttributes.ofAttributeKey(providerType, attributes,
                "name", "email", "avatar_url");
    }

    private static ProviderAttributes ofFacebook(ProviderType providerType, Map<String, Object> attributes) {
        return ProviderAttributes.ofAttributeKey(providerType, attributes,
                "name", "email", "imageUrl");
    }

    private static ProviderAttributes ofNaver(ProviderType providerType, Map<String, Object> attributes) {
        return ProviderAttributes.ofAttributeKey(providerType, attributes,
                "response/name", "response/email", "response/profile_image");
    }

    private static ProviderAttributes ofKakao(ProviderType providerType, Map<String, Object> attributes) {
        return ProviderAttributes.ofAttributeKey(providerType, attributes,
                "kakao_account/profile/nickname", "kakao_account/email", "kakao_account/profile/profile_image_url");
    }

    private static ProviderAttributes ofDiscord(ProviderType providerType, Map<String, Object> attributes) {
        var profileImageUrl = "https://cdn.discordapp.com/avatars/" +
                getAttributeValue(attributes, "id") + "/" + getAttributeValue(attributes, "avatar") + ".png";

        return new ProviderAttributes(providerType, attributes,
                getAttributeValue(attributes, "username"),
                getAttributeValue(attributes, "email"),
                profileImageUrl);
    }

    private static ProviderAttributes ofMicrosoft(ProviderType providerType, Map<String, Object> attributes) {
        return ProviderAttributes.ofAttributeKey(providerType, attributes,
                "displayName", "userPrincipalName", null);
    }
}

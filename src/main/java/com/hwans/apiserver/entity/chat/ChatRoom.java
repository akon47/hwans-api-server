package com.hwans.apiserver.entity.chat;

import com.hwans.apiserver.entity.BaseEntity;
import com.hwans.apiserver.entity.account.Account;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "tb_chat_room")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoom extends BaseEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID id;
    @Column(length = 255, nullable = false)
    private String title;
    @Column(length = 2000)
    private String description;
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "chatRoom")
    @OrderBy(value = "createdAt asc")
    private final List<ChatMessage> messages = new ArrayList<>();
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_account_id", nullable = false)
    private Account ownerAccount;
    @Column(nullable = false)
    private boolean deleted;
}
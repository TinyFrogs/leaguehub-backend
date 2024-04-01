package leaguehub.leaguehubbackend.domain.notice.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@DynamicUpdate
@Getter
public class Notice {

    @Id
    @Column(name = "notice_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private GameType gameType;

    private String gameLink;

    private String gameTitle;

    private String gameInfo;


    public static Notice createNotice(GameType gameType, String gameLink, String gameTitle, String gameInfo) {

        Notice notice = new Notice();
        notice.gameType = gameType;
        notice.gameLink = gameLink;
        notice.gameTitle = gameTitle;
        notice.gameInfo = gameInfo;

        return notice;
    }

    public void updateNotice(Notice updateNotice) {
        this.gameLink = updateNotice.gameLink;
        this.gameTitle = updateNotice.gameTitle;
        this.gameInfo = updateNotice.gameInfo;
    }
}

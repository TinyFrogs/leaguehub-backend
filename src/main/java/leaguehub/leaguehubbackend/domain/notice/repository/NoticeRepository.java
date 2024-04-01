package leaguehub.leaguehubbackend.domain.notice.repository;

import leaguehub.leaguehubbackend.domain.notice.entity.GameType;
import leaguehub.leaguehubbackend.domain.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {


    List<Notice> findAllByGameTypeOrderByIdAsc(@Param("gameType") GameType gameType);
}

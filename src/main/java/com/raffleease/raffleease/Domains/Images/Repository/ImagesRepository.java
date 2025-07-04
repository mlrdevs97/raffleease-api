package com.raffleease.raffleease.Domains.Images.Repository;

import com.raffleease.raffleease.Domains.Associations.Model.Association;
import com.raffleease.raffleease.Domains.Images.Model.Image;
import com.raffleease.raffleease.Domains.Images.Model.ImageStatus;
import com.raffleease.raffleease.Domains.Raffles.Model.Raffle;
import com.raffleease.raffleease.Domains.Users.Model.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository("ImagesRepository")
public interface ImagesRepository extends JpaRepository<Image, Long> {

    @Query("SELECT COUNT(i) FROM Image i WHERE i.status = :status AND i.user = :user")
    int countImagesByUserAndStatus(@Param("user") User user, @Param("status") ImageStatus status);

    List<Image> findAllByRaffleIsNullAndCreatedAtBefore(LocalDateTime cutoff);
    List<Image> findAllByRaffleIsNullAndUserAndStatus(User user, ImageStatus status);
    List<Image> findAllByRaffle(Raffle raffle);

    @Query("SELECT i.filePath FROM Image i WHERE i.filePath IS NOT NULL")
    List<String> findAllFilePaths();

    List<Image> findAllByUserAndStatus(User user, ImageStatus imageStatus);
    List<Image> findAllByUserAndRaffleAndStatus(User user, Raffle raffle, ImageStatus imageStatus);
}

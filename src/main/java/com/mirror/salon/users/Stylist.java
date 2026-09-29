package com.mirror.salon.users;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "STYLIST")
@Getter
@Setter
@NoArgsConstructor
public class Stylist {

    @Id
    @Column(name = "staff_id")
    private Integer staffId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "staff_id")
    private Staff staff;

    @Column(name = "bio", length = 500)
    private String bio;

    @Column(name = "photo_url", length = 255)
    private String photoUrl;

    @Column(name = "specialty_title", length = 100)
    private String specialtyTitle;
}

package world.pasds.back.team.entity;

import jakarta.persistence.*;
import lombok.Getter;
import world.pasds.back.common.BaseEntity;

@Entity
@Getter
public class PrivateDataRole extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(cascade = CascadeType.REMOVE)
    @JoinColumn(name = "privatedata_id")
    private PrivateData privateData;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;
}
package rs.mmitic.njp.rafcloud

import org.springframework.format.annotation.DateTimeFormat
import java.time.LocalDate
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.persistence.ManyToOne


@Entity
class Machine(
    var name: String,
    var uid: String,
    var status: MachineStatus,
    var active: Boolean,
    @ManyToOne
    var createdBy: User,
    var creationDate: LocalDate = LocalDate.now(),
    @Id @GeneratedValue
    var id: Long? = null
)

enum class MachineStatus(val transitionStatus: MachineStatus? = null) {
    STOPPING, STARTING, STOPPED(STOPPING), RUNNING(STARTING)
}

class MachineDTO(
    val name: String,
    val uid: String,
    val status: MachineStatus,
    val createdBy: String,
    val creationDate: LocalDate
)

fun Machine.toDTO() = MachineDTO(name, uid, status, createdBy.username, creationDate)

class SearchModel(
    var name: String?,
    var status: List<String>?,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    var dateFrom: LocalDate?,
    @field:DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    var dateTo: LocalDate?
)

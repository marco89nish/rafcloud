package rs.mmitic.njp.rafcloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Service
import rs.mmitic.njp.rafcloud.MachineStatus.*
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import kotlin.random.Random

@Service
class MachineService @Autowired constructor(
    private val machineRepository: MachineRepository
) {
    private val logger: Logger = LoggerFactory.getLogger(MachineService::class.java)

    fun getAllActive(owner: User): List<Machine> =
        machineRepository.findAllByCreatedByAndActiveIsTrue(owner)

    fun createMachine(name: String?, user: User): Boolean {
        val machineEntity = Machine(
            id = null, //autogenerate by JPA
            name = name ?: "",
            uid = UUID.randomUUID().toString(),
            status = STOPPED,
            active = true,
            createdBy = user
        )
        machineRepository.save(machineEntity)

        if (name == null) {  // Generate name based on id
            machineRepository.updateAndSave(machineEntity) {
                it.name = "Machine ${it.id}"
            }
        }

        logger.info("Created machine ${machineEntity.id}")
        return true
    }

    fun startMachine(uid: String, user: User): Boolean {
        val machine = findMachine(uid, user, STOPPED) ?: return false
        GlobalScope.launch(Dispatchers.IO) {
            machine.changeStatusTo(RUNNING)
        }
        return true
    }

    fun stopMachine(uid: String, user: User): Boolean {
        val machine = findMachine(uid, user, RUNNING) ?: return false
        GlobalScope.launch(Dispatchers.IO) {
            machine.changeStatusTo(STOPPED)
        }
        return true
    }

    private fun findMachine(uid: String, user: User, wantedStatus: MachineStatus) =
        machineRepository.findByUid(uid)?.takeIf {
            it.isActiveAndOwnedBy(user) && it.status == wantedStatus
        }

    fun restartMachine(uid: String, user: User): Boolean {
        val machine = findMachine(uid, user, RUNNING) ?: return false
        GlobalScope.launch(Dispatchers.IO) {
            val duration = getRandomTime()
            machine.changeStatusTo(STOPPED, duration / 2)
            machine.changeStatusTo(RUNNING, duration / 2)
        }
        return true
    }

    fun destroyMachine(uid: String, user: User): Boolean {
        val machine = findMachine(uid, user, STOPPED) ?: return false
        machineRepository.updateAndSave(machine) {
            it.active = false
        }
        logger.info("Deactivated machine ${machine.id}")
        return true
    }

    fun searchMachines(machineQueryModel: SearchModel, user: User): List<Machine> =
        machineRepository.searchMachines(machineQueryModel, user)

    private fun getRandomTime() = 10_000L + Random.nextInt(10_000)
    private val machinesChangingState = ConcurrentSkipListSet<Long>()
    private suspend fun Machine.changeStatusTo(newStatus: MachineStatus, duration: Long = getRandomTime()) {
        val machine = this
        val machineId = machine.id!!
        val added = machinesChangingState.add(machineId)
        if (!added) { //another request already started changing status
            return
        }

        newStatus.transitionStatus?.let { temporaryStatus ->
            machineRepository.updateAndSave(machine) {
                it.status = temporaryStatus
            }
            logger.info("Changed status of machine $machineId to $temporaryStatus")
        }

        delay(duration)

        machineRepository.updateAndSave(machine) {
            it.status = newStatus
        }
        logger.info("Changed status of machine $machineId to $newStatus")

        machinesChangingState -= machineId
    }
}

private inline fun Machine.isActiveAndOwnedBy(user: User) = this.let { machine ->
    machine.active && machine.createdBy.id == user.id
}

private inline fun <T, ID> CrudRepository<T, ID>.updateAndSave(entity: T, updateFunction: (T) -> Unit) {
    updateFunction(entity)
    this.save(entity)
}

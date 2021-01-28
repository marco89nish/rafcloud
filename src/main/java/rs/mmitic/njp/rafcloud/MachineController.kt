package rs.mmitic.njp.rafcloud

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.security.Principal

@RestController
@RequestMapping("/machines")
class MachineController @Autowired constructor(
    private val machineService: MachineService,
    private val userRepository: UserRepository
) {

    @GetMapping("/active")
    fun getAllActive(principal: Principal): List<MachineDTO> =
        machineService.getAllActive(getUser(principal)).map { it.toDTO() }

    @GetMapping("/search")
    fun search(query: SearchModel, principal: Principal): List<MachineDTO> =
        machineService.searchMachines(query, getUser(principal)).map { it.toDTO() }

    @PostMapping("/create")
    fun create(@RequestParam name: String?, principal: Principal) = handleErrors {
        machineService.createMachine(name, getUser(principal))
    }

    @PostMapping("/start/uid/{uid}")
    fun start(@PathVariable uid: String, principal: Principal) = handleErrors {
        machineService.startMachine(uid, getUser(principal))
    }

    @PostMapping("/stop/uid/{uid}")
    fun stop(@PathVariable uid: String, principal: Principal) = handleErrors {
        machineService.stopMachine(uid, getUser(principal))
    }

    @PostMapping("/restart/uid/{uid}")
    fun restart(@PathVariable uid: String, principal: Principal) = handleErrors {
        machineService.restartMachine(uid, getUser(principal))
    }

    @PostMapping("/destroy/uid/{uid}")
    fun destroy(@PathVariable uid: String, principal: Principal) = handleErrors {
        machineService.destroyMachine(uid, getUser(principal))
    }

    private fun handleErrors(action: () -> Boolean): ResponseEntity<String> =
        runCatching(action).map { success ->
            if (success) ResponseEntity.noContent().build<String>()
            else ResponseEntity.badRequest().build()
        }.getOrElse {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Encountered exception: ${it.message}")
        }

    private fun getUser(principal: Principal) =
        userRepository.findByUsername(principal.name)!!
}

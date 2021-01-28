package rs.mmitic.njp.rafcloud

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.*
import javax.persistence.EntityManager
import javax.persistence.PersistenceContext
import javax.persistence.criteria.CriteriaQuery
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root


@Repository
interface CustomMachineRepository {
    fun searchMachines(queryModel: SearchModel, user: User): List<Machine>
}

@Repository
interface MachineRepository : JpaRepository<Machine, Long>, CustomMachineRepository {
    fun findAllByCreatedByAndActiveIsTrue(createdBy: User): List<Machine>
    fun findByUid(uid: String): Machine?
}

fun MachineRepository.findAllActiveOfUser(createdBy: User) =
    findAllByCreatedByAndActiveIsTrue(createdBy)

@Repository
class CustomMachineRepositoryImpl @Autowired constructor(
    @PersistenceContext private val entityManager: EntityManager
) : CustomMachineRepository {

    override fun searchMachines(queryModel: SearchModel, user: User): List<Machine> {
        val builder = entityManager.getCriteriaBuilder()
        val query: CriteriaQuery<Machine> = builder.createQuery(Machine::class.java)
        val root: Root<Machine> = query.from(Machine::class.java)
        val predicates: MutableList<Predicate> = ArrayList()

        predicates += builder.equal(root.get<Long>("createdBy"), user.id!!)

        queryModel.name?.let { nameQuery ->
            val lowerCase = nameQuery.toLowerCase(Locale.ROOT)
            predicates += builder.like(builder.lower(root.get("name")), "%$lowerCase%")
        }

        if (queryModel.dateFrom != null && queryModel.dateTo != null) {
            predicates += builder.between(root.get("creationDate"), queryModel.dateFrom!!, queryModel.dateTo!!)
        }

        queryModel.status?.let { statusQuery ->
            val list = statusQuery.map { MachineStatus.valueOf(it) }.distinct()
            if (list.size == 1) {
                predicates += builder.equal(root.get<MachineStatus>("status"), list.first())
            }
        }

        query.select(root).where(*predicates.toTypedArray())
        return entityManager.createQuery(query).getResultList()
    }
}

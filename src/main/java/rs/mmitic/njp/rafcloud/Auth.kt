package rs.mmitic.njp.rafcloud

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.security.core.Authentication
import org.springframework.security.core.AuthenticationException
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.stereotype.Controller
import org.springframework.stereotype.Repository
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.security.Principal
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id
import javax.validation.constraints.Email
import javax.validation.constraints.Size


@Controller
class LoginController @Autowired constructor(
    private val userRepository: UserRepository
) {

    @GetMapping("/user")
    @ResponseBody
    fun user(user: Principal): UserDTO =
        userRepository.findByUsername(user.name)!!.toDTO()

}

@EnableWebSecurity
@Configuration
class MySecurityConfiguration @Autowired constructor(
    private val userRepository: UserRepository,
    private val authProvider: CustomAuthenticationProvider,
    private val passwordEncoder: PasswordEncoder
) : WebSecurityConfigurerAdapter() {

    @Throws(Exception::class)
    override fun configure(auth: AuthenticationManagerBuilder) {
        auth.authenticationProvider(authProvider)
    }

    @Throws(Exception::class)
    override fun configure(http: HttpSecurity) {
        http.cors().and()
            .httpBasic().and()
            .authorizeRequests().anyRequest().authenticated()
            .and().csrf().disable()

        if (userRepository.count() == 0L) { //hardcode-ujemo par korisnika
            userRepository.save(User("admin@raf.rs", passwordEncoder.encode("admin"), role = UserRole.ADMIN))
            userRepository.save(User("user@raf.rs", passwordEncoder.encode("user"), "User"))
        }
    }

    @Bean
    fun corsConfigurer() = object : WebMvcConfigurer {
        override fun addCorsMappings(registry: CorsRegistry) {
            registry.addMapping("/**"); // CORS za sve endpointe
        }
    }
}

@Component
class CustomAuthenticationProvider @Autowired constructor(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder
) : AuthenticationProvider {

    @Throws(AuthenticationException::class)
    override fun authenticate(authentication: Authentication): Authentication {
        val username: String = authentication.name
        val password: String = authentication.credentials.toString()

        val user: User = userRepository.findByUsername(username)
            ?: throw UsernameNotFoundException("User doesn't exist")
        if (!passwordEncoder.matches(password, user.password))
            throw BadCredentialsException("Passwords don't match")

        val authorities = listOf(SimpleGrantedAuthority(user.role.value))
        return UsernamePasswordAuthenticationToken(username, password, authorities)
    }

    override fun supports(authentication: Class<*>): Boolean =
        authentication == UsernamePasswordAuthenticationToken::class.java
}


@Configuration
class Config {
    @get:Bean //@Bean on getter method
    val passwordEncoder: PasswordEncoder = BCryptPasswordEncoder()
}

@Entity
class User(
    @Column(length = 100)
    @Size(max = 100)
    @Email
    var username: String,

    var password: String,

    @Column(length = 100)
    @Size(max = 100)
    var firstName: String? = null,

    @Column(length = 100)
    @Size(max = 100)
    var lastName: String? = null,

    var role: UserRole = UserRole.USER,

    @Id @GeneratedValue
    var id: Long? = null
)

enum class UserRole(val value: String) {
    USER("USER"), ADMIN("ADMIN")
}

class UserDTO(
    val username: String,
    val firstName: String?,
    val lastName: String?,
    val role: UserRole
)

fun User.toDTO() = UserDTO(username, firstName, lastName, role)

@Repository
interface UserRepository : JpaRepository<User, Long> {
    fun findByUsername(username: String): User?
}
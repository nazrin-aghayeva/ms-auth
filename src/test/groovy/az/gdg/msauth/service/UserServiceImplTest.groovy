package az.gdg.msauth.service

import az.gdg.msauth.client.MsStorageClient
import az.gdg.msauth.dao.UserRepository
import az.gdg.msauth.exception.ExceedLimitException
import az.gdg.msauth.exception.NotAllowedException
import az.gdg.msauth.exception.NotFoundException
import az.gdg.msauth.exception.WrongDataException
import az.gdg.msauth.mapper.UserMapper
import az.gdg.msauth.model.dto.UserDTO
import az.gdg.msauth.model.dto.UserDetail
import az.gdg.msauth.model.entity.UserEntity
import az.gdg.msauth.security.model.dto.UserInfo
import az.gdg.msauth.security.util.TokenUtil
import az.gdg.msauth.service.impl.MailServiceImpl
import az.gdg.msauth.service.impl.UserServiceImpl
import spock.lang.Specification
import spock.lang.Title

@Title("Testing for user service implementation")
class UserServiceImplTest extends Specification {

    UserRepository userRepository
    UserServiceImpl userService
    MailServiceImpl mailServiceImpl
    TokenUtil tokenUtil
    MsStorageClient msStorageClient


    def setup() {
        userRepository = Mock()
        mailServiceImpl = Mock()
        tokenUtil = Mock()
        msStorageClient = Mock()
        userService = new UserServiceImpl(userRepository, msStorageClient, mailServiceImpl, tokenUtil)
    }


    def "don't throw NotAllowedException if user accepts our terms and conditions"() {

        given:
            def userDto = new UserDTO()
            userDto.setAreTermsAndConditionsConfirmed(true)
            userDto.setMail("example.com")
            userDto.setPassword("pw")

        when:
            userService.signUp(userDto)

        then:
            notThrown(NotAllowedException)
    }

    def "should throw NotAllowedException if user accepts our terms and conditions"() {

        given:
            def userDto = new UserDTO()
            userDto.setAreTermsAndConditionsConfirmed(false)
            userDto.setMail("example.com")
            userDto.setPassword("pw")

        when:
            userService.signUp(userDto)

        then:
            thrown(NotAllowedException)
    }

    def "don't throw WrongDataException if email which is sent with dto doesn't exist in database"() {

        given:
            def userDto = new UserDTO()
            def entity = null
            userDto.setAreTermsAndConditionsConfirmed(true)
            userDto.setMail("example.com")
            userDto.setPassword("pw")

        when:
            userService.signUp(userDto)

        then:
            1 * userRepository.findByMail(userDto.getMail()) >> entity
            notThrown(WrongDataException)
    }

    def "should throw WrongDataException if email exists in database"() {

        given:
            def userDto = new UserDTO()
            def entity = new UserEntity()
            userDto.setAreTermsAndConditionsConfirmed(true)
            userDto.setMail("example.com")
            userDto.setPassword("pw")

        when:
            userService.signUp(userDto)

        then:
            1 * userRepository.findByMail(userDto.getMail()) >> entity
            thrown(WrongDataException)
    }

    def "verify account if user exists in database"() {

        given:
            def token = "dsadsfsf"
            def userEntity = new UserEntity()
            userEntity.setMail("example.com")

        when:
            userService.verifyAccount(token)

        then:
            1 * tokenUtil.getMailFromToken(token) >> userEntity.getMail()
            1 * userRepository.findByMail(userEntity.getMail()) >> userEntity
            1 * userRepository.save(userEntity)
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't verify account if user doesn't exist in database"() {

        given:
            def token = "dsadsfsf"
            def userEntity = null
            def mail = "example.com"

        when:
            userService.verifyAccount(token)

        then:
            1 * tokenUtil.getMailFromToken(token) >> mail
            1 * userRepository.findByMail(mail) >> userEntity
            thrown(NotFoundException)
    }

    def "send reset password link to mail if user exists in database"() {

        given:
            def userEntity = new UserEntity()
            userEntity.setMail("example.com")

        when: "send mail to service"
            userService.sendResetPasswordLinkToMail(userEntity.getMail())

        then:
            1 * userRepository.findByMail(userEntity.getMail()) >> userEntity
            1 * mailServiceImpl.sendMail('<h2>Reset Password</h2></br>' +
                    '<a href=http://virustat.org/reset.html?token=null>http://virustat.org/reset.html?token=null</a>',
                    'example.com',
                    'Your reset password letter')
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't send reset password link to mail if user doesn't exist in database"() {

        given:
            def mail = "example.com"
            def userEntity = null

        when:
            userService.sendResetPasswordLinkToMail(mail)

        then:
            1 * userRepository.findByMail("example.com") >> userEntity
            thrown(NotFoundException)
    }

    def "add popularity if user exists in database"() {

        given:
            def userEntity = new UserEntity()
            userEntity.setId(1)
            userEntity.setPopularity(1)
            def user = Optional.of(userEntity)
        when:
            userService.addPopularity(userEntity.getId())

        then:
            1 * userRepository.findById(userEntity.getId()) >> user
            1 * userRepository.save(userEntity)
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't add popularity if user doesn't exist in database"() {

        given:
            def userId = 1
            def userEntity = Optional.empty()

        when:
            userService.addPopularity(userId)

        then:
            1 * userRepository.findById(userId) >> userEntity
            thrown(NotFoundException)
    }

    def "get remaining quack count if user exists in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            def entity = new UserEntity()
            entity.setRemainingQuackCount(12)
            userInfo.setUserId("1")
            def userEntity = Optional.of(entity)

        when:
            userService.getRemainingQuackCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            entity.getRemainingQuackCount() == 12
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't get remaining quack count if user doesn't exist in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            userInfo.setUserId("1")
            def userEntity = Optional.empty()

        when:
            userService.getRemainingQuackCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            thrown(NotFoundException)
    }

    def "get remaining hate count if user exists in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            def entity = new UserEntity()
            entity.setRemainingHateCount(12)
            userInfo.setUserId("1")
            def userEntity = Optional.of(entity)

        when:
            userService.getRemainingHateCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            entity.getRemainingHateCount() == 12
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't get remaining hate count if user doesn't exist in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            userInfo.setUserId("1")
            def userEntity = Optional.empty()

        when:
            userService.getRemainingHateCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            thrown(NotFoundException)
    }

    def "refresh remaining quack and hate count if users exist in database"() {

        given:
            def userEntity = new UserEntity()
            def listUsers = []
            listUsers.add(userEntity)
        when:
            userService.refreshRemainingQuackAndHateCount()

        then:
            1 * userRepository.findAll() >> listUsers
            1 * userRepository.save(userEntity)
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't refresh remaining quack and hate count if users don't exist in database"() {

        given:
            def listUsers = Collections.emptyList()
        when:
            userService.refreshRemainingQuackAndHateCount()

        then:
            1 * userRepository.findAll() >> listUsers
            thrown(NotFoundException)
    }

    def "get popular users"() {

        given:
            def userEntity = new UserEntity()
            def listUsers = []
            listUsers.add(userEntity)
            def populars = UserMapper.INSTANCE.entityToDtoList(listUsers)

        when:
            userService.getPopularUsers()

        then:
            1 * userRepository.findFirst3ByOrderByPopularityDesc() >> listUsers
            UserMapper.INSTANCE.entityToDtoList(listUsers) == populars
    }

    def "update remaining quack count if user exists in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            def entity = new UserEntity()
            entity.setRemainingQuackCount(12)
            userInfo.setUserId("1")
            def userEntity = Optional.of(entity)

        when:
            userService.updateRemainingQuackCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            1 * userRepository.save(entity)
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't update remaining quack count if user doesn't exist in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            userInfo.setUserId("1")
            def userEntity = Optional.empty()

        when:
            userService.updateRemainingQuackCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            thrown(NotFoundException)
    }

    def "should throw ExceedLimitException and don't update remaining quack count if there is not any remaining quack count in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            def entity = new UserEntity()
            entity.setRemainingQuackCount(0)
            userInfo.setUserId("1")
            def userEntity = Optional.of(entity)

        when:
            userService.updateRemainingQuackCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            thrown(ExceedLimitException)
    }

    def "update remaining hate count if user exists in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            def entity = new UserEntity()
            entity.setRemainingHateCount(12)
            userInfo.setUserId("1")
            def userEntity = Optional.of(entity)

        when:
            userService.updateRemainingHateCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            1 * userRepository.save(entity)
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't update remaining hate count if user doesn't exist in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            userInfo.setUserId("1")
            def userEntity = Optional.empty()

        when:
            userService.updateRemainingHateCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            thrown(NotFoundException)
    }

    def "should throw ExceedLimitException and don't update remaining hate count if there is not any remaining hate count in database"() {

        given:
            def token = "dsadsfsf"
            def userInfo = new UserInfo()
            def entity = new UserEntity()
            entity.setRemainingHateCount(0)
            userInfo.setUserId("1")
            def userEntity = Optional.of(entity)

        when:
            userService.updateRemainingHateCount(token)

        then:
            1 * tokenUtil.getUserInfoFromToken(token) >> userInfo
            1 * userRepository.findById(Integer.parseInt(userInfo.getUserId())) >> userEntity
            thrown(ExceedLimitException)
    }

    def "get user by id if user exists in database"() {

        given:
            def userEntity = new UserEntity()
            userEntity.setId(1)
            def user = Optional.of(userEntity)
            def userDetail = new UserDetail()
            userDetail.setId(1)
        when:
            userService.getUserById(userEntity.getId())

        then:
            1 * userRepository.findById(userEntity.getId()) >> user
            UserMapper.INSTANCE.entityToDto(userEntity) == userDetail
            notThrown(NotFoundException)
    }

    def "should throw NotFoundException and don't get user by id if user doesn't exist in database"() {

        given:
            def userId = 1
            def userEntity = Optional.empty()
        when:
            userService.getUserById(userId)

        then:
            1 * userRepository.findById(userId) >> userEntity
            thrown(NotFoundException)
    }


}
